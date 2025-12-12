package com.ziwen.moudle.service.vector;

import com.ziwen.moudle.config.MilvusConfig;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MilvusService {

    private final MilvusServiceClient milvusClient;
    private final MilvusConfig milvusConfig;

    @Data
    public static class VectorSearchResult {
        private Long chunkId;
        private Float distance;  // 余弦距离，值越小越相似
        private Float similarity; // 相似度分数 (0-1)，值越大越相似
    }

    @PostConstruct
    public void init() {
        try {
            log.info("尝试初始化Milvus向量数据库...");
            createCollectionIfNotExists();
            log.info("Milvus初始化成功");
        } catch (Exception e) {
            log.error("Milvus初始化失败，应用将继续运行但向量功能不可用: {}", e.getMessage());
            // 不抛出异常，允许应用继续启动
        }
    }

    private void createCollectionIfNotExists() {
        R<Boolean> hasCollection = milvusClient.hasCollection(
            HasCollectionParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .build()
        );

        if (hasCollection.getData()) {
            log.info("Milvus collection already exists: {}", milvusConfig.getCollectionName());
            return;
        }

        FieldType chunkIdField = FieldType.newBuilder()
            .withName("chunk_id")
            .withDataType(DataType.Int64)
            .withPrimaryKey(true)
            .withAutoID(false)
            .build();

        FieldType vectorField = FieldType.newBuilder()
            .withName("vector")
            .withDataType(DataType.FloatVector)
            .withDimension(milvusConfig.getVectorDim())
            .build();

        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
            .withCollectionName(milvusConfig.getCollectionName())
            .withDescription("File chunks vector collection")
            .withFieldTypes(List.of(chunkIdField, vectorField))
            .build();

        milvusClient.createCollection(createParam);

        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
            .withCollectionName(milvusConfig.getCollectionName())
            .withFieldName("vector")
            .withIndexType(io.milvus.param.IndexType.IVF_FLAT)
            .withMetricType(io.milvus.param.MetricType.COSINE)
            .withExtraParam("{\"nlist\":128}")
            .build();

        milvusClient.createIndex(indexParam);
        milvusClient.loadCollection(LoadCollectionParam.newBuilder()
            .withCollectionName(milvusConfig.getCollectionName())
            .build());

        log.info("Created Milvus collection: {}", milvusConfig.getCollectionName());
    }

    public void insertVector(Long chunkId, List<Float> vector) {
        try {
            List<Long> chunkIds = Collections.singletonList(chunkId);
            List<List<Float>> vectors = Collections.singletonList(vector);

            InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withFields(List.of(
                    new InsertParam.Field("chunk_id", chunkIds),
                    new InsertParam.Field("vector", vectors)
                ))
                .build();

            R<MutationResult> result = milvusClient.insert(insertParam);
            if (result.getStatus() != 0) {
                log.error("Failed to insert vector for chunk {}: {}", chunkId, result.getMessage());
            }
        } catch (Exception e) {
            log.error("插入向量失败: {}", e.getMessage());
        }
    }

    /**
     * 搜索相似向量（带相似度阈值过滤）
     * @param queryVector 查询向量
     * @param topK 返回Top K结果
     * @param minSimilarity 最小相似度阈值 (0-1)，默认0.5
     */
    public List<VectorSearchResult> searchVectors(List<Float> queryVector, int topK, float minSimilarity) {
        try {
            SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withMetricType(io.milvus.param.MetricType.COSINE)
                .withTopK(topK)
                .withVectors(Collections.singletonList(queryVector))
                .withVectorFieldName("vector")
                .withOutFields(List.of("chunk_id"))
                .build();

            R<SearchResults> response = milvusClient.search(searchParam);
            if (response.getStatus() != 0) {
                log.error("Search failed: {}", response.getMessage());
                return List.of();
            }

            List<VectorSearchResult> results = new ArrayList<>();
            SearchResults searchResults = response.getData();

            if (searchResults != null && searchResults.getResults().getFieldsDataCount() > 0) {
                long numResults = searchResults.getResults().getTopK();
                for (int i = 0; i < numResults; i++) {
                    float distance = searchResults.getResults().getScores(i);
                    // 余弦距离转相似度: similarity = 1 - distance
                    float similarity = 1.0f - distance;

                    // 过滤低相似度结果
                    if (similarity < minSimilarity) {
                        log.debug("过滤低相似度结果: similarity={}, threshold={}", similarity, minSimilarity);
                        continue;
                    }

                    VectorSearchResult result = new VectorSearchResult();
                    long chunkId = searchResults.getResults().getFieldsData(0).getScalars().getLongData().getData(i);
                    result.setChunkId(chunkId);
                    result.setDistance(distance);
                    result.setSimilarity(similarity);
                    results.add(result);
                }
            }

            log.info("检索到 {} 个相似片段（相似度 >= {}）", results.size(), minSimilarity);
            return results;
        } catch (Exception e) {
            log.error("向量搜索失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 搜索相似向量（使用默认相似度阈值 0.5）
     */
    public List<VectorSearchResult> searchVectors(List<Float> queryVector, int topK) {
        return searchVectors(queryVector, topK, 0.5f);
    }

    public void deleteVector(Long chunkId) {
        try {
            String expr = "chunk_id == " + chunkId;
            DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withExpr(expr)
                .build();

            R<MutationResult> result = milvusClient.delete(deleteParam);
            if (result.getStatus() != 0) {
                log.error("Failed to delete vector for chunk {}: {}", chunkId, result.getMessage());
            }
        } catch (Exception e) {
            log.error("删除向量失败: {}", e.getMessage());
        }
    }
}

