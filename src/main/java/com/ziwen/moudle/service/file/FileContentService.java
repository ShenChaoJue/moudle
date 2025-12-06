package com.ziwen.moudle.service.file;

import com.ziwen.moudle.dto.file.FileContentInfo;
import com.ziwen.moudle.entity.file.FileEntity;
import com.ziwen.moudle.mapper.file.FileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件内容读取服务
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-06
 */
@Slf4j
@Service
public class FileContentService {

    private final FileMapper fileMapper;

    // 支持的可读文件类型（文本文件）
    private static final List<String> READABLE_TYPES = Arrays.asList(
            "text/plain",
            "text/markdown",
            "text/html",
            "text/xml",
            "application/json",
            "application/xml",
            "application/properties",
            "application/x-yaml",
            "text/x-java-source",
            "text/x-python-source",
            "text/x-c",
            "text/x-c++",
            "application/x-sql"
    );

    // 支持的媒体文件类型（只返回元数据，不读取内容）
    private static final List<String> MEDIA_TYPES = Arrays.asList(
            "video/",
            "audio/",
            "image/"
    );

    // 支持的文本文件扩展名
    private static final List<String> TEXT_EXTENSIONS = Arrays.asList(
            "txt", "md", "markdown", "json", "xml", "html", "properties",
            "yml", "yaml", "java", "py", "c", "cpp", "h", "hpp", "cs",
            "js", "ts", "css", "scss", "sql", "sh", "bat", "log"
    );

