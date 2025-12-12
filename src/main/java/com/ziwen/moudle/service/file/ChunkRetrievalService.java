package com.ziwen.moudle.service.file;

import com.ziwen.moudle.entity.file.FileChunkEntity;
import com.ziwen.moudle.mapper.file.FileChunkMapper;
import com.ziwen.moudle.service.embedding.EmbeddingService;
import com.ziwen.moudle.service.vector.MilvusService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 片段检索服务
 * 基于向量相似度检索文本片段
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-07
 */
@Slf4j
@Service
public class ChunkRetrievalService {

    @Data
    private static class FileScore {
        private final Long fileId;
        private float maxSimilarity = 0f;
        private float totalSimilarity = 0f;
        private int matchCount = 0;

        public void addMatch(float similarity, float distance) {
            maxSimilarity = Math.max(maxSimilarity, similarity);
            totalSimilarity += similarity;
            matchCount++;
        }

        public float avgSimilarity() {
            return matchCount > 0 ? totalSimilarity / matchCount : 0f;
        }
    }

    private final EmbeddingService embeddingService;
    private final MilvusService milvusService;
    private final FileChunkMapper chunkMapper;

    public ChunkRetrievalService(EmbeddingService embeddingService,
                                MilvusService milvusService,
                                FileChunkMapper chunkMapper) {
        this.embeddingService = embeddingService;
        this.milvusService = milvusService;
        this.chunkMapper = chunkMapper;
    }

    /**
     * 检索相似片段（使用默认相似度阈值 0.35）
     */
    public Mono<List<FileChunkEntity>> retrieveSimilarChunks(String query, int topK) {
        return retrieveSimilarChunks(query, topK, 0.35f);
    }

    /**
     * 检索相似片段（可指定相似度阈值）
     * @param query 查询文本
     * @param topK 最多返回数量
     * @param minSimilarity 最小相似度阈值 (0-1)，推荐 0.6-0.8
     */
    public Mono<List<FileChunkEntity>> retrieveSimilarChunks(String query, int topK, float minSimilarity) {
        log.info("\n========== 开始检索 ==========");
        log.info("原始问题: {}", query);

        // 1. 扩展查询（添加上下文）
        String expandedQuery = expandQuery(query);
        log.info("扩展查询: {}", expandedQuery);
        log.info("相似度阈值: {}", minSimilarity);

        // 2. Query向量化
        return embeddingService.embedText(expandedQuery)
                .map(queryVector -> {
                    try {
                        // 3. Milvus向量检索（带相似度过滤）
                        List<MilvusService.VectorSearchResult> results =
                                milvusService.searchVectors(queryVector, topK * 2, minSimilarity);

                        // 4. 按文件分组，计算综合评分
                        Map<Long, FileScore> fileScores = new HashMap<>();

                        log.info("========== 向量检索结果 ==========");
                        for (MilvusService.VectorSearchResult result : results) {
                            FileChunkEntity chunk = chunkMapper.selectById(result.getChunkId());
                            if (chunk != null) {
                                Long fileId = chunk.getFileId();
                                fileScores.computeIfAbsent(fileId, FileScore::new)
                                    .addMatch(result.getSimilarity(), result.getDistance());

                                log.info("片段ID: {}, 文件ID: {}, 相似度: {}, 距离: {}, 预览: {}",
                                    result.getChunkId(), fileId,
                                    String.format("%.4f", result.getSimilarity()),
                                    String.format("%.4f", result.getDistance()),
                                    chunk.getChunkText().substring(0, Math.min(30, chunk.getChunkText().length())));
                            }
                        }
                        log.info("====================================");

                        // 5. 综合评分 + 关键词过滤
                        log.info("\n========== 文件评分汇总 ==========");
                        Set<String> queryKeywords = extractKeywords(query);
                        log.info("查询关键词: {}", queryKeywords);

                        List<Long> qualifiedFileIds = fileScores.values().stream()
                                .peek(fs -> log.info("文件ID: {}, 最高相似度: {}, 平均相似度: {}, 匹配数: {}",
                                    fs.fileId,
                                    String.format("%.4f", fs.maxSimilarity),
                                    String.format("%.4f", fs.avgSimilarity()),
                                    fs.matchCount))
                                .filter(fs -> {
                                    // 所有文件都必须通过关键词匹配（如果有关键词）
                                    if (!queryKeywords.isEmpty() && !hasKeywordMatch(fs.fileId, queryKeywords)) {
                                        log.info("文件ID: {} 被过滤（不包含关键词）", fs.fileId);
                                        return false;
                                    }
                                    // 相似度阈值过滤
                                    return fs.maxSimilarity >= minSimilarity;
                                })
                                .sorted((a, b) -> Float.compare(b.maxSimilarity, a.maxSimilarity))
                                .limit(topK)
                                .map(fs -> fs.fileId)
                                .collect(Collectors.toList());

                        log.info("过滤后保留 {} 个文件", qualifiedFileIds.size());
                        log.info("==================================\n");

                        if (qualifiedFileIds.isEmpty()) {
                            log.warn("未找到相似文件，返回空列表");
                            return List.<FileChunkEntity>of();
                        }

                        // 6. 获取所有片段并 Rerank
                        List<FileChunkEntity> allChunks = qualifiedFileIds.stream()
                                .flatMap(fileId -> chunkMapper.selectByFileId(fileId).stream())
                                .collect(Collectors.toList());

                        // 7. Rerank：基于关键词匹配度重新排序
                        List<FileChunkEntity> rerankedChunks = rerank(allChunks, query, queryKeywords);

                        log.info("返回 {} 个片段（来自 {} 个文件，已Rerank）", rerankedChunks.size(), qualifiedFileIds.size());
                        return rerankedChunks;
                    } catch (Exception e) {
                        log.error("检索相似片段失败: {}", e.getMessage());
                        return List.<FileChunkEntity>of();
                    }
                })
                .onErrorResume(error -> {
                    log.error("检索失败: {}", error.getMessage());
                    return Mono.just(List.<FileChunkEntity>of());
                });
    }

