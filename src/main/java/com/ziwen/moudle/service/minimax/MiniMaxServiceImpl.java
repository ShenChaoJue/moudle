package com.ziwen.moudle.service.minimax;

import com.ziwen.moudle.config.MiniMaxConfig;
import com.ziwen.moudle.dto.minimax.ChatMessage;
import com.ziwen.moudle.dto.minimax.ChatRequest;
import com.ziwen.moudle.dto.minimax.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * MiniMax AI 服务实现
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-06
 */
@Slf4j
@Service
public class MiniMaxServiceImpl implements MiniMaxService {

    private final WebClient webClient;
    private final MiniMaxConfig config;

    public MiniMaxServiceImpl(MiniMaxConfig config) {
        this.config = config;
        this.webClient = WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .build();
    }

    @Override
    public Mono<ChatResponse> chat(ChatRequest request) {
        // 设置默认模型
        if (request.getModel() == null || request.getModel().isEmpty()) {
            request.setModel(config.getDefaultModel());
        }

        log.info("发送请求到 MiniMax AI, 模型: {}, 消息数: {}",
                request.getModel(),
                request.getMessages() != null ? request.getMessages().size() : 0);

        return webClient.post()
                .uri("/v1/messages")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .retryWhen(Retry.backoff(config.getMaxRetries(), Duration.ofMillis(500))
                        .filter(this::isRetryableError)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            log.error("MiniMax AI 请求重试失败，已达到最大重试次数");
                            return retrySignal.failure();
                        }))
                .timeout(Duration.ofMillis(config.getTimeout()))
                .doOnNext(response -> log.info("MiniMax AI 响应成功, ID: {}", response.getId()))
                .doOnError(error -> log.error("MiniMax AI 请求失败: {}", error.getMessage()));
    }

    @Override
    public Mono<String> simpleChat(String message) {
        ChatMessage userMessage = new ChatMessage();
        userMessage.setRole("user");
        userMessage.setContent(message);

        ChatRequest request = new ChatRequest();
        request.setMessages(Collections.singletonList(userMessage));

        return chat(request)
                .map(response -> {
                    if (response.getContent() != null && !response.getContent().isEmpty()) {
                        return response.getContent().get(0).getText();
                    }
                    return "抱歉，未获取到有效响应";
                });
    }

    @Override
    public boolean isAvailable() {
        try {
            // 发送一个简单的测试请求
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            simpleChat("你好")
                    .subscribe(
                            response -> {
                                log.info("MiniMax AI 服务可用性测试成功");
                                future.complete(true);
                            },
                            error -> {
                                log.error("MiniMax AI 服务可用性测试失败: {}", error.getMessage());
                                future.complete(false);
                            });

            return future.get();
        } catch (Exception e) {
            log.error("检查 MiniMax AI 服务可用性时发生异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 判断错误是否可重试
     */
    private boolean isRetryableError(Throwable error) {
        if (error instanceof WebClientResponseException webClientError) {
            int statusCode = webClientError.getStatusCode().value();
            // 5xx 错误和 429 限流错误可以重试
            return statusCode >= 500 || statusCode == 429;
        }
        return false;
    }
}