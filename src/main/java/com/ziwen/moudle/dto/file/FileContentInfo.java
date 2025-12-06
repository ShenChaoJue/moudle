package com.ziwen.moudle.dto.file;

import lombok.Builder;
import lombok.Data;

/**
 * 文件内容信息 DTO
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-06
 */
@Data
@Builder
public class FileContentInfo {

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
     * 文件类型
     */
    private String contentType;

    /**
     * 文件内容
     */
    private String content;

    /**
     * 内容摘要
     */
    private String summary;

    /**
     * 文件编码
     */
    private String encoding;

    /**
     * 内容长度
     */
    private Integer contentLength;

    /**
     * 获取内容预览（前100字符）
     */
    public String getPreview() {
        if (content == null || content.isEmpty()) {
            return "";
        }
        int maxLength = 100;
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}