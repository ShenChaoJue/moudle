package com.ziwen.moudle.service.ai;

import com.ziwen.moudle.dto.minimax.ChatMessage;
import com.ziwen.moudle.dto.minimax.ChatRequest;
import com.ziwen.moudle.dto.minimax.ChatResponse;
import com.ziwen.moudle.service.minimax.MiniMaxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 智能关键词提取服务
 * 用于从用户问题中提取或生成有效的搜索关键词
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-06
 */
@Slf4j
@Service
public class KeywordExtractionService {

    private final MiniMaxService miniMaxService;

    // 常见停用词
    private static final Set<String> STOP_WORDS = Set.of(
            "目前", "有哪些", "什么", "怎么", "如何", "有没有", "吗", "呢", "啊", "的", "了", "在", "是", "我", "你", "他", "她", "它", "我们", "你们", "他们"
    );

    // 常见文件查询模式及对应的搜索策略
    private static final Map<String, List<String>> QUERY_PATTERNS = Map.of(
            "有哪些文件", Arrays.asList("", "file", "文档", "资料"),
            "有什么文件", Arrays.asList("", "file", "文档", "资料"),
            "查看文件", Arrays.asList("", "file", "文档"),
            "列出文件", Arrays.asList("", "file", "文档"),
            "文件列表", Arrays.asList("", "file", "文档"),
            "有哪些视频", Arrays.asList("video", "视频", "mp4", "avi", "mov"),
            "有哪些图片", Arrays.asList("image", "图片", "jpg", "png", "gif"),
            "有哪些文档", Arrays.asList("doc", "文档", "pdf", "txt", "word")
    );

    public KeywordExtractionService(MiniMaxService miniMaxService) {
        this.miniMaxService = miniMaxService;
    }

    /**
     * 提取或生成搜索关键词
     *
     * @param question 用户问题
     * @param originalKeyword 原始关键词（可能为空）
     * @return 提取的关键词列表
     */
    public Mono<List<String>> extractKeywords(String question, String originalKeyword) {
        List<String> keywords = new ArrayList<>();

        // 1. 尝试从原始关键词中提取
        if (StringUtils.hasText(originalKeyword)) {
            keywords.add(originalKeyword);
        }

        // 2. 从问题中提取关键词
        List<String> extractedKeywords = extractFromQuestion(question);
        keywords.addAll(extractedKeywords);

        // 3. 特殊处理：如果是查询文件列表的通用问题，返回空关键词列表（表示搜索所有文件）
        if (isFileListQuery(question)) {
            log.info("检测到文件列表查询，返回空关键词列表以搜索所有文件");
            return Mono.just(Collections.emptyList());
        }

        // 4. 如果还没有有效关键词，使用AI生成
        if (keywords.isEmpty() || keywords.stream().allMatch(k -> !StringUtils.hasText(k) || STOP_WORDS.contains(k))) {
            log.info("原始关键词无效，使用AI生成关键词");
            return generateKeywordsWithAI(question)
                    .doOnNext(aiKeywords -> {
                        keywords.addAll(aiKeywords);
                        log.info("AI生成的关键词: {}", aiKeywords);
                    })
                    .thenReturn(keywords);
        }

        log.info("提取的关键词: {}", keywords);
        return Mono.just(keywords);
    }

    /**
     * 当搜索结果为空时，补充更多关键词
     *
     * @param question 用户问题
     * @param currentKeywords 当前使用的关键词
     * @return 补充后的关键词列表
     */
    public Mono<List<String>> supplementKeywords(String question, List<String> currentKeywords) {
        log.info("当前搜索结果为空，尝试补充关键词");

        // 1. 基于问题模式补充关键词
        List<String> patternKeywords = getPatternBasedKeywords(question);
        List<String> supplemented = new ArrayList<>(currentKeywords);
        supplemented.addAll(patternKeywords);

        // 2. 如果补充后仍然无效，使用AI生成
        if (supplemented.size() <= currentKeywords.size() || supplemented.stream()
                .allMatch(k -> !StringUtils.hasText(k) || STOP_WORDS.contains(k))) {
            log.info("模式匹配无效，使用AI生成补充关键词");
            return generateKeywordsWithAI(question)
                    .doOnNext(aiKeywords -> {
                        supplemented.addAll(aiKeywords);
                        log.info("AI生成的补充关键词: {}", aiKeywords);
                    })
                    .thenReturn(supplemented);
        }

        log.info("补充后的关键词: {}", supplemented);
        return Mono.just(supplemented);
    }

    /**
     * 判断是否是文件列表查询
     *
     * @param question 用户问题
     * @return 是否是文件列表查询
     */
    private boolean isFileListQuery(String question) {
        if (!StringUtils.hasText(question)) {
            return false;
        }

        String lowerQuestion = question.toLowerCase().trim();

        // 检查是否是查询文件列表的通用问题
        return lowerQuestion.matches(".*(目前|现在|有哪些|有什么|什么|查看|列出).*文件.*")
                || lowerQuestion.matches(".*文件列表.*")
                || lowerQuestion.equals("目前有哪些文件")
                || lowerQuestion.equals("有哪些文件")
                || lowerQuestion.equals("有什么文件")
                || lowerQuestion.equals("列出文件")
                || lowerQuestion.equals("查看文件");
    }

