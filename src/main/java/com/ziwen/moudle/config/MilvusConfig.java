package com.ziwen.moudle.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 向量数据库配置
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-07
 */
@Configuration
@ConfigurationProperties(prefix = "milvus")
@Data
public class MilvusConfig {

    private String host = "localhost";
    private Integer port = 19530;
    private String collectionName = "multimodal_search";
    private Integer vectorDim = 1024;  // 通义千问多模态embedding维度
    private String indexType = "IVF_FLAT";  // 索引类型：IVF_FLAT 或 HNSW
    private Integer nlist = 1024;  // IVF_FLAT的聚类数量
    private Integer m = 64;  // HNSW的最大邻居数
    private Integer efConstruction = 100;  // HNSW的候选邻居数
    
    @Bean
    public MilvusServiceClient milvusClient() {
        return new MilvusServiceClient(
            ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .build()
        );
    }
}

