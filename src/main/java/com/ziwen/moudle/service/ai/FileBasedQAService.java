package com.ziwen.moudle.service.ai;

import com.ziwen.moudle.dto.ai.FileBasedQARequest;
import com.ziwen.moudle.dto.ai.FileBasedQAResponse;
import com.ziwen.moudle.dto.file.FileContentInfo;
import com.ziwen.moudle.dto.minimax.ChatMessage;
import com.ziwen.moudle.dto.minimax.ChatRequest;
import com.ziwen.moudle.dto.minimax.ChatResponse;
import com.ziwen.moudle.service.file.FileContentService;
import com.ziwen.moudle.service.minimax.MiniMaxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于文件的AI问答服务
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-06
 */
@Slf4j
@Service
public class FileBasedQAService {

    private final FileContentService fileContentService;
    private final MiniMaxService miniMaxService;
    private final KeywordExtractionService keywordExtractionService;

    public FileBasedQAService(FileContentService fileContentService,
                            MiniMaxService miniMaxService,
                            KeywordExtractionService keywordExtractionService) {
        this.fileContentService = fileContentService;
        this.miniMaxService = miniMaxService;
        this.keywordExtractionService = keywordExtractionService;
    }

    /**
     * 基于文件回答问题
     *
     * @param request 问答请求
     * @return 问答响应
     */
    public Mono<FileBasedQAResponse> answerQuestion(FileBasedQARequest request) {
        log.info("处理基于文件的AI问答，问题: {}", request.getQuestion());

        // 1. 提取或生成关键词
        return keywordExtractionService.extractKeywords(request.getQuestion(), request.getKeyword())
                .flatMap(keywords -> {
                    log.info("提取的关键词: {}", keywords);

                    // 2. 搜索相关文件（支持重试）
                    return searchRelevantFilesWithRetry(request, keywords);
                })
                .flatMap(files -> {
                    // 3. 构建提示词
                    String promptText = buildPrompt(request.getQuestion(), files);
                    log.info("构建的提示词长度: {}", promptText.length());

                    // 4. 调用 AI
                    ChatMessage userMessage = new ChatMessage();
                    userMessage.setRole("user");
                    userMessage.setContent(promptText);

                    ChatRequest chatRequest = new ChatRequest();
                    chatRequest.setMessages(List.of(userMessage));

                    log.info("调用MiniMax AI...");
                    return miniMaxService.chat(chatRequest)
                            .doOnNext(aiResponse -> log.info("AI响应: {}", aiResponse))
                            .map(aiResponse -> {
                                // 5. 构建响应
                                return buildResponse(request, files, aiResponse);
                            });
                })
                .onErrorResume(error -> {
                    log.error("AI问答处理失败", error);
                    return Mono.just(FileBasedQAResponse.builder()
                            .question(request.getQuestion())
                            .answer("抱歉，处理您的问题时发生错误: " + error.getMessage())
                            .error(error.getMessage())
                            .build());
                });
    }

    /**
     * 搜索相关文件（支持重试和关键词补充）
     *
     * @param request 问答请求
     * @param keywords 提取的关键词列表
     * @return 相关文件列表
     */
    private Mono<List<FileContentInfo>> searchRelevantFilesWithRetry(FileBasedQARequest request, List<String> keywords) {
        return Mono.fromCallable(() -> searchFilesWithKeywords(request, keywords))
                .flatMap(files -> {
                    // 如果搜索结果为空，尝试补充关键词
                    if (files.isEmpty()) {
                        log.info("第一次搜索结果为空，尝试补充关键词");
                        return keywordExtractionService.supplementKeywords(request.getQuestion(), keywords)
                                .flatMap(supplementedKeywords -> {
                                    log.info("补充后的关键词: {}", supplementedKeywords);
                                    List<FileContentInfo> retryFiles = searchFilesWithKeywords(request, supplementedKeywords);
                                    log.info("补充关键词后搜索到 {} 个文件", retryFiles.size());
                                    return Mono.just(retryFiles);
                                });
                    }
                    return Mono.just(files);
                });
    }

