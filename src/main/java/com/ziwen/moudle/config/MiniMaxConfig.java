package com.ziwen.moudle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MiniMax AI 配置类
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-06
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minimax")
public class MiniMaxConfig {

    /**
     * API 密钥
     */
    private String apiKey;

    /**
     * API 基础 URL
     */
    private String baseUrl;

    /**
     * 请求超时时间（毫秒）
     */
    private int timeout = 30000;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 默认模型
     */
    private String defaultModel = "abab6.5s-chat";

    /**
     * 请求间隔（毫秒）
     */
    private int requestInterval = 100;
}