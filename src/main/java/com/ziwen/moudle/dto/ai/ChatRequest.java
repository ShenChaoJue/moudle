package com.ziwen.moudle.dto.ai;

import lombok.Data;

import java.util.List;

/**
 * 聊天请求DTO
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-08
 */
@Data
public class ChatRequest {
    private String model;
    private List<ChatMessage> messages;
    private Double temperature;
    private Integer maxTokens;
}