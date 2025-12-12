package com.ziwen.moudle.service.file;

import com.ziwen.moudle.entity.file.FileEntity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 文档解析服务
 * 支持多种文件格式的文本提取
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-07
 */
@Slf4j
@Service
public class DocumentParserService {

    /**
     * 解析结果
     */
    @Data
    public static class ParseResult {
        private String text;              // 提取的文本
        private boolean canChunk;         // 是否可以切片
        private String fileType;          // 文件类型
        private String warning;           // 警告信息
    }

    /**
     * 解析文件
     */
    public ParseResult parse(FileEntity file) {
        log.info("解析文件: {}, 类型: {}", file.getOriginalName(), file.getContentType());
        
        String contentType = file.getContentType().toLowerCase();
        
        // 1. 纯文本文件
        if (isTextFile(contentType)) {
            return parseTextFile(file);
        }
        
        // 2. Office文档（暂不实现，需要POI依赖）
        if (isOfficeFile(contentType)) {
            return parseOfficeFile(file);
        }
        
        // 3. PDF文档（暂不实现，需要PDFBox依赖）
        if (isPDF(contentType)) {
            return parsePDFFile(file);
        }
        
        // 4. 媒体文件（视频、图片、音频）
        if (isMediaFile(contentType)) {
            return parseMediaFile(file);
        }
        
        // 5. 不支持的类型
        ParseResult result = new ParseResult();
        result.setCanChunk(false);
        result.setText("不支持的文件类型: " + contentType);
        return result;
    }

    /**
     * 解析纯文本文件
     */
    private ParseResult parseTextFile(FileEntity file) {
        try {
            // 检查文件大小，如果太大则拒绝处理，现在支持最大50MB
            if (file.getFileSize() > 50 * 1024 * 1024) { // 50MB
                log.warn("文件过大，无法处理: {} bytes", file.getFileSize());
                ParseResult result = new ParseResult();
                result.setCanChunk(false);
                result.setWarning("文件过大，无法处理，请使用小于50MB的文件");
                return result;
            }
            
            // 使用File类确保路径分隔符正确，然后转换为Path
            File textFile = new File(file.getFilePath());
            String text = Files.readString(textFile.toPath(), StandardCharsets.UTF_8);

            ParseResult result = new ParseResult();
            result.setText(text);
            result.setCanChunk(true);
            result.setFileType("文本文件");

            log.info("文本文件解析成功，长度: {}", text.length());
            return result;

        } catch (IOException e) {
            log.error("文本文件解析失败，路径: {}", file.getFilePath(), e);
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        }
    }

    /**
     * 解析Office文档（TODO: 需要Apache POI）
     */
    private ParseResult parseOfficeFile(FileEntity file) {
        ParseResult result = new ParseResult();
        result.setCanChunk(false);
        result.setText("Office文档解析功能开发中，暂不支持");
        result.setFileType("Office文档");
        result.setWarning("需要添加Apache POI依赖");
        return result;
    }

    /**
     * 解析PDF文档（TODO: 需要PDFBox）
     */
    private ParseResult parsePDFFile(FileEntity file) {
        ParseResult result = new ParseResult();
        result.setCanChunk(false);
        result.setText("PDF文档解析功能开发中，暂不支持");
        result.setFileType("PDF文档");
        result.setWarning("需要添加PDFBox依赖");
        return result;
    }

    /**
     * 解析媒体文件（提取元数据）
     */
    private ParseResult parseMediaFile(FileEntity file) {
        StringBuilder metadata = new StringBuilder();
        metadata.append("【媒体文件 - 仅元数据，无法查看具体内容】\n\n");
        metadata.append("文件名: ").append(file.getOriginalName()).append("\n");
        metadata.append("文件类型: ").append(file.getContentType()).append("\n");
        metadata.append("文件大小: ").append(formatFileSize(file.getFileSize())).append("\n");
        metadata.append("上传时间: ").append(file.getUploadTime()).append("\n\n");
        metadata.append("⚠️ 注意: 此文件为媒体文件，仅支持查询文件信息，不能查看具体内容。");
        
        ParseResult result = new ParseResult();
        result.setText(metadata.toString());
        result.setCanChunk(true);  // 元数据可以作为单个片段
        result.setFileType("媒体文件");
        result.setWarning("仅元数据，不包含实际内容");
        
        log.info("媒体文件元数据提取完成: {}", file.getOriginalName());
        return result;
    }

    // 判断文件类型
    private boolean isTextFile(String contentType) {
        return contentType.startsWith("text/") || 
               contentType.contains("json") || 
               contentType.contains("xml");
    }

    private boolean isOfficeFile(String contentType) {
        return contentType.contains("word") || 
               contentType.contains("excel") || 
               contentType.contains("powerpoint") ||
               contentType.contains("officedocument");
    }

    private boolean isPDF(String contentType) {
        return contentType.contains("pdf");
    }

    private boolean isMediaFile(String contentType) {
        return contentType.startsWith("video/") || 
               contentType.startsWith("audio/") || 
               contentType.startsWith("image/");
    }

    // 格式化文件大小
    private String formatFileSize(Long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / 1024.0 / 1024.0);
        return String.format("%.2f GB", size / 1024.0 / 1024.0 / 1024.0);
    }
}

