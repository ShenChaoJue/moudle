package com.ziwen.moudle.dto.ai;

import com.ziwen.moudle.dto.minimax.ChatResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 基于文件的AI问答响应 DTO
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-06
 */
@Data
@Builder
public class FileBasedQAResponse {

    /**
     * 用户问题
     */
    private String question;

    /**
     * AI回答
     */
    private String answer;

    /**
     * 引用的文件列表
     */
    private List<ReferencedFile> referencedFiles;

    /**
     * 引用文件数量
     */
    private Integer filesCount;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * Token使用统计
     */
    private ChatResponse.Usage usage;

    /**
     * 响应时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 错误信息（如果发生错误）
     */
    private String error;

    @Data
    @Builder
    public static class ReferencedFile {
        /**
         * 文件ID
         */
        private Long fileId;

        /**
         * 文件名
         */
        private String fileName;

        /**
         * 文件路径
         */
        private String filePath;

        /**
         * 文件内容预览（前100字符）
         */
        private String preview;
    }
}