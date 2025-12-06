package com.ziwen.moudle.dto.ai;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 基于文件的AI问答请求 DTO
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileBasedQARequest {

    /**
     * 用户问题
     */
    private String question;

    /**
     * 搜索关键词（用于匹配相关文件，支持中英文）
     */
    private String keyword;

    /**
     * 模糊文件名（用于匹配相关文件）
     */
    private String fuzzyFileName;

    /**
     * 最大文件数量（默认5）
     */
    private Integer maxFiles;

    /**
     * 是否使用AI总结（可选）
     */
    private Boolean useSummary;
}