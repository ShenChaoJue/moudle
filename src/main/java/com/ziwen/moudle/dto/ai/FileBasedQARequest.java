package com.ziwen.moudle.dto.ai;

import lombok.Data;

/**
 * 基于文件的AI问答请求
 *
 * @author : zixiwen
 * @date : 2025-12-08
 */
@Data
public class FileBasedQARequest {

    /**
     * 问题内容（必填）
     */
    private String question;

    /**
     * 最大检索文件数量（可选，默认5）
     */
    private Integer maxFiles;

    /**
     * 模型名称（可选，默认使用MiniMax默认模型）
     */
    private String model;

    /**
     * 检索阈值（可选，0-1之间，越高越严格，默认0.7）
     */
    private Double similarityThreshold;

    /**
     * 是否包含文件预览（可选，默认true）
     */
    private Boolean includePreview;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Integer getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(Integer maxFiles) {
        this.maxFiles = maxFiles;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(Double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public Boolean getIncludePreview() {
        return includePreview;
    }

    public void setIncludePreview(Boolean includePreview) {
        this.includePreview = includePreview;
    }
}
