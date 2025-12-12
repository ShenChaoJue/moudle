package com.ziwen.moudle.dto.ai;

import lombok.Data;

import java.util.List;

/**
 * 聊天响应DTO
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-08
 */
@Data
public class ChatResponse {
    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Content> choices;
    private Usage usage;

    @Data
    public static class Content {
        private Integer index;
        private Message message;
        private String finishReason;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
}