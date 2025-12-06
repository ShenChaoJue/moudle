package com.ziwen.moudle.dto.minimax;

import lombok.Data;


/**
 * 聊天响应 DTO
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-06
 */
@Data
public class ChatResponse {
    /**
     * 响应 ID
     */
    private String id;

    /**
     * 对象类型
     */
    private String type;

    /**
     * 角色（通常是 "assistant"）
     */
    private String role;

    /**
     * 内容
     */
    private java.util.List<Content> content;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 停止原因（stop, max_tokens, 等）
     */
    private String stop_reason;

    /**
     * 停止序列
     */
    private java.util.List<String> stop_sequence;

    /**
     * 使用统计
     */
    private Usage usage;

    @Data
    public static class Content {
        /**
         * 内容类型（通常是 "text"）
         */
        private String type;

        /**
         * 文本内容
         */
        private String text;
    }

    @Data
    public static class Usage {
        /**
         * 输入_tokens数量
         */
        private Integer input_tokens;

        /**
         * 输出_tokens数量
         */
        private Integer output_tokens;

        /**
         * 总_tokens数量
         */
        private Integer total_tokens;
    }
}