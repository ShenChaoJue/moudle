package com.ziwen.moudle.controller.minimax;

import com.ziwen.moudle.common.AjaxResult;
import com.ziwen.moudle.dto.minimax.ChatRequest;
import com.ziwen.moudle.service.minimax.MiniMaxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * MiniMax AI 控制器
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-06
 */
@Slf4j
@RestController
@RequestMapping("/api/minimax")
@Tag(name = "MiniMax AI", description = "MiniMax AI 聊天接口")
public class MiniMaxController {

    private final MiniMaxService miniMaxService;

    public MiniMaxController(MiniMaxService miniMaxService) {
        this.miniMaxService = miniMaxService;
    }

    @PostMapping("/chat")
    @Operation(summary = "发送聊天请求", description = "发送消息到 MiniMax AI 获取响应")
    public Mono<AjaxResult> chat(@RequestBody(required = false) ChatRequest request) {
        log.info("收到 MiniMax AI 聊天请求: {}", request);
        // 如果请求体为空，返回错误信息
        if (request == null) {
            return Mono.just(AjaxResult.error("请求体不能为空"));
        }
        return miniMaxService.chat(request)
                .map(response -> AjaxResult.success("请求成功", response))
                .onErrorResume(error -> {
                    log.error("MiniMax AI 请求失败: {}", error.getMessage());
                    return Mono.just(AjaxResult.error("AI 请求失败: " + error.getMessage()));
                });
    }

    @PostMapping("/simple-chat")
    @Operation(summary = "简单聊天", description = "发送简单消息获取 AI 响应")
    public Mono<AjaxResult> simpleChat(@RequestParam String message) {
        log.info("收到简单聊天请求: {}", message);
        return miniMaxService.simpleChat(message)
                .map(response -> AjaxResult.success("请求成功", response))
                .onErrorResume(error -> {
                    log.error("简单聊天请求失败: {}", error.getMessage());
                    return Mono.just(AjaxResult.error("聊天失败: " + error.getMessage()));
                });
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查 MiniMax AI 服务可用性")
    public AjaxResult health() {
        log.info("检查 MiniMax AI 服务健康状态");
        boolean available = miniMaxService.isAvailable();
        if (available) {
            return AjaxResult.success("服务正常", true);
        } else {
            return AjaxResult.error("服务不可用");
        }
    }
}