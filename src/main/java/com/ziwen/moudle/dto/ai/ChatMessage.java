package com.ziwen.moudle.dto.ai;

import lombok.Data;

/**
 * 聊天消息DTO
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-08
 */
@Data
public class ChatMessage {
    private String role;
    private String content;
}