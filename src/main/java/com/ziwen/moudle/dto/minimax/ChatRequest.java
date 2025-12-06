package com.ziwen.moudle.dto.minimax;

import lombok.Data;

import java.util.List;

/**
 * 聊天请求 DTO
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-06
 */
@Data
public class ChatRequest {
    /**
     * 模型名称
     */
    private String model;

    /**
     * 消息列表
     */
    private List<ChatMessage> messages;

    /**
     * 最大_tokens（可选）
     */
    private Integer max_tokens;

    /**
     * 温度（可选，0-1）
     */
    private Double temperature;

    /**
     * 随机种子（可选）
     */
    private Integer seed;

    /**
     * 流式输出（可选）
     */
    private Boolean stream = false;

    /**
     * 停止序列（可选，Claude API 特有）
     */
    private List<String> stop_sequences;

    /**
     * 系统提示（可选，Claude API 特有）
     */
    private String system;
}