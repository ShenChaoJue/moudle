package com.ziwen.moudle.service.ai;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ziwen.moudle.config.DashScopeConfig;
import com.ziwen.moudle.dto.ai.ChatMessage;
import com.ziwen.moudle.dto.ai.ChatRequest;
import com.ziwen.moudle.dto.ai.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * 通义千问API服务
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QwenService {

    private final DashScopeConfig dashScopeConfig;
    private final WebClient webClient = WebClient.builder().build();

    /**
     * 聊天对话
     */
    public Mono<ChatResponse> chat(ChatRequest request) {
        String model = request.getModel() != null ? request.getModel() : "qwen-plus";
        log.info("开始调用通义千问API，模型: {}", model);

        // 构建符合DashScope规范的请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);

        // input 包含 messages
        JSONObject input = new JSONObject();
        JSONArray messages = new JSONArray();
        for (ChatMessage msg : request.getMessages()) {
            JSONObject message = new JSONObject();
            message.put("role", msg.getRole());
            message.put("content", msg.getContent());
            messages.add(message);
        }
        input.put("messages", messages);
        requestBody.put("input", input);

        // parameters 包含可选参数
        JSONObject parameters = new JSONObject();
        if (request.getTemperature() != null) {
            parameters.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            parameters.put("max_tokens", request.getMaxTokens());
        }
        parameters.put("result_format", "message");
        requestBody.put("parameters", parameters);

        log.info("请求体: {}", requestBody.toJSONString());

        return webClient.post()
            .uri(dashScopeConfig.getBaseUrl() + "/services/aigc/text-generation/generation")
            .header("Authorization", "Bearer " + dashScopeConfig.getApiKey())
            .header("Content-Type", "application/json")
            .bodyValue(requestBody.toJSONString())
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> {
                log.debug("通义千问API原始响应: {}", response);

                try {
                    return parseChatResponse(response);
                } catch (Exception e) {
                    log.error("解析通义千问响应失败", e);
                    throw new RuntimeException("解析响应失败", e);
                }
            })
            .doOnError(error -> log.error("通义千问API调用失败: {}", error.getMessage()));
    }



    /**
     * 解析聊天响应
     */
    private ChatResponse parseChatResponse(String response) {
        JSONObject jsonResponse = JSONObject.parseObject(response);

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setId(jsonResponse.getString("request_id"));
        chatResponse.setObject("chat.completion");
        chatResponse.setCreated(System.currentTimeMillis() / 1000);

        // 解析output
        JSONObject output = jsonResponse.getJSONObject("output");
        if (output != null) {
            JSONArray choices = output.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                List<ChatResponse.Content> contentList = new ArrayList<>();
                for (int i = 0; i < choices.size(); i++) {
                    JSONObject choice = choices.getJSONObject(i);
                    ChatResponse.Content content = new ChatResponse.Content();
                    content.setIndex(i);

                    // 解析message
                    JSONObject messageObj = choice.getJSONObject("message");
                    ChatResponse.Message message = new ChatResponse.Message();
                    message.setRole(messageObj.getString("role"));
                    message.setContent(messageObj.getString("content"));
                    content.setMessage(message);

                    content.setFinishReason(choice.getString("finish_reason"));
                    contentList.add(content);
                }
                chatResponse.setChoices(contentList);
            }
        }

        // 解析usage
        JSONObject usageObj = jsonResponse.getJSONObject("usage");
        if (usageObj != null) {
            ChatResponse.Usage usage = new ChatResponse.Usage();
            usage.setPromptTokens(usageObj.getInteger("input_tokens"));
            usage.setCompletionTokens(usageObj.getInteger("output_tokens"));
            usage.setTotalTokens(usageObj.getInteger("total_tokens"));
            chatResponse.setUsage(usage);
        }

        return chatResponse;
    }
}