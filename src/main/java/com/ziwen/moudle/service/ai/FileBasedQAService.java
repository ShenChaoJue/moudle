package com.ziwen.moudle.service.ai;

import com.ziwen.moudle.dto.ai.FileBasedQARequest;
import com.ziwen.moudle.dto.ai.FileBasedQAResponse;
import com.ziwen.moudle.dto.ai.ChatMessage;
import com.ziwen.moudle.dto.ai.ChatRequest;
import com.ziwen.moudle.dto.ai.ChatResponse;
import com.ziwen.moudle.entity.file.FileChunkEntity;
import com.ziwen.moudle.service.file.ChunkRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于文件的AI问答服务（RAG版本）
 */
@Slf4j
@Service
public class FileBasedQAService {

    private final ChunkRetrievalService chunkRetrievalService;
    private final QwenService qwenService;

    public FileBasedQAService(ChunkRetrievalService chunkRetrievalService,
                            QwenService qwenService) {
        this.chunkRetrievalService = chunkRetrievalService;
        this.qwenService = qwenService;
    }

    /**
     * 回答问题（RAG流程）
     */
    public Mono<FileBasedQAResponse> answerQuestion(FileBasedQARequest request) {
        log.info("处理问题: {}", request.getQuestion());

        int topK = request.getMaxFiles() != null ? request.getMaxFiles() : 5;

        // 1. 检索相似片段
        return chunkRetrievalService.retrieveSimilarChunks(request.getQuestion(), topK)
                .flatMap(chunks -> {
                    log.info("检索到 {} 个相关片段", chunks.size());

                    // 2. 构建Prompt
                    String prompt = buildPrompt(request.getQuestion(), chunks);

                    // 3. 调用LLM
                    ChatMessage message = new ChatMessage();
                    message.setRole("user");
                    message.setContent(prompt);

                    ChatRequest chatRequest = new ChatRequest();
                    chatRequest.setMessages(List.of(message));

                    return qwenService.chat(chatRequest)
                            .map(response -> buildResponse(request, chunks, response));
                })
                .onErrorResume(error -> {
                    log.error("问答失败", error);
                    return Mono.just(FileBasedQAResponse.builder()
                            .question(request.getQuestion())
                            .answer("抱歉，处理失败: " + error.getMessage())
                            .error(error.getMessage())
                            .build());
                });
    }

    /**
     * 构建Prompt
     */
    private String buildPrompt(String question, List<FileChunkEntity> chunks) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("根据以下文档片段回答问题：\n\n");

        for (int i = 0; i < chunks.size(); i++) {
            prompt.append("[片段 ").append(i + 1).append("]\n");
            prompt.append(chunks.get(i).getChunkText()).append("\n\n");
        }

        prompt.append("问题: ").append(question).append("\n");
        prompt.append("请基于以上片段回答，如果片段中没有相关信息，请说明。");

        return prompt.toString();
    }

    /**
     * 构建响应
     */
    private FileBasedQAResponse buildResponse(FileBasedQARequest request,
                                             List<FileChunkEntity> chunks,
                                             ChatResponse aiResponse) {
        String answer = "";
        if (aiResponse.getChoices() != null && !aiResponse.getChoices().isEmpty()) {
            for (ChatResponse.Content content : aiResponse.getChoices()) {
                if (content.getMessage() != null && content.getMessage().getContent() != null) {
                    answer = content.getMessage().getContent();
                    break;
                }
            }
        }

        // 构建引用片段信息
        List<FileBasedQAResponse.ReferencedFile> references = chunks.stream()
                .map(chunk -> FileBasedQAResponse.ReferencedFile.builder()
                        .fileId(chunk.getFileId())
                        .fileName("片段 " + chunk.getChunkIndex())
                        .preview(chunk.getChunkText().substring(0, Math.min(100, chunk.getChunkText().length())))
                        .build())
                .collect(Collectors.toList());

        return FileBasedQAResponse.builder()
                .question(request.getQuestion())
                .answer(answer)
                .referencedFiles(references)
                .filesCount(chunks.size())
                .model(aiResponse.getModel())
                .usage(aiResponse.getUsage())
                .timestamp(LocalDateTime.now())
                .build();
    }
}

