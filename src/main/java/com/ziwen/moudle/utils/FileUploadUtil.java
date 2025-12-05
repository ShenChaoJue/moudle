package com.ziwen.moudle.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class FileUploadUtil {

    /** 服务器文件存储根目录 */
    @Value("${file.upload.path}")
    private String uploadRootPath;

    /** 文件访问虚拟路径 */
    @Value("${file.upload.access-path}")
    private String accessPath;

    /**
     * 创建存储目录（按日期分目录，避免单目录文件过多）
     */
    public String createStorageDir() {
        // 按yyyy/MM/dd创建子目录
        LocalDateTime now = LocalDateTime.now();
        String dateDir = String.format("%d/%02d/%02d",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        String fullDir = uploadRootPath + dateDir;

        File dir = new File(fullDir);
        if (!dir.exists()) {
            dir.mkdirs(); // 递归创建目录
        }
        return fullDir;
    }

    /**
     * 生成唯一文件名（避免覆盖）
     */
    public String generateUniqueFileName(String originalFileName) {
        // 提取扩展名
        String extension = StringUtils.cleanPath(originalFileName)
                .substring(originalFileName.lastIndexOf("."));
        // 用UUID生成唯一文件名 + 扩展名
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * 生成安全的原始文件名（避免覆盖，同名文件自动重命名）
     */
    public String generateSafeFileName(String originalFileName) {
        // 提取文件名和扩展名
        String cleanFileName = StringUtils.cleanPath(originalFileName);

        // 如果文件名包含路径，取最后一部分
        if (cleanFileName.contains("/") || cleanFileName.contains("\\")) {
            cleanFileName = cleanFileName.substring(cleanFileName.lastIndexOf("/") + 1);
            cleanFileName = cleanFileName.substring(cleanFileName.lastIndexOf("\\") + 1);
        }

        int dotIndex = cleanFileName.lastIndexOf(".");
        String name = dotIndex > 0 ? cleanFileName.substring(0, dotIndex) : cleanFileName;
        String extension = dotIndex > 0 ? cleanFileName.substring(dotIndex) : "";

        // 检查文件是否存在，如果存在则添加时间戳
        String finalFileName = cleanFileName;
        String dateDir = createStorageDir();
        File file = new File(dateDir + File.separator + cleanFileName);

        int counter = 1;
        while (file.exists()) {
            finalFileName = name + "_" + System.currentTimeMillis() + extension;
            file = new File(dateDir + File.separator + finalFileName);
            counter++;
            // 防止无限循环
            if (counter > 100) {
                break;
            }
        }

        return finalFileName;
    }

    /**
     * 构建文件访问路径
     */
    public String buildAccessPath(String dateDir, String uniqueFileName) {
        return accessPath + dateDir + "/" + uniqueFileName;
    }

    /**
     * 获取文件绝对路径
     */
    public String getAbsolutePath(String dateDir, String uniqueFileName) {
        return dateDir + File.separator + uniqueFileName;
    }

    /**
     * 获取上传根路径
     */
    public String getUploadRootPath() {
        return uploadRootPath;
    }
}