    /**
     * 扩展查询，添加上下文信息
     */
    private String expandQuery(String query) {
        // 如果查询太短，添加上下文
        if (query.length() < 10) {
            return query + " 的详细信息、背景、事迹";
        }
        return query;
    }

    /**
     * 提取查询关键词（支持中文分词）
     */
    private Set<String> extractKeywords(String query) {
        Set<String> stopWords = Set.of("的", "是", "一个", "什么", "样", "人", "详细", "信息", "背景", "事迹", "相关", "文件");
        Set<String> keywords = new HashSet<>();

        // 按空格和标点分词
        String[] words = query.split("[\\s\\p{Punct}]+");
        for (String word : words) {
            word = word.trim();
            if (word.length() > 1 && !stopWords.contains(word)) {
                keywords.add(word);
                // 中文按字符拆分（2-4字）
                if (word.length() >= 2 && word.matches(".*[\\u4e00-\\u9fa5].*")) {
                    for (int i = 0; i <= word.length() - 2; i++) {
                        String sub = word.substring(i, Math.min(i + 3, word.length()));
                        if (sub.length() >= 2 && !stopWords.contains(sub)) {
                            keywords.add(sub);
                        }
                    }
                }
            }
        }
        return keywords;
    }

    /**
     * 检查文件是否包含关键词
     */
    private boolean hasKeywordMatch(Long fileId, Set<String> keywords) {
        if (keywords.isEmpty()) return true;

        List<FileChunkEntity> chunks = chunkMapper.selectByFileId(fileId);
        String fileContent = chunks.stream()
                .map(FileChunkEntity::getChunkText)
                .collect(Collectors.joining(" "))
                .toLowerCase();

        return keywords.stream()
                .anyMatch(keyword -> fileContent.contains(keyword.toLowerCase()));
    }

    /**
     * Rerank：基于多维度评分重新排序片段
     */
    private List<FileChunkEntity> rerank(List<FileChunkEntity> chunks, String query, Set<String> keywords) {
        log.info("开始 Rerank，片段数: {}", chunks.size());

        return chunks.stream()
                .map(chunk -> {
                    double score = calculateRelevanceScore(chunk, query, keywords);
                    return new ScoredChunk(chunk, score);
                })
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .peek(sc -> log.debug("片段ID: {}, Rerank分数: {:.4f}, 预览: {}",
                    sc.chunk.getId(), sc.score,
                    sc.chunk.getChunkText().substring(0, Math.min(30, sc.chunk.getChunkText().length()))))
                .map(sc -> sc.chunk)
                .collect(Collectors.toList());
    }

    /**
     * 计算片段相关性分数（多维度）
     */
    private double calculateRelevanceScore(FileChunkEntity chunk, String query, Set<String> keywords) {
        String text = chunk.getChunkText().toLowerCase();
        double score = 0.0;

        // 1. 关键词匹配度（权重 0.6）
        if (!keywords.isEmpty()) {
            long matchCount = keywords.stream()
                    .filter(kw -> text.contains(kw.toLowerCase()))
                    .count();
            score += (matchCount / (double) keywords.size()) * 0.6;
        }

        // 2. 查询词完整匹配（权重 0.3）
        if (text.contains(query.toLowerCase())) {
            score += 0.3;
        }

        // 3. 文本长度合理性（权重 0.1）
        int length = chunk.getChunkText().length();
        if (length >= 50 && length <= 1000) {
            score += 0.1;
        } else if (length > 1000) {
            score += 0.05;
        }

        return score;
    }

    @Data
    private static class ScoredChunk {
        private final FileChunkEntity chunk;
        private final double score;
    }
}