    public FileContentService(FileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    /**
     * 根据文件ID读取文件内容
     *
     * @param fileId 文件ID
     * @return 文件内容信息
     */
    public FileContentInfo readFileContent(Long fileId) {
        log.info("读取文件内容，文件ID: {}", fileId);

        // 查询文件信息
        FileEntity fileEntity = fileMapper.selectById(fileId);
        if (fileEntity == null) {
            throw new RuntimeException("文件不存在，ID: " + fileId);
        }

        return readFileContent(fileEntity);
    }

    /**
     * 根据文件实体读取文件内容
     *
     * @param fileEntity 文件实体
     * @return 文件内容信息
     */
    public FileContentInfo readFileContent(FileEntity fileEntity) {
        if (fileEntity == null || !StringUtils.hasText(fileEntity.getFilePath())) {
            throw new IllegalArgumentException("文件信息无效");
        }

        log.info("读取文件内容: {}", fileEntity.getOriginalName());

        try {
            // 检查文件是否可读
            if (!isReadableFile(fileEntity)) {
                throw new RuntimeException("文件类型不支持读取: " + fileEntity.getContentType());
            }

            String content;
            String encoding;
            String summary;

            // 判断是否为媒体文件（视频、音频、图片）
            boolean isMediaFile = MEDIA_TYPES.stream()
                    .anyMatch(type -> fileEntity.getContentType().toLowerCase().startsWith(type));

            if (isMediaFile) {
                // 媒体文件：只返回元数据，不读取二进制内容
                content = String.format("【媒体文件】\n文件名: %s\n文件类型: %s\n文件大小: %.2f MB\n上传时间: %s",
                        fileEntity.getOriginalName(),
                        fileEntity.getContentType(),
                        fileEntity.getFileSize() / 1024.0 / 1024.0,
                        fileEntity.getUploadTime());
                encoding = "N/A";
                summary = fileEntity.getOriginalName();
            } else {
                // 文本文件：读取实际内容
                content = readFileAsText(fileEntity.getFilePath());
                encoding = detectEncoding(fileEntity.getFilePath());
                summary = extractSummary(content);
            }

            return FileContentInfo.builder()
                    .fileId(fileEntity.getId())
                    .fileName(fileEntity.getOriginalName())
                    .filePath(fileEntity.getFilePath())
                    .contentType(fileEntity.getContentType())
                    .content(content)
                    .summary(summary)
                    .encoding(encoding)
                    .contentLength(content.length())
                    .build();

        } catch (Exception e) {
            log.error("读取文件失败: {}", fileEntity.getFilePath(), e);
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 搜索并读取多个文件的内容
     *
     * @param keyword 搜索关键词（支持逗号分隔的多个关键词）
     * @return 文件内容列表
     */
    public List<FileContentInfo> searchAndReadFiles(String keyword) {
        log.info("搜索文件并读取内容，关键词: {}", keyword);

        // 如果关键词包含逗号，拆分成多个关键词
        String[] keywords = keyword.split("[,，]");
        List<FileEntity> allFiles = new ArrayList<>();

        // 对每个关键词进行搜索
        for (String kw : keywords) {
            String trimmedKeyword = kw.trim();
            if (StringUtils.hasText(trimmedKeyword)) {
                List<FileEntity> files = fileMapper.searchByKeyword(trimmedKeyword);
                log.info("关键词 '{}' 搜索到 {} 个文件", trimmedKeyword, files.size());
                allFiles.addAll(files);
            }
        }

        // 去重并返回结果
        return allFiles.stream()
                .distinct()
                .filter(this::isReadableFile)
                .map(this::readFileContent)
                .collect(Collectors.toList());
    }

    /**
     * 读取所有可读文件的内容
     *
     * @return 文件内容列表
     */
    public List<FileContentInfo> readAllReadableFiles() {
        log.info("读取所有可读文件");

        List<FileEntity> files = fileMapper.selectReadableFiles();
        return files.stream()
                .map(this::readFileContent)
                .collect(Collectors.toList());
    }

    /**
     * 检查文件是否可读
     *
     * @param fileEntity 文件实体
     * @return 是否可读（文本文件或媒体文件）
     */
    public boolean isReadableFile(FileEntity fileEntity) {
        if (fileEntity == null || !StringUtils.hasText(fileEntity.getContentType())) {
            return false;
        }

        String contentType = fileEntity.getContentType().toLowerCase();
        String fileName = fileEntity.getOriginalName().toLowerCase();

        // 检查 MIME 类型 - 文本文件
        boolean isReadableType = READABLE_TYPES.stream()
                .anyMatch(type -> contentType.contains(type.toLowerCase()));

        // 检查 MIME 类型 - 媒体文件（视频、音频、图片）
        boolean isMediaType = MEDIA_TYPES.stream()
                .anyMatch(type -> contentType.startsWith(type));

        // 检查文件扩展名
        boolean isReadableExtension = TEXT_EXTENSIONS.stream()
                .anyMatch(ext -> fileName.endsWith("." + ext));

        return isReadableType || isMediaType || isReadableExtension;
    }

    /**
     * 以文本形式读取文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException 读取异常
     */
    private String readFileAsText(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }

        if (!Files.isReadable(path)) {
            throw new IOException("文件不可读: " + filePath);
        }

        // 检测编码
        Charset charset = detectCharset(filePath);

        // 读取文件内容
        List<String> lines = Files.readAllLines(path, charset);
        return String.join("\n", lines);
    }

    /**
     * 检测文件编码
     *
     * @param filePath 文件路径
     * @return 编码名称
     */
    private String detectEncoding(String filePath) {
        Charset charset = detectCharset(filePath);
        return charset.name();
    }

    /**
     * 检测文件编码
     *
     * @param filePath 文件路径
     * @return Charset对象
     */
    private Charset detectCharset(String filePath) {
        try {
            // 简单检测：尝试 UTF-8，如果失败则使用系统默认编码
            Path path = Paths.get(filePath);
            byte[] bytes = Files.readAllBytes(path);

            // 检查 BOM
            if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                return StandardCharsets.UTF_8;
            }

            // 尝试 UTF-8 解码
            new String(bytes, StandardCharsets.UTF_8);
            return StandardCharsets.UTF_8;

        } catch (Exception e) {
            log.warn("编码检测失败，使用默认编码: {}", e.getMessage());
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * 提取文件内容摘要
     *
     * @param content 文件内容
     * @return 摘要（前500字符）
     */
    private String extractSummary(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        int maxLength = 500;
        if (content.length() <= maxLength) {
            return content;
        }

        return content.substring(0, maxLength) + "...";
    }
}