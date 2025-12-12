package com.ziwen.moudle.service.embedding;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ziwen.moudle.config.DashScopeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.*;

/**
 * 多模态向量化服务
 * 使用通义千问API进行文本和图片嵌入
 *
 * API调用模式：DashScope原生模式（vs OpenAI兼容模式）
 * 优势：支持稀疏向量、text_type参数、自定义任务说明
 *
 * API选择策略：
 * - 文本向量化：text-embedding-v4（DashScope原生模式）- 支持100+语种，8192 Token
 * - 图片向量化：tongyi-embedding-vision-plus（多模态向量API）- 支持图片和视频
 *
 * 请求格式对比：
 * - DashScope原生：{model, input: "文本"/["文本1","文本2"], dimension, text_type}
 * - OpenAI兼容：{model, input: ["文本1","文本2"], dimensions}
 *
 * @author : zixiwen
 * @version : 3.0 使用DashScope原生模式，符合官方规范
 * @date : 2025-12-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final DashScopeConfig dashScopeConfig;
    private final WebClient webClient = WebClient.builder().build();
    private final RestTemplate restTemplate;

    @Autowired
    public EmbeddingService(DashScopeConfig dashScopeConfig) {
        this.dashScopeConfig = dashScopeConfig;
        // 配置RestTemplate超时和连接
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30秒连接超时
        factory.setReadTimeout(60000); // 60秒读取超时

        this.restTemplate = new RestTemplate(factory);
        // 设置消息转换器，强制UTF-8
        this.restTemplate.getMessageConverters().forEach(converter -> {
            if (converter instanceof org.springframework.http.converter.StringHttpMessageConverter) {
                ((org.springframework.http.converter.StringHttpMessageConverter) converter).setDefaultCharset(StandardCharsets.UTF_8);
            }
        });
        log.info("RestTemplate初始化完成，连接超时: 30s, 读取超时: 60s, 编码: UTF-8");
    }


    /**
     * 彻底清洗文本，解决JSON非法字符问题
     */
    private String cleanTextForJson(String text) {
        if (text == null) return "";

        // 1. 移除所有不可见控制字符（包括零宽空格、换页符等）
        String clean = text.replaceAll("[\\x00-\\x1F\\x7F-\\x9F\\u200b\\u200c\\u200d]", "");
        // 2. 替换换行/回车/制表符为空格
        clean = clean.replaceAll("[\r\n\t]", " ");
        // 3. 替换全角空格为半角
        clean = clean.replaceAll("　", " ");
        // 4. 合并多个空格为一个，移除首尾空格
        clean = clean.replaceAll("\\s+", " ").trim();
        // 5. 兜底：空文本返回默认值
        return clean.isEmpty() ? "无有效内容" : clean;
    }

    /**
     * 文本向量化 - 用于查询（query类型）
     */
    public Mono<List<Float>> embedText(String text) {
        return embedText(text, "query");
    }

    /**
     * 文本向量化 - 可指定类型
     * @param text 文本内容
     * @param textType "query" 用于查询，"document" 用于文档入库
     */
    public Mono<List<Float>> embedText(String text, String textType) {
        return Mono.fromCallable(() -> {
            try {
                log.info("开始向量化文本 [类型: {}]: {}", textType, text.substring(0, Math.min(50, text.length())));

                // 1. 超级严格清理文本（彻底解决隐形字符问题）
                String cleanText = cleanTextForJson(text);

                // 2. 用JSONObject工具构建标准JSON请求体（符合DashScope原生API格式）
                JSONObject requestJson = new JSONObject();
                requestJson.put("model", "text-embedding-v4");

                // 关键修复：input 必须是 {"texts": ["文本"]} 格式
                JSONObject inputObj = new JSONObject();
                JSONArray textsArray = new JSONArray();
                textsArray.add(cleanText);
                inputObj.put("texts", textsArray);
                requestJson.put("input", inputObj);

                // parameters 也需要包装成对象
                JSONObject parameters = new JSONObject();
                parameters.put("dimension", 1024);
                parameters.put("text_type", textType);  // 区分 query 和 document
                requestJson.put("parameters", parameters);

                String requestBody = requestJson.toJSONString();
                log.debug("【JSON请求体】: {}", requestBody);

                // 3. 验证API Key
                String apiKey = dashScopeConfig.getApiKey();
                if (apiKey == null || apiKey.trim().isEmpty() || "YOUR_DASHSCOPE_API_KEY".equals(apiKey)) {
                    log.error("DashScope API Key 未配置或无效！当前值: {}", apiKey);
                    throw new RuntimeException("DashScope API Key 未配置或无效");
                }

                String fullUrl = dashScopeConfig.getBaseUrl() + "/services/embeddings/text-embedding/text-embedding";
                log.info("正在调用文本向量API: {}", fullUrl);

                // 3. 构建请求头（强制UTF-8编码）
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)); // 必须指定UTF-8
                headers.set("Authorization", "Bearer " + apiKey);
                // 关键：禁用压缩，避免内容被修改
                headers.set("Accept-Encoding", "identity");

                // 4. 发送请求（同步，无底层编码问题）
                HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> responseEntity = restTemplate.exchange(
                        fullUrl,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );

                // 6. 检查响应状态
                if (responseEntity.getStatusCode() != HttpStatus.OK) {
                    throw new RuntimeException("API响应失败，状态码：" + responseEntity.getStatusCode() +
                            "，响应体：" + responseEntity.getBody());
                }
                String response = responseEntity.getBody();
                log.info("API响应: {}", response);

                // 7. 解析向量
                JSONObject jsonResponse = JSONObject.parseObject(response);
                JSONObject output = jsonResponse.getJSONObject("output");
                if (output == null || !output.containsKey("embeddings")) {
                    throw new RuntimeException("响应无embeddings字段：" + response);
                }
                JSONArray embeddings = output.getJSONArray("embeddings");
                if (embeddings == null || embeddings.isEmpty()) {
                    throw new RuntimeException("embeddings数组为空：" + response);
                }
                JSONObject embeddingItem = embeddings.getJSONObject(0);
                JSONArray embedding = embeddingItem.getJSONArray("embedding");

                List<Float> vector = new ArrayList<>();
                for (int i = 0; i < embedding.size(); i++) {
                    vector.add(embedding.getFloat(i));
                }
                log.info("成功提取文本向量数据，维度: {}", vector.size());
                return vector;

            } catch (Exception e) {
                log.error("文本向量化失败", e);
                throw new RuntimeException("DashScope API调用失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 图片向量化 - 使用通义千问多模态Embedding API
     */
    public Mono<List<Float>> embedImage(String imageBase64) {
        log.info("开始向量化图片");

        // 构建通义千问多模态嵌入请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", dashScopeConfig.getEmbeddingModel());

        Map<String, Object> input = new HashMap<>();
        List<Map<String, String>> contents = new ArrayList<>();
        Map<String, String> contentItem = new HashMap<>();
        // 确保图片是 base64 格式
        if (!imageBase64.startsWith("data:")) {
            imageBase64 = "data:image/jpeg;base64," + imageBase64;
        }
        contentItem.put("image", imageBase64);
        contents.add(contentItem);
        input.put("contents", contents);
        requestBody.put("input", input);

        return webClient.post()
            .uri(dashScopeConfig.getBaseUrl() + "/services/embeddings/multimodal-embedding/multimodal-embedding")
            .header("Authorization", "Bearer " + dashScopeConfig.getApiKey())
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> {
                log.debug("通义千问API原始响应: {}", response);

                // 解析通义千问嵌入响应
                try {
                    JSONObject jsonResponse = JSONObject.parseObject(response);

                    // 从output.embeddings数组中提取向量
                    JSONObject output = jsonResponse.getJSONObject("output");
                    if (output != null) {
                        JSONArray embeddings = output.getJSONArray("embeddings");
                        if (embeddings != null && !embeddings.isEmpty()) {
                            JSONObject firstItem = embeddings.getJSONObject(0);
                            JSONArray embedding = firstItem.getJSONArray("embedding");

                            if (embedding != null) {
                                List<Float> vector = new ArrayList<>();
                                for (int i = 0; i < embedding.size(); i++) {
                                    vector.add(embedding.getFloat(i));
                                }
                                log.info("成功提取图片向量数据，维度: {}", vector.size());
                                return vector;
                            }
                        }
                    }

                    log.warn("未能从响应中提取向量数据");
                    return Collections.<Float>emptyList();

                } catch (Exception e) {
                    log.error("解析通义千问响应失败", e);
                    return Collections.<Float>emptyList();
                }
            })
            .doOnError(error -> log.error("图片向量化失败: {}", error.getMessage()));
    }

    /**
     * 批量文本向量化 - 终极修复：替换为RestTemplate避开WebClient底层编码坑
     */
    public Mono<List<List<Float>>> embedTexts(List<String> texts) {
        return Mono.fromCallable(() -> {
            try {
                log.info("批量向量化 {} 个文本", texts.size());

                // 1. 超级严格清理所有文本（彻底解决隐形字符问题）
                List<String> cleanedTexts = texts.stream()
                    .map(this::cleanTextForJson) // 使用统一清洗方法
                    .toList();

                log.info("最终清理后文本数量: {}", cleanedTexts.size());

                // 2. 用JSONObject工具构建标准JSON请求体（符合DashScope原生API格式）
                JSONObject requestJson = new JSONObject();
                requestJson.put("model", "text-embedding-v4");

                // 关键修复：批量文本格式 {"texts": ["文本1", "文本2"]}
                JSONObject inputObj = new JSONObject();
                JSONArray textsArray = new JSONArray();
                textsArray.addAll(cleanedTexts);
                inputObj.put("texts", textsArray);
                requestJson.put("input", inputObj);

                // parameters 也需要包装成对象
                JSONObject parameters = new JSONObject();
                parameters.put("dimension", 1024);
                parameters.put("text_type", "document");
                requestJson.put("parameters", parameters);

                String requestBody = requestJson.toJSONString();
                log.info("【工具构建的JSON请求体】: {}", requestBody);

                String fullUrl = dashScopeConfig.getBaseUrl() + "/services/embeddings/text-embedding/text-embedding";
                log.info("正在调用批量文本向量API: {}", fullUrl);

                // 3. 构建请求头（强制UTF-8编码）
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)); // 必须指定UTF-8
                headers.set("Authorization", "Bearer " + dashScopeConfig.getApiKey());
                headers.set("Accept-Encoding", "identity");

                // 4. 发送请求（同步，无底层编码问题）
                HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> responseEntity = restTemplate.exchange(
                        fullUrl,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );

                // 5. 检查响应状态
                if (responseEntity.getStatusCode() != HttpStatus.OK) {
                    throw new RuntimeException("API响应失败，状态码：" + responseEntity.getStatusCode() +
                            "，响应体：" + responseEntity.getBody());
                }
                String response = responseEntity.getBody();
                log.info("API响应: {}", response);

                // 6. 解析向量
                JSONObject jsonResponse = JSONObject.parseObject(response);
                JSONObject output = jsonResponse.getJSONObject("output");
                if (output == null || !output.containsKey("embeddings")) {
                    throw new RuntimeException("响应无embeddings字段：" + response);
                }
                JSONArray embeddings = output.getJSONArray("embeddings");
                if (embeddings == null || embeddings.isEmpty()) {
                    throw new RuntimeException("embeddings数组为空：" + response);
                }

                List<List<Float>> result = new ArrayList<>();
                for (int i = 0; i < embeddings.size(); i++) {
                    JSONObject item = embeddings.getJSONObject(i);
                    JSONArray embedding = item.getJSONArray("embedding");

                    if (embedding != null) {
                        List<Float> vector = new ArrayList<>();
                        for (int j = 0; j < embedding.size(); j++) {
                            vector.add(embedding.getFloat(j));
                        }
                        result.add(vector);
                    }
                }
                log.info("批量文本向量化完成，共 {} 个向量，维度: {}",
                    result.size(), result.isEmpty() ? 0 : result.get(0).size());
                return result;

            } catch (Exception e) {
                log.error("批量文本向量化失败", e);
                throw new RuntimeException("DashScope API调用失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 单个文本向量化（兼容方法）
     */
    public Mono<List<Float>> embedSingleText(String text) {
        return embedText(text);
    }

    /**
     * 批量图片向量化
     */
    public Mono<List<List<Float>>> embedImages(List<String> imageBase64List) {
        log.info("批量向量化 {} 个图片", imageBase64List.size());

        List<Mono<List<Float>>> monoList = imageBase64List.stream()
                .map(this::embedImage)
                .toList();

        return Mono.zip(monoList, objects -> {
            List<List<Float>> result = new ArrayList<>();
            for (Object obj : objects) {
                @SuppressWarnings("unchecked")
                List<Float> floatList = (List<Float>) obj;
                result.add(floatList);
            }
            return result;
        });
    }
}
