package com.ziwen.moudle.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * MIME类型工具类
 * 根据文件扩展名自动识别Content-Type
 *
 * @author ziwen
 */
public class MimeTypeUtils {

    /**
     * 文件扩展名到MIME类型的映射表
     */
    private static final Map<String, String> MIME_TYPE_MAP = new HashMap<>();

    static {
        // 文档类型
        MIME_TYPE_MAP.put("doc", "application/msword");
        MIME_TYPE_MAP.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIME_TYPE_MAP.put("xls", "application/vnd.ms-excel");
        MIME_TYPE_MAP.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        MIME_TYPE_MAP.put("ppt", "application/vnd.ms-powerpoint");
        MIME_TYPE_MAP.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        MIME_TYPE_MAP.put("pdf", "application/pdf");
        MIME_TYPE_MAP.put("txt", "text/plain");
        MIME_TYPE_MAP.put("rtf", "application/msword");

        // 图片类型
        MIME_TYPE_MAP.put("jpg", "image/jpeg");
        MIME_TYPE_MAP.put("jpeg", "image/jpeg");
        MIME_TYPE_MAP.put("png", "image/png");
        MIME_TYPE_MAP.put("gif", "image/gif");
        MIME_TYPE_MAP.put("bmp", "image/bmp");
        MIME_TYPE_MAP.put("tif", "image/tiff");
        MIME_TYPE_MAP.put("tiff", "image/tiff");
        MIME_TYPE_MAP.put("ico", "image/x-icon");
        MIME_TYPE_MAP.put("svg", "image/svg+xml");
        MIME_TYPE_MAP.put("webp", "image/webp");

        // 音频类型
        MIME_TYPE_MAP.put("mp3", "audio/mpeg");
        MIME_TYPE_MAP.put("wav", "audio/wav");
        MIME_TYPE_MAP.put("flac", "audio/flac");
        MIME_TYPE_MAP.put("aac", "audio/aac");
        MIME_TYPE_MAP.put("ogg", "audio/ogg");
        MIME_TYPE_MAP.put("wma", "audio/x-ms-wma");

        // 视频类型
        MIME_TYPE_MAP.put("mp4", "video/mp4");
        MIME_TYPE_MAP.put("avi", "video/x-msvideo");
        MIME_TYPE_MAP.put("mov", "video/quicktime");
        MIME_TYPE_MAP.put("wmv", "video/x-ms-wmv");
        MIME_TYPE_MAP.put("flv", "video/x-flv");
        MIME_TYPE_MAP.put("3gp", "video/3gpp");
        MIME_TYPE_MAP.put("webm", "video/webm");
        MIME_TYPE_MAP.put("mkv", "video/x-matroska");

        // 压缩文件类型
        MIME_TYPE_MAP.put("zip", "application/zip");
        MIME_TYPE_MAP.put("rar", "application/x-rar-compressed");
        MIME_TYPE_MAP.put("7z", "application/x-7z-compressed");
        MIME_TYPE_MAP.put("tar", "application/x-tar");
        MIME_TYPE_MAP.put("gz", "application/gzip");

        // 代码/配置文件类型
        MIME_TYPE_MAP.put("json", "application/json");
        MIME_TYPE_MAP.put("xml", "text/xml");
        MIME_TYPE_MAP.put("html", "text/html");
        MIME_TYPE_MAP.put("htm", "text/html");
        MIME_TYPE_MAP.put("css", "text/css");
        MIME_TYPE_MAP.put("js", "application/javascript");
        MIME_TYPE_MAP.put("ts", "text/typescript");
        MIME_TYPE_MAP.put("sql", "text/plain");
        MIME_TYPE_MAP.put("properties", "text/plain");
        MIME_TYPE_MAP.put("yml", "text/yaml");
        MIME_TYPE_MAP.put("yaml", "text/yaml");

        // 可执行文件类型
        MIME_TYPE_MAP.put("exe", "application/x-msdownload");
        MIME_TYPE_MAP.put("msi", "application/x-msdownload");
        MIME_TYPE_MAP.put("apk", "application/vnd.android.package-archive");
        MIME_TYPE_MAP.put("ipa", "application/vnd.iphone");
        MIME_TYPE_MAP.put("dmg", "application/x-apple-diskimage");

        // 字体文件类型
        MIME_TYPE_MAP.put("ttf", "font/ttf");
        MIME_TYPE_MAP.put("otf", "font/otf");
        MIME_TYPE_MAP.put("woff", "font/woff");
        MIME_TYPE_MAP.put("woff2", "font/woff2");
        MIME_TYPE_MAP.put("eot", "application/vnd.ms-fontobject");

        // 其他类型
        MIME_TYPE_MAP.put("swf", "application/x-shockwave-flash");
        MIME_TYPE_MAP.put("eps", "application/postscript");
        MIME_TYPE_MAP.put("ai", "application/postscript");
        MIME_TYPE_MAP.put("psd", "image/vnd.adobe.photoshop");
        MIME_TYPE_MAP.put("sketch", "application/sketch");
        MIME_TYPE_MAP.put("dwg", "application/acad");
        MIME_TYPE_MAP.put("dxf", "application/dxf");

        // 默认类型
        MIME_TYPE_MAP.put("default", "application/octet-stream");
    }

