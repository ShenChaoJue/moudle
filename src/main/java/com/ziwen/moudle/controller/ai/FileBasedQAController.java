package com.ziwen.moudle.controller.ai;

import com.ziwen.moudle.common.AjaxResult;
import com.ziwen.moudle.dto.ai.FileBasedQARequest;
import com.ziwen.moudle.dto.ai.FileBasedQAResponse;
import com.ziwen.moudle.service.ai.FileBasedQAService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 基于文件的AI问答控制器
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-06
 */
@Slf4j
@RestController
@RequestMapping("/api/qa")
@Tag(name = "基于文件的AI问答", description = "基于存储文件的AI问答接口")
public class FileBasedQAController {

    private final FileBasedQAService fileBasedQAService;

    public FileBasedQAController(FileBasedQAService fileBasedQAService) {
        this.fileBasedQAService = fileBasedQAService;
    }

    @PostMapping("/ask")
    @Operation(summary = "智能问答", description = "基于文件内容回答问题")
    public Mono<AjaxResult> askQuestion(@RequestBody(required = false) FileBasedQARequest request) {
        // 检查请求体是否为空
        if (request == null) {
            log.warn("收到空请求体");
            return Mono.just(AjaxResult.error("请求体不能为空"));
        }

        // 检查问题是否为空
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            log.warn("收到空问题");
            return Mono.just(AjaxResult.error("问题不能为空"));
        }

        log.info("收到AI问答请求，问题: {}", request.getQuestion());
        return fileBasedQAService.answerQuestion(request)
                .map(response -> AjaxResult.success("问答完成", response))
                .onErrorResume(error -> {
                    log.error("AI问答处理失败", error);
                    return Mono.just(AjaxResult.error("问答处理失败: " + error.getMessage()));
                });
    }

    @GetMapping("/simple-ask")
    @Operation(summary = "简单问答", description = "直接传入问题，自动搜索相关文件")
    public Mono<AjaxResult> simpleAsk(@RequestParam String question) {
        log.info("收到简单问答请求，问题: {}", question);
        return fileBasedQAService.simpleQuestion(question)
                .map(response -> AjaxResult.success("问答完成", response))
                .onErrorResume(error -> {
                    log.error("简单问答处理失败", error);
                    return Mono.just(AjaxResult.error("问答处理失败: " + error.getMessage()));
                });
    }

    @PostMapping("/batch-ask")
    @Operation(summary = "批量问答", description = "批量处理多个问题")
    public Mono<AjaxResult> batchAsk(@RequestBody List<FileBasedQARequest> requests) {
        log.info("收到批量问答请求，数量: {}", requests.size());

        // 串行处理所有问题
        return Mono.fromCallable(() -> requests)
                .flatMapIterable(new java.util.function.Function<List<FileBasedQARequest>, Iterable<FileBasedQARequest>>() {
                    @Override
                    public Iterable<FileBasedQARequest> apply(List<FileBasedQARequest> requestsList) {
                        return requestsList;
                    }
                })
                .flatMap(request -> fileBasedQAService.answerQuestion(request)
                        .onErrorReturn(FileBasedQAResponse.builder()
                                .question(request.getQuestion())
                                .answer("处理失败: " + "未知错误")
                                .error("未知错误")
                                .build()))
                .collectList()
                .map(responses -> AjaxResult.success("批量问答完成", responses))
                .onErrorResume(error -> {
                    log.error("批量问答处理失败", error);
                    return Mono.just(AjaxResult.error("批量问答失败: " + error.getMessage()));
                });
    }
}