    /**
     * 从问题中提取关键词
     *
     * @param question 问题
     * @return 提取的关键词列表
     */
    private List<String> extractFromQuestion(String question) {
        if (!StringUtils.hasText(question)) {
            return Collections.emptyList();
        }

        List<String> keywords = new ArrayList<>();

        // 1. 基于模式匹配
        for (Map.Entry<String, List<String>> entry : QUERY_PATTERNS.entrySet()) {
            if (question.contains(entry.getKey())) {
                keywords.addAll(entry.getValue());
            }
        }

        // 2. 提取文件名相关词汇
        Pattern filePattern = Pattern.compile("(视频|图片|文档|文件|mp4|jpg|png|pdf|doc|txt)");
        Matcher matcher = filePattern.matcher(question);
        while (matcher.find()) {
            keywords.add(matcher.group());
        }

        // 3. 移除停用词并去重
        return keywords.stream()
                .filter(k -> StringUtils.hasText(k) && !STOP_WORDS.contains(k))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 基于问题模式获取关键词
     *
     * @param question 问题
     * @return 关键词列表
     */
    private List<String> getPatternBasedKeywords(String question) {
        if (!StringUtils.hasText(question)) {
            return Collections.emptyList();
        }

        List<String> keywords = new ArrayList<>();

        // 检查是否是查询文件的通用问题
        if (question.contains("文件") && (question.contains("有哪些") || question.contains("什么") || question.contains("列表"))) {
            // 对于查询文件列表的问题，尝试搜索所有类型
            keywords.addAll(Arrays.asList("", "file", "文档", "视频", "图片"));
        }

        // 检查是否是查询特定类型的问题
        if (question.contains("视频") || question.contains("movie") || question.contains("video")) {
            keywords.addAll(Arrays.asList("video", "视频", "mp4", "avi"));
        }

        if (question.contains("图片") || question.contains("image") || question.contains("photo")) {
            keywords.addAll(Arrays.asList("image", "图片", "jpg", "png", "gif"));
        }

        if (question.contains("文档") || question.contains("doc") || question.contains("pdf")) {
            keywords.addAll(Arrays.asList("doc", "文档", "pdf", "txt", "word"));
        }

        return keywords.stream()
                .filter(k -> StringUtils.hasText(k) && !STOP_WORDS.contains(k))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 使用AI生成关键词
     *
     * @param question 用户问题
     * @return AI生成的关键词列表
     */
    private Mono<List<String>> generateKeywordsWithAI(String question) {
        String prompt = buildKeywordPrompt(question);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setRole("user");
        userMessage.setContent(prompt);

        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setMessages(List.of(userMessage));

        log.info("调用AI生成关键词...");
        return miniMaxService.chat(chatRequest)
                .map(this::parseAIResponse)
                .doOnError(error -> log.error("AI生成关键词失败", error))
                .onErrorReturn(Collections.emptyList());
    }

    /**
     * 构建关键词生成提示词
     *
     * @param question 用户问题
     * @return 提示词
     */
    private String buildKeywordPrompt(String question) {
        return "你是一个关键词提取助手。用户问了一个关于文件查询的问题，请提取出最适合用于文件搜索的关键词。\n\n" +
                "请遵循以下规则：\n" +
                "1. 提取3-5个最相关的关键词\n" +
                "2. 关键词应该能帮助搜索到相关文件\n" +
                "3. 可以包括文件类型、文件名特征、扩展名等\n" +
                "4. 如果问题是关于文件列表，关键词可以为空字符串或包含通用词汇\n" +
                "5. 仅返回关键词，用逗号分隔，不要任何解释\n\n" +
                "用户问题: " + question + "\n\n" +
                "请提取关键词:";
    }

    /**
     * 解析AI响应
     *
     * @param response AI响应
     * @return 关键词列表
     */
    private List<String> parseAIResponse(ChatResponse response) {
        try {
            if (response.getContent() != null && !response.getContent().isEmpty()) {
                for (ChatResponse.Content content : response.getContent()) {
                    if (content != null && "text".equals(content.getType()) && content.getText() != null) {
                        String text = content.getText().trim();
                        log.info("AI关键词响应: {}", text);

                        // 解析关键词（用逗号分割）
                        return Arrays.stream(text.split(","))
                                .map(String::trim)
                                .filter(k -> StringUtils.hasText(k) && !STOP_WORDS.contains(k))
                                .distinct()
                                .collect(Collectors.toList());
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析AI响应失败", e);
        }
        return Collections.emptyList();
    }
}
