package com.ziwen.moudle.dto.minimax;

import lombok.Data;

/**
 * 聊天消息 DTO
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-06
 */
@Data
public class ChatMessage {
    /**
     * 消息角色：system, user, assistant
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;
}