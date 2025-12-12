package com.ziwen.moudle.service.ai;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ziwen.moudle.config.DashScopeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * 视觉语言服务（通义千问VL）
 * 用于提取图片的描述和关键词
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VisionLanguageService {

    private final DashScopeConfig dashScopeConfig;
    private final WebClient webClient = WebClient.builder().build();

    /**
     * 提取图片描述
     *
     * @param imageBase64 Base64编码的图片数据
     * @return 图片描述文本
     */
    public Mono<String> describeImage(String imageBase64) {
        log.info("开始提取图片描述");

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", dashScopeConfig.getQwenVlModel());

        // 构建输入内容
        List<Map<String, Object>> input = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        content.put("image", "data:image/jpeg;base64," + imageBase64);
        content.put("text", "请详细描述这张图片，包括：1. 图片主要内容；2. 关键物体和场景；3. 颜色、动作等细节；4. 如果是技术文档或图表，请描述其核心信息。请用简洁的中文回答。");
        input.add(content);
        requestBody.put("input", input);

        return webClient.post()
            .uri(dashScopeConfig.getBaseUrl() + "/services/aigc/multimodal-generation/generation")
            .header("Authorization", "Bearer " + dashScopeConfig.getApiKey())
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> {
                log.debug("通义千问VL原始响应: {}", response);

                try {
                    JSONObject jsonResponse = JSONObject.parseObject(response);
                    JSONArray results = jsonResponse.getJSONArray("results");

                    if (results != null && !results.isEmpty()) {
                        JSONObject firstResult = results.getJSONObject(0);
                        JSONObject output = firstResult.getJSONObject("output");
                        JSONArray choices = output.getJSONArray("choices");

                        if (choices != null && !choices.isEmpty()) {
                            JSONObject choice = choices.getJSONObject(0);
                            String description = choice.getString("message");
                            log.info("成功提取图片描述，长度: {}", description.length());
                            return description;
                        }
                    }

                    log.warn("未能从响应中提取图片描述");
                    return "无法识别图片内容";

                } catch (Exception e) {
                    log.error("解析图片描述响应失败", e);
                    return "图片描述提取失败: " + e.getMessage();
                }
            })
            .doOnError(error -> log.error("图片描述提取失败: {}", error.getMessage()));
    }

    /**
     * 提取图片关键词
     *
     * @param imageBase64 Base64编码的图片数据
     * @return 关键词列表
     */
    public Mono<List<String>> extractKeywords(String imageBase64) {
        log.info("开始提取图片关键词");

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", dashScopeConfig.getQwenVlModel());

        // 构建输入内容
        List<Map<String, Object>> input = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        content.put("image", "data:image/jpeg;base64," + imageBase64);
        content.put("text", "请提取这张图片的5-10个关键词，用于检索。请用逗号分隔的关键词列表格式回答，例如：汽车, 红色, 城市");
        input.add(content);
        requestBody.put("input", input);

        return webClient.post()
            .uri(dashScopeConfig.getBaseUrl() + "/services/aigc/multimodal-generation/generation")
            .header("Authorization", "Bearer " + dashScopeConfig.getApiKey())
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> {
                log.debug("通义千问VL关键词响应: {}", response);

                try {
                    JSONObject jsonResponse = JSONObject.parseObject(response);
                    JSONArray results = jsonResponse.getJSONArray("results");

                    if (results != null && !results.isEmpty()) {
                        JSONObject firstResult = results.getJSONObject(0);
                        JSONObject output = firstResult.getJSONObject("output");
                        JSONArray choices = output.getJSONArray("choices");

                        if (choices != null && !choices.isEmpty()) {
                            JSONObject choice = choices.getJSONObject(0);
                            String keywordsText = choice.getString("message");

                            // 解析关键词
                            List<String> keywords = Arrays.asList(
                                keywordsText.split("[,，、\\s]+")
                            );

                            log.info("成功提取 {} 个关键词", keywords.size());
                            return keywords;
                        }
                    }

                    log.warn("未能从响应中提取关键词");
                    return List.of("未知");

                } catch (Exception e) {
                    log.error("解析关键词响应失败", e);
                    return List.of("提取失败");
                }
            })
            .doOnError(error -> log.error("关键词提取失败: {}", error.getMessage()));
    }
}