    /**
     * 根据文件扩展名获取MIME类型
     *
     * @param fileName 文件名
     * @return MIME类型，如果找不到则返回application/octet-stream
     */
    public static String getMimeType(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return MIME_TYPE_MAP.get("default");
        }

        // 获取文件扩展名
        String extension = getFileExtension(fileName);
        if (extension == null || extension.isEmpty()) {
            return MIME_TYPE_MAP.get("default");
        }

        // 转换为小写查找
        String mimeType = MIME_TYPE_MAP.get(extension.toLowerCase());
        return mimeType != null ? mimeType : MIME_TYPE_MAP.get("default");
    }

    /**
     * 根据文件扩展名获取MIME类型（不区分大小写）
     *
     * @param fileName 文件名
     * @param defaultType 默认MIME类型
     * @return MIME类型，如果找不到则返回defaultType
     */
    public static String getMimeType(String fileName, String defaultType) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return defaultType;
        }

        String extension = getFileExtension(fileName);
        if (extension == null || extension.isEmpty()) {
            return defaultType;
        }

        String mimeType = MIME_TYPE_MAP.get(extension.toLowerCase());
        return mimeType != null ? mimeType : defaultType;
    }

    /**
     * 获取文件扩展名（小写）
     *
     * @param fileName 文件名
     * @return 文件扩展名（不包含点号），如果文件名没有扩展名则返回null
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return null;
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }

        return null;
    }

    /**
     * 检查是否为图片文件
     *
     * @param fileName 文件名
     * @return true表示是图片文件
     */
    public static boolean isImage(String fileName) {
        String mimeType = getMimeType(fileName);
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * 检查是否为视频文件
     *
     * @param fileName 文件名
     * @return true表示是视频文件
     */
    public static boolean isVideo(String fileName) {
        String mimeType = getMimeType(fileName);
        return mimeType != null && mimeType.startsWith("video/");
    }

    /**
     * 检查是否为音频文件
     *
     * @param fileName 文件名
     * @return true表示是音频文件
     */
    public static boolean isAudio(String fileName) {
        String mimeType = getMimeType(fileName);
        return mimeType != null && mimeType.startsWith("audio/");
    }

    /**
     * 检查是否为文档文件
     *
     * @param fileName 文件名
     * @return true表示是文档文件
     */
    public static boolean isDocument(String fileName) {
        String mimeType = getMimeType(fileName);
        return mimeType != null && (
            mimeType.startsWith("application/pdf") ||
            mimeType.startsWith("application/msword") ||
            mimeType.startsWith("application/vnd.openxmlformats-officedocument") ||
            mimeType.startsWith("text/plain")
        );
    }
}