    /**
     * 根据关键词列表搜索文件
     *
     * @param request 问答请求
     * @param keywords 关键词列表
     * @return 相关文件列表
     */
    private List<FileContentInfo> searchFilesWithKeywords(FileBasedQARequest request, List<String> keywords) {
        List<FileContentInfo> allFiles = new ArrayList<>();

        // 1. 如果关键词列表为空，返回所有可读文件
        if (keywords == null || keywords.isEmpty()) {
            log.info("关键词列表为空，返回所有可读文件");
            allFiles.addAll(fileContentService.readAllReadableFiles());
        } else {
            // 2. 根据每个关键词搜索
            for (String keyword : keywords) {
                if (StringUtils.hasText(keyword)) {
                    List<FileContentInfo> keywordFiles = fileContentService.searchAndReadFiles(keyword);
                    log.info("关键词 '{}' 搜索到 {} 个文件: {}", keyword, keywordFiles.size(),
                            keywordFiles.stream().map(FileContentInfo::getFileName).collect(Collectors.toList()));
                    allFiles.addAll(keywordFiles);
                }
            }

            // 3. 如果指定了模糊文件名，搜索匹配的文件
            if (StringUtils.hasText(request.getFuzzyFileName())) {
                List<FileContentInfo> fuzzyFiles = fileContentService.searchAndReadFiles(request.getFuzzyFileName());
                log.info("根据模糊文件名搜索到 {} 个文件: {}", fuzzyFiles.size(),
                        fuzzyFiles.stream().map(FileContentInfo::getFileName).collect(Collectors.toList()));
                allFiles.addAll(fuzzyFiles);
            }
        }

        // 4. 去重（根据文件ID）
        List<FileContentInfo> result = allFiles.stream()
                .distinct()
                .limit(request.getMaxFiles() != null ? request.getMaxFiles() : 5)
                .collect(Collectors.toList());

        log.info("搜索完成，总共找到 {} 个相关文件", result.size());
        return result;
    }

    /**
     * 搜索相关文件（兼容性保留）
     *
     * @param request 问答请求
     * @return 相关文件列表
     */
    private List<FileContentInfo> searchRelevantFiles(FileBasedQARequest request) {
        List<String> keywords = new ArrayList<>();
        if (StringUtils.hasText(request.getKeyword())) {
            keywords.add(request.getKeyword());
        }
        return searchFilesWithKeywords(request, keywords);
    }

    /**
     * 构建提示词
     *
     * @param question 问题
     * @param files 相关文件
     * @return 提示词
     */
    private String buildPrompt(String question, List<FileContentInfo> files) {
        StringBuilder prompt = new StringBuilder();

        // 系统提示
        prompt.append("你是一个基于文档的AI助手。请根据提供的文档内容回答用户的问题。\n\n");
        prompt.append("请遵循以下规则：\n");
        prompt.append("1. 仅基于提供的文档内容回答问题\n");
        prompt.append("2. 如果文档中没有相关信息，请明确说明\n");
        prompt.append("3. 引用具体文件时，请注明文件名\n");
        prompt.append("4. 回答要准确、简洁、有条理\n\n");

        // 文档内容
        prompt.append("=== 参考文档 ===\n\n");
        for (int i = 0; i < files.size(); i++) {
            FileContentInfo file = files.get(i);
            prompt.append("【文档 ").append(i + 1).append("】文件名：").append(file.getFileName()).append("\n");
            prompt.append("内容：\n").append(file.getContent()).append("\n\n");
        }

        // 用户问题
        prompt.append("=== 用户问题 ===\n");
        prompt.append(question).append("\n\n");
        prompt.append("请根据以上文档内容回答问题。");

        return prompt.toString();
    }

    /**
     * 构建响应
     *
     * @param request 原始请求
     * @param files 相关文件
     * @param aiResponse AI响应
     * @return 问答响应
     */
    private FileBasedQAResponse buildResponse(FileBasedQARequest request,
                                            List<FileContentInfo> files,
                                            ChatResponse aiResponse) {
        String answer = "";
        if (aiResponse.getContent() != null && !aiResponse.getContent().isEmpty()) {
            // 遍历所有content，找到type="text"的内容
            for (ChatResponse.Content content : aiResponse.getContent()) {
                if (content != null && "text".equals(content.getType()) && content.getText() != null) {
                    answer = content.getText();
                    log.info("AI回答内容: {}", answer);
                    break;
                }
            }
            if (answer.isEmpty()) {
                log.warn("AI响应中未找到text类型的content，content: {}", aiResponse.getContent());
            }
        } else {
            log.warn("AI响应content为空或null，response: {}", aiResponse);
        }

        // 构建引用文件列表
        List<FileBasedQAResponse.ReferencedFile> referencedFiles = files.stream()
                .map(file -> FileBasedQAResponse.ReferencedFile.builder()
                        .fileId(file.getFileId())
                        .fileName(file.getFileName())
                        .filePath(file.getFilePath())
                        .preview(file.getPreview())
                        .build())
                .collect(Collectors.toList());

        log.info("构建响应，文件数量: {}, 回答长度: {}", files.size(), answer.length());

        return FileBasedQAResponse.builder()
                .question(request.getQuestion())
                .answer(answer)
                .referencedFiles(referencedFiles)
                .filesCount(files.size())
                .model(aiResponse.getModel())
                .usage(aiResponse.getUsage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 简单问答（只传入问题，自动搜索相关文件）
     *
     * @param question 问题
     * @return 问答响应
     */
    public Mono<FileBasedQAResponse> simpleQuestion(String question) {
        FileBasedQARequest request = FileBasedQARequest.builder()
                .question(question)
                .keyword(null)  // 不指定关键词，让智能提取服务自动处理
                .maxFiles(5)
                .build();

        return answerQuestion(request);
    }
}