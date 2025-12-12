package com.ziwen.moudle.service.ai;

import com.alibaba.fastjson.JSONObject;
import com.ziwen.moudle.config.MilvusConfig;
import com.ziwen.moudle.service.embedding.EmbeddingService;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * 多模态搜索服务
 * 支持以文搜图、以文搜文、以图搜图、以图搜文
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultiModalSearchService {

    private final MilvusServiceClient milvusClient;
    private final MilvusConfig milvusConfig;
    private final EmbeddingService embeddingService;

    /**
     * 初始化集合（仅在首次运行时调用）
     * 如果集合已存在，会跳过创建
     */
    public Mono<Void> initializeCollection() {
        return Mono.fromCallable(() -> {
            String collectionName = milvusConfig.getCollectionName();
            log.info("初始化多模态搜索集合: {}", collectionName);

            try {
                // 检查集合是否已存在
                R<Boolean> hasCollection = milvusClient.hasCollection(
                    HasCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
                );

                if (hasCollection.getStatus() == 0 && hasCollection.getData()) {
                    log.info("集合 {} 已存在，跳过创建", collectionName);
                    // 集合已存在，尝试创建索引
                    createIndexSafely("image_embedding");
                    createIndexSafely("text_embedding");
                } else {
                    // 集合不存在，创建新集合
                    log.info("创建新集合: {}", collectionName);
                    // 构建集合模式 - 使用FieldType列表
                    List<FieldType> fields = Arrays.asList(
                        FieldType.newBuilder()
                            .withName("id")
                            .withDataType(DataType.Int64)
                            .withPrimaryKey(true)
                            .withAutoID(true)
                            .build(),
                        FieldType.newBuilder()
                            .withName("origin")
                            .withDataType(DataType.VarChar)
                            .withMaxLength(512)
                            .build(),
                        FieldType.newBuilder()
                            .withName("image_description")
                            .withDataType(DataType.VarChar)
                            .withMaxLength(1024)
                            .build(),
                        FieldType.newBuilder()
                            .withName("image_embedding")
                            .withDataType(DataType.FloatVector)
                            .withDimension(milvusConfig.getVectorDim())
                            .build(),
                        FieldType.newBuilder()
                            .withName("text_embedding")
                            .withDataType(DataType.FloatVector)
                            .withDimension(milvusConfig.getVectorDim())
                            .build()
                    );

                    CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withFieldTypes(fields)
                        .build();

                    milvusClient.createCollection(createCollectionParam);
                    log.info("集合创建成功");

                    // 创建索引
                    createIndexSafely("image_embedding");
                    createIndexSafely("text_embedding");
                }

                log.info("集合初始化完成");
            } catch (Exception e) {
                log.error("集合初始化失败", e);
                throw new RuntimeException(e);
            }
            return null;
        }).then();
    }

    /**
     * 安全地创建索引（如果字段不存在或已存在，跳过而不是报错）
     */
    private void createIndexSafely(String fieldName) {
        try {
            String extraParams;
            if ("IVF_FLAT".equalsIgnoreCase(milvusConfig.getIndexType())) {
                extraParams = String.format("{\"nlist\": %d}", milvusConfig.getNlist());
                CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(milvusConfig.getCollectionName())
                    .withFieldName(fieldName)
                    .withIndexType(IndexType.IVF_FLAT)
                    .withMetricType(MetricType.L2)
                    .withExtraParam(extraParams)
                    .build();
                milvusClient.createIndex(createIndexParam);
            } else {
                extraParams = String.format("{\"M\": %d, \"efConstruction\": %d}",
                    milvusConfig.getM(), milvusConfig.getEfConstruction());
                CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(milvusConfig.getCollectionName())
                    .withFieldName(fieldName)
                    .withIndexType(IndexType.HNSW)
                    .withMetricType(MetricType.L2)
                    .withExtraParam(extraParams)
                    .build();
                milvusClient.createIndex(createIndexParam);
            }

            log.info("创建索引成功: {}", fieldName);
        } catch (Exception e) {
            // 如果字段不存在或其他错误，记录警告但不抛出异常
            log.warn("创建索引失败或字段不存在，跳过: {}. 错误: {}", fieldName, e.getMessage());
        }
    }

    /**
     * 插入多模态数据
     */
    public Mono<Void> insertMultimodalData(String origin, String imageBase64, String description) {
        log.info("插入多模态数据: {}", origin);

        return embeddingService.embedImage(imageBase64)
            .flatMap(imageEmbedding ->
                embeddingService.embedText(description)
                    .map(textEmbedding -> {
                        try {
                            // 准备数据 - 使用JSONObject
                            JSONObject data = new JSONObject();
                            data.put("origin", origin);
                            data.put("image_description", description);
                            data.put("image_embedding", imageEmbedding);
                            data.put("text_embedding", textEmbedding);

                            InsertParam insertParam = InsertParam.newBuilder()
                                .withCollectionName(milvusConfig.getCollectionName())
                                .withRows(Collections.singletonList(data))
                                .build();

                            milvusClient.insert(insertParam);

                            log.info("数据插入成功");
                            return null;
                        } catch (Exception e) {
                            log.error("数据插入失败", e);
                            throw new RuntimeException(e);
                        }
                    })
            )
            .then();
    }

    /**
     * 以文搜图 - 根据文本查询最相似的图片
     */
    public Mono<List<SearchResult>> searchByText(String query, int limit) {
        log.info("以文搜图: {}", query);

        return embeddingService.embedText(query)
            .map(embedding -> searchVectors(embedding, "image_embedding", limit));
    }

    /**
     * 以文搜文 - 根据文本查询最相似的文本描述
     */
    public Mono<List<SearchResult>> searchTextByText(String query, int limit) {
        log.info("以文搜文: {}", query);

        return embeddingService.embedText(query)
            .map(embedding -> searchVectors(embedding, "text_embedding", limit));
    }

    /**
     * 以图搜图 - 根据图片查询最相似的图片
     */
    public Mono<List<SearchResult>> searchByImage(String imageBase64, int limit) {
        log.info("以图搜图");

        return embeddingService.embedImage(imageBase64)
            .map(embedding -> searchVectors(embedding, "image_embedding", limit));
    }

    /**
     * 以图搜文 - 根据图片查询最相似的文本描述
     */
    public Mono<List<SearchResult>> searchTextByImage(String imageBase64, int limit) {
        log.info("以图搜文");

        return embeddingService.embedImage(imageBase64)
            .map(embedding -> searchVectors(embedding, "text_embedding", limit));
    }

    private List<SearchResult> searchVectors(List<Float> queryEmbedding, String fieldName, int limit) {
        try {
            String searchParams;
            if ("IVF_FLAT".equalsIgnoreCase(milvusConfig.getIndexType())) {
                searchParams = "{\"nprobe\": 10}";
            } else {
                searchParams = "{\"ef\": 10}";
            }

            SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withVectorFieldName(fieldName)
                .withVectors(Collections.singletonList(queryEmbedding))
                .withParams(searchParams)
                .withTopK(limit)
                .withOutFields(Arrays.asList("origin", "image_description"))
                .build();

            R<SearchResults> response = milvusClient.search(searchParam);
            if (response.getStatus() != 0) {
                log.error("搜索失败: {}", response.getMessage());
                throw new RuntimeException("搜索失败: " + response.getMessage());
            }

            SearchResults searchResults = response.getData();
            SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResults.getResults());

            // 处理搜索结果 - 使用SearchResultsWrapper.getRowRecords(int)非deprecated版本
            List<SearchResult> resultList = new ArrayList<>();
            for (QueryResultsWrapper.RowRecord row : wrapper.getRowRecords(0)) {
                SearchResult result = SearchResult.builder()
                    .distance((Float) row.get("distance"))  // 相似度分数存储在distance字段中
                    .origin((String) row.get("origin"))
                    .imageDescription((String) row.get("image_description"))
                    .build();
                resultList.add(result);
            }

            return resultList;

        } catch (Exception e) {
            log.error("搜索失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 搜索结果
     */
    public static class SearchResult {
        private Float distance;
        private String origin;
        private String imageDescription;

        // Getters and Setters
        public Float getDistance() { return distance; }
        public void setDistance(Float distance) { this.distance = distance; }

        public String getOrigin() { return origin; }
        public void setOrigin(String origin) { this.origin = origin; }

        public String getImageDescription() { return imageDescription; }
        public void setImageDescription(String imageDescription) { this.imageDescription = imageDescription; }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private SearchResult result = new SearchResult();

            public Builder distance(Float distance) {
                result.setDistance(distance);
                return this;
            }

            public Builder origin(String origin) {
                result.setOrigin(origin);
                return this;
            }

            public Builder imageDescription(String imageDescription) {
                result.setImageDescription(imageDescription);
                return this;
            }

            public SearchResult build() { return result; }
        }
    }
}