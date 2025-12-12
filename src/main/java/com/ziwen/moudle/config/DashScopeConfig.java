package com.ziwen.moudle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DashScope API配置（阿里云多模态AI服务）
 *
 * 调用模式说明：
 * - DashScope原生模式：baseUrl=https://dashscope.aliyuncs.com/api/v1
 *   优势：支持稀疏向量、text_type参数、instruct参数
 *   请求格式：{model, input: "文本"/["文本1","文本2"], dimension, text_type}
 *
 * - OpenAI兼容模式：baseUrl=https://dashscope.aliyuncs.com/compatible-mode/v1
 *   优势：适配OpenAI生态，无需改动原有调用逻辑
 *   请求格式：{model, input: ["文本1","文本2"], dimensions}
 *
 * 当前使用：DashScope原生模式（支持更多功能）
 *
 * @author : zixiwen
 * @version : 2.0 添加模式说明
 * @date : 2025-12-08
 */
@Configuration
@ConfigurationProperties(prefix = "dashscope")
@Data
public class DashScopeConfig {
    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API基础URL
     */
    private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";

    /**
     * 通义千问VL模型（用于图片理解）
     */
    private String qwenVlModel = "qwen-vl-v1";

    /**
     * 嵌入模型（用于多模态向量化）- 推荐使用官方文档中的模型
     */
    private String embeddingModel = "tongyi-embedding-vision-plus";
}
