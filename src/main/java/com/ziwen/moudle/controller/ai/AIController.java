package com.ziwen.moudle.controller.ai;

import com.ziwen.moudle.common.AjaxResult;
import com.ziwen.moudle.dto.ai.FileBasedQARequest;
import com.ziwen.moudle.service.ai.FileBasedQAService;
import com.ziwen.moudle.service.ai.MultiModalSearchService;
import com.ziwen.moudle.service.ai.VisionLanguageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * AI 问答控制器（RAG - 检索增强生成）
 * 支持多模态检索和问答
 *
 * @author : zixiwen
 * @date : 2025-12-07
 * @version : 2.0 重构为完整的RAG流程
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final FileBasedQAService qaService;
    private final MultiModalSearchService searchService;
    private final VisionLanguageService visionLanguageService;

    /**
     * 初始化多模态集合（仅首次调用）
     */
    @PostMapping("/init")
    public Mono<AjaxResult> initialize() {
        return searchService.initializeCollection()
            .map(v -> AjaxResult.success("集合初始化成功"))
            .onErrorResume(error ->
                Mono.just(AjaxResult.error("初始化失败: " + error.getMessage()))
            );
    }

    /**
     * 索引图片到知识库
     */
    @PostMapping("/index")
    public Mono<AjaxResult> indexDocument(@RequestBody IndexRequest request) {
        try {
            return visionLanguageService.describeImage(request.getImageBase64())
                .flatMap(description -> {
                    return searchService.insertMultimodalData(
                        request.getOrigin(),
                        request.getImageBase64(),
                        description
                    ).map(v -> AjaxResult.success("文档索引成功"));
                })
                .onErrorResume(error ->
                    Mono.just(AjaxResult.error("索引失败: " + error.getMessage()))
                );
        } catch (Exception e) {
            return Mono.just(AjaxResult.error("请求格式错误: " + e.getMessage()));
        }
    }

    /**
     * 基于文本问题的RAG（检索+回答）
     * 流程：文本问题 → 检索相关文档 → 基于检索结果生成答案
     */
    @PostMapping("/rag/text")
    public Mono<AjaxResult> ragByText(@RequestBody RAGRequest request) {
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return Mono.just(AjaxResult.warn("问题不能为空"));
        }

        // 直接使用FileBasedQAService，它内部已完成RAG流程
        FileBasedQARequest qaRequest = new FileBasedQARequest();
        qaRequest.setQuestion(request.getQuestion());
        qaRequest.setMaxFiles(request.getLimit());

        return qaService.answerQuestion(qaRequest)
            .map(response -> {
                if (response.getError() != null) {
                    return AjaxResult.error(response.getError());
                }
                return AjaxResult.success("RAG回答成功", response);
            })
            .onErrorResume(error ->
                Mono.just(AjaxResult.error("RAG问答失败: " + error.getMessage()))
            );
    }

    /**
     * 基于图片问题的RAG（检索+回答）
     * 流程：图片问题 → 提取图片描述 → 检索相关文档 → 基于检索结果生成答案
     */
    @PostMapping("/rag/image")
    public Mono<AjaxResult> ragByImage(@RequestBody RAGRequest request) {
        if (request.getImageBase64() == null || request.getImageBase64().trim().isEmpty()) {
            return Mono.just(AjaxResult.warn("图片不能为空"));
        }

        // Step 1: 提取图片描述
        return visionLanguageService.describeImage(request.getImageBase64())
            .flatMap(description -> {
                // Step 2: 使用描述进行RAG问答
                FileBasedQARequest qaRequest = new FileBasedQARequest();
                qaRequest.setQuestion(request.getQuestion() != null ?
                    request.getQuestion() + " [图片描述: " + description + "]" : description);
                qaRequest.setMaxFiles(request.getLimit());

                return qaService.answerQuestion(qaRequest)
                    .map(response -> {
                        if (response.getError() != null) {
                            return AjaxResult.error(response.getError());
                        }
                        return AjaxResult.success("RAG回答成功", response);
                    });
            })
            .onErrorResume(error ->
                Mono.just(AjaxResult.error("RAG问答失败: " + error.getMessage()))
            );
    }

    /**
     * 仅检索文本相关文档（不回答）
     */
    @PostMapping("/search/text")
    public Mono<AjaxResult> searchByText(@RequestBody SearchRequest request) {
        return searchService.searchTextByText(request.getQuery(), request.getLimit())
            .map(results -> AjaxResult.success("检索成功", results))
            .onErrorResume(error ->
                Mono.just(AjaxResult.error("检索失败: " + error.getMessage()))
            );
    }

    /**
     * 仅检索图片相关文档（不回答）
     */
    @PostMapping("/search/image")
    public Mono<AjaxResult> searchByImage(@RequestBody SearchRequest request) {
        return searchService.searchByImage(request.getImageBase64(), request.getLimit())
            .map(results -> AjaxResult.success("检索成功", results))
            .onErrorResume(error ->
                Mono.just(AjaxResult.error("检索失败: " + error.getMessage()))
            );
    }


    /**
     * 原始的基于文件的问答（RAG）
     */
    @PostMapping("/qa")
    public Mono<AjaxResult> askQuestion(@RequestBody FileBasedQARequest request) {
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return Mono.just(AjaxResult.warn("问题不能为空"));
        }

        return qaService.answerQuestion(request)
            .map(response -> {
                if (response.getError() != null) {
                    return AjaxResult.error(response.getError());
                }
                return AjaxResult.success("回答成功", response);
            })
            .onErrorResume(error ->
                Mono.just(AjaxResult.error("问答失败: " + error.getMessage()))
            );
    }


    // ==================== 请求/响应类 ====================

    /**
     * 索引请求
     */
    public static class IndexRequest {
        private String origin;
        private String imageBase64;

        public String getOrigin() { return origin; }
        public void setOrigin(String origin) { this.origin = origin; }
        public String getImageBase64() { return imageBase64; }
        public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    }

    /**
     * RAG请求
     */
    public static class RAGRequest {
        private String question;
        private String imageBase64;
        private Integer limit = 5;

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public String getImageBase64() { return imageBase64; }
        public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
    }

    /**
     * 仅搜索请求
     */
    public static class SearchRequest {
        private String query;
        private String imageBase64;
        private Integer limit = 5;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public String getImageBase64() { return imageBase64; }
        public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
    }

}
