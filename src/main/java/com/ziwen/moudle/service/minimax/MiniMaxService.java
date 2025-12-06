package com.ziwen.moudle.service.minimax;

import com.ziwen.moudle.dto.minimax.ChatRequest;
import com.ziwen.moudle.dto.minimax.ChatResponse;
import reactor.core.publisher.Mono;

/**
 * MiniMax AI 服务接口
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-06
 */
public interface MiniMaxService {

    /**
     * 发送聊天请求
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    Mono<ChatResponse> chat(ChatRequest request);

    /**
     * 发送简单聊天消息
     *
     * @param message 消息内容
     * @return 响应内容
     */
    Mono<String> simpleChat(String message);

    /**
     * 检查服务是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();
}