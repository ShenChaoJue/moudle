package com.ziwen.moudle.controller.file;

import com.ziwen.moudle.common.AjaxResult;
import com.ziwen.moudle.entity.file.FileEntity;
import com.ziwen.moudle.mapper.file.FileChunkMapper;
import com.ziwen.moudle.service.file.FileService;
import com.ziwen.moudle.utils.FileUploadUtil;
import com.ziwen.moudle.utils.FileAccessSessionManager;
import com.ziwen.moudle.utils.MimeTypeUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.UUID;

/**
 * 文件管理 REST API
 *
 * @author ziwen
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileUploadUtil fileUploadUtil;
    private final FileAccessSessionManager sessionManager;
    private final com.ziwen.moudle.service.file.FileChunkingService fileChunkingService;

    @Autowired
    private FileChunkMapper chunkMapper;

    /** 允许上传的文件类型 */
    @Value("${file.upload.allowed-types}")
    private String allowedTypes;

    /** 文件大小阈值（字节）- 超过此大小将自动分片 */
    @Value("${file.upload.auto-chunk.threshold:10485760}") // 默认10MB
    private long autoChunkThreshold;

    /** 分片大小（字节）- 默认1MB */
    @Value("${file.upload.chunk.size:1048576}") // 默认1MB
    private long chunkSize;

    /**
     * 自动分片处理
     *
     * @param sourceFile 源文件
     * @param fileId 文件ID
     * @param chunkSize 分片大小
     * @return 分片数量
     * @throws IOException 分片失败
     */
    private int autoChunkFile(File sourceFile, Long fileId, long chunkSize) throws IOException {
        // 生成临时分片目录
        String tempDirPath = fileUploadUtil.getUploadRootPath() + "temp/" + fileId + "/";
        File tempDir = new File(tempDirPath);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        int totalChunks = 0;
        try (FileInputStream fis = new FileInputStream(sourceFile)) {
            byte[] buffer = new byte[(int) chunkSize];
            int bytesRead;
            int chunkNumber = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                // 保存分片文件
                File chunkFile = new File(tempDirPath + chunkNumber);
                try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
                    fos.write(buffer, 0, bytesRead);
                }

                chunkNumber++;
                totalChunks++;
            }
        }

        return totalChunks;
    }

    /**
     * 自动合并分片文件
     *
     * @param fileId 文件ID
     * @param totalChunks 分片总数
     * @return 合并后的临时文件
     * @throws IOException 合并失败
     */
    private File autoMergeChunks(Long fileId, int totalChunks) throws IOException {
        String tempDirPath = fileUploadUtil.getUploadRootPath() + "temp/" + fileId + "/";
        String mergedFileName = "merged_" + fileId;
        File mergedFile = new File(tempDirPath + mergedFileName);

        try (FileOutputStream fos = new FileOutputStream(mergedFile)) {
            for (int i = 0; i < totalChunks; i++) {
                File chunkFile = new File(tempDirPath + i);
                if (!chunkFile.exists()) {
                    throw new IOException("分片文件不存在：" + chunkFile.getAbsolutePath());
                }

                // 读取分片并写入合并文件
                Files.copy(chunkFile.toPath(), fos);
            }
        }

        return mergedFile;
    }

    /**
     * 设置Content-Disposition头部（支持中文文件名）
     *
     * @param response 响应对象
     * @param fileName 原始文件名
     */
    private void setContentDisposition(HttpServletResponse response, String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            fileName = "unknown";
        }

        try {
            // 对中文文件名进行URL编码（解决所有浏览器乱码问题）
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                    // 替换URLEncoder编码后的空格（%20）为+，部分浏览器需要
                    .replace("+", "%20");

            // 构造Content-Disposition头
            // 兼容Chrome/Firefox/Edge（RFC 5987标准）
            String contentDisposition = String.format("attachment; filename=\"%s\"; filename*=UTF-8''%s",
                    encodedFileName, encodedFileName);

            // 设置响应头
            response.setHeader("Content-Disposition", contentDisposition);
        } catch (Exception e) {
            // 如果编码失败，生成一个安全的文件名
            String safeFileName = "document";
            response.setHeader("Content-Disposition",
                "attachment; filename=\"" + safeFileName + "\"");
        }
    }

    /**
     * 重新向量化文件
     */
    @PostMapping("/reindex/{fileId}")
    public AjaxResult reindexFile(@PathVariable Long fileId) {
        try {
            FileEntity file = fileService.getFile(fileId);
            if (file == null) {
                return AjaxResult.error("文件不存在");
            }
            // 删除旧的片段和向量
            chunkMapper.deleteByFileId(fileId);
            // 重新处理
            fileChunkingService.processFile(file);
            return AjaxResult.success("重新向量化成功");
        } catch (Exception e) {
            log.error("重新向量化失败", e);
            return AjaxResult.error("重新向量化失败: " + e.getMessage());
        }
    }

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public AjaxResult uploadFile(@RequestParam("file") MultipartFile file) {
        // 1. 基础校验
        if (file.isEmpty()) {
            return AjaxResult.warn("上传文件不能为空");
        }

        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        // 2. 校验文件类型
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
        if (!allowedTypes.contains(fileExtension)) {
            return AjaxResult.warn("不支持的文件类型，允许类型：" + allowedTypes);
        }

        File destFile = null; // 用于记录已创建的文件
        boolean fileCreated = false; // 标记文件是否创建成功
        Long fileId = null; // 用于记录已保存的文件ID

        try {
            // 3. 创建存储目录（按日期分目录）
            String dateDir = fileUploadUtil.createStorageDir();
            // 4. 生成安全的原始文件名（避免覆盖）
            String finalFileName = fileUploadUtil.generateSafeFileName(originalFileName);
            // 5. 构建文件绝对路径
            String absolutePath = fileUploadUtil.getAbsolutePath(dateDir, finalFileName);
            // 6. 构建访问路径
            String accessPath = fileUploadUtil.buildAccessPath(
                    dateDir.replace(fileUploadUtil.getUploadRootPath(), ""),
                    finalFileName
            );

            // 7. 将文件写入服务器目录
            destFile = new File(absolutePath);
            file.transferTo(destFile); // 核心：写入文件到服务器

            fileCreated = true; // 标记文件创建成功

            // 8. 保存文件元信息到数据库
            FileEntity fileEntity = new FileEntity();
            fileEntity.setOriginalName(originalFileName); // 存储原始文件名

            // 自动识别Content-Type：优先使用上传时浏览器提供的MIME类型，如果不可靠则根据文件扩展名识别
            String contentType = file.getContentType();
            if (contentType == null || contentType.isEmpty() ||
                "application/octet-stream".equals(contentType) ||
                contentType.contains("application/")) {
                // 如果浏览器提供的类型不可靠（如application/octet-stream），则根据扩展名识别
                contentType = MimeTypeUtils.getMimeType(originalFileName);
            }
            fileEntity.setContentType(contentType);

            fileEntity.setFileSize(file.getSize());
            fileEntity.setFilePath(absolutePath); // 存绝对路径
            fileEntity.setAccessPath(accessPath); // 存访问路径
            fileEntity.initUploadTime(); // 设置上传时间

            fileId = fileService.saveFile(fileEntity);
            fileEntity.setId(fileId);

            // RAG处理：异步处理文件切片和向量化
            try {
                fileChunkingService.processFile(fileEntity);
            } catch (Exception e) {
                log.error("文件RAG处理失败: {}", fileEntity.getOriginalName(), e);
                // RAG处理失败时，删除已保存的文件和数据库记录
                if (destFile != null && destFile.exists()) {
                    destFile.delete();
                }
                if (fileId != null) {
                    fileService.deleteFile(fileId);
                }
                return AjaxResult.error("文件处理失败：" + e.getMessage());
            }

            // 自动分片处理：如果文件大小超过阈值，自动分片
            if (fileEntity.getFileSize() > autoChunkThreshold) {
                try {
                    // 自动分片
                    int totalChunks = autoChunkFile(destFile, fileId, chunkSize);

                    // 更新文件信息为分片模式
                    fileEntity.setIsChunked(true);
                    fileEntity.setTotalChunks(totalChunks);
                    fileEntity.setChunkSize(chunkSize);
                    fileEntity.setUploadId(String.valueOf(fileId));
                    fileService.updateFile(fileEntity);

                    // 可以删除原始文件节省空间（保留分片文件即可）
                    // destFile.delete();

                    return AjaxResult.success("文件上传成功（自动分片：" + totalChunks + "片）", fileEntity);
                } catch (IOException e) {
                    // 分片失败时也要清理资源
                    log.error("文件分片处理失败: {}", fileEntity.getOriginalName(), e);
                    if (destFile != null && destFile.exists()) {
                        destFile.delete();
                    }
                    if (fileId != null) {
                        fileService.deleteFile(fileId);
                    }
                    return AjaxResult.error("文件分片处理失败：" + e.getMessage());
                }
            }

            // 返回完整的文件信息，包括文件类型，便于前端识别
            return AjaxResult.success("文件上传成功", fileEntity);

        } catch (Exception e) {
            e.printStackTrace();
            // 事务回滚：删除已创建的文件和数据库记录
            if (fileCreated && destFile != null && destFile.exists()) {
                boolean deleted = destFile.delete();
                if (!deleted) {
                    System.err.println("警告：数据库保存失败，且无法删除已创建的文件：" + destFile.getAbsolutePath());
                }
            }
            // 如果文件记录已保存到数据库，则删除它
            if (fileId != null) {
                try {
                    fileService.deleteFile(fileId);
                } catch (Exception deleteEx) {
                    System.err.println("警告：无法删除已保存的文件记录，ID：" + fileId);
                    deleteEx.printStackTrace();
                }
            }
            return AjaxResult.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 文件下载
     */
    @GetMapping("/download/{id}")
    public void downloadFile(@PathVariable Long id, HttpServletResponse response) throws IOException {
        // 1. 查询文件信息
        FileEntity fileEntity = fileService.getFile(id);
        if (fileEntity == null) {
            throw new RuntimeException("文件不存在，ID：" + id);
        }

        File file = new File(fileEntity.getFilePath());
        if (!file.exists()) {
            throw new RuntimeException("文件已被删除，路径：" + fileEntity.getFilePath());
        }

        // 2. 处理文件（分片文件需要先合并）
        File downloadFile = file;
        if (Boolean.TRUE.equals(fileEntity.getIsChunked())) {
            try {
                // 自动合并分片文件
                downloadFile = autoMergeChunks(fileEntity.getId(), fileEntity.getTotalChunks());
            } catch (IOException e) {
                throw new RuntimeException("分片合并失败：" + e.getMessage(), e);
            }
        }

        // 3. 设置响应头
        // 设置Content-Type（不要添加charset=UTF-8，让浏览器根据文件类型自动处理）
        response.setContentType(fileEntity.getContentType());

        // 设置Content-Disposition头部（支持中文文件名）
        setContentDisposition(response, fileEntity.getOriginalName());

        response.setContentLengthLong(fileEntity.getFileSize());

        // 4. 读取文件并写入响应流
        try (FileInputStream fis = new FileInputStream(downloadFile);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[1024 * 8]; // 8KB缓冲区
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } finally {
            // 清理合并后的临时文件（可选）
            if (downloadFile != file && downloadFile.exists() && downloadFile.getName().startsWith("merged_")) {
                downloadFile.delete();
            }
        }
    }

    /**
     * 直接访问文件（需要访问令牌）
     * URL格式: /api/files/access/{id}?token={访问令牌}
     */
    @GetMapping("/access/{id}")
    public void accessFile(@PathVariable Long id,
                          @RequestParam String token,
                          HttpServletResponse response) throws IOException {
        // 验证访问令牌
        FileAccessSessionManager.FileAccessSession session = sessionManager.validateToken(token);
        if (session == null) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            AjaxResult errorResult = AjaxResult.error("访问令牌无效或已过期");
            response.getWriter().write(com.alibaba.fastjson.JSON.toJSONString(errorResult));
            return;
        }

        // 验证文件ID是否匹配
        if (!session.getFileId().equals(id)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            AjaxResult errorResult = AjaxResult.error("令牌与文件ID不匹配");
            response.getWriter().write(com.alibaba.fastjson.JSON.toJSONString(errorResult));
            return;
        }

        FileEntity fileEntity = fileService.getFile(id);
        if (fileEntity == null) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            AjaxResult errorResult = AjaxResult.error("文件不存在，ID：" + id);
            response.getWriter().write(com.alibaba.fastjson.JSON.toJSONString(errorResult));
            return;
        }

        File file = new File(fileEntity.getFilePath());
        if (!file.exists()) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            AjaxResult errorResult = AjaxResult.error("文件已被删除，路径：" + fileEntity.getFilePath());
            response.getWriter().write(com.alibaba.fastjson.JSON.toJSONString(errorResult));
            return;
        }

        // 设置响应头（支持在线播放和预览）
        response.setContentType(fileEntity.getContentType());
        response.setHeader("Content-Length", String.valueOf(fileEntity.getFileSize()));
        response.setHeader("Accept-Ranges", "bytes");

        // 流式输出文件（在输出过程中持续检查令牌有效性）
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[1024 * 16];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                // 在每次写入前检查令牌是否仍然有效
                session = sessionManager.validateToken(token);
                if (session == null) {
                    // 令牌被撤销，强制停止输出
                    try {
                        response.getOutputStream().close();
                    } catch (IOException ignored) {}
                    return;
                }
                os.write(buffer, 0, len);
            }
            os.flush();

            // 文件传输完成后自动撤销令牌（可选）
            // sessionManager.revokeToken(token);
        }
    }

    /**
     * 生成文件访问令牌
     * 用途：前端获取播放URL之前，先请求此接口获取访问令牌
     */
    @PostMapping("/token/{id}")
    public AjaxResult generateAccessToken(@PathVariable Long id,
                                        @RequestParam(defaultValue = "60") int expiresInMinutes) {
        // 检查文件是否存在
        FileEntity fileEntity = fileService.getFile(id);
        if (fileEntity == null) {
            return AjaxResult.error("文件不存在，ID：" + id);
        }

        // TODO: 根据实际权限系统验证用户是否有权访问此文件
        // String userId = getCurrentUserId();

        // 生成访问令牌（无需登录的版本）
        String token = sessionManager.createAccessToken(id, null, expiresInMinutes);

        // 构建访问URL
        String accessUrl = "/api/files/access/" + id + "?token=" + token;

        return AjaxResult.success("令牌生成成功", new AccessTokenInfo(
            token,
            accessUrl,
            expiresInMinutes,
            "令牌将在" + expiresInMinutes + "分钟后过期"
        ));
    }

    /**
     * 撤销文件访问令牌（强制停止播放）
     * 用途：随时撤销正在进行的播放权限
     */
    @DeleteMapping("/token/{id}")
    public AjaxResult revokeAccessToken(@PathVariable Long id,
                                       @RequestParam String token) {
        boolean success = sessionManager.revokeToken(token);
        if (success) {
            return AjaxResult.success("令牌已撤销，播放已停止");
        } else {
            return AjaxResult.error("令牌撤销失败：令牌不存在或已过期");
        }
    }

    /**
     * 撤销文件的所有访问令牌（管理员功能）
     * 用途：强制停止某个文件的所有播放
     */
    @DeleteMapping("/tokens/{id}")
    public AjaxResult revokeAllFileTokens(@PathVariable Long id) {
        int count = sessionManager.revokeFileTokens(id);
        return AjaxResult.success("已撤销" + count + "个访问令牌");
    }

    /**
     * 查询文件列表
     */
    @GetMapping("/list")
    public AjaxResult listFiles() {
        return AjaxResult.success(fileService.listFiles());
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete/{id}")
    public AjaxResult deleteFile(@PathVariable Long id) {
        FileEntity fileEntity = fileService.getFile(id);
        if (fileEntity == null) {
            return AjaxResult.warn("文件不存在，ID：" + id);
        }

        try {
            // 1. 删除服务器文件
            File file = new File(fileEntity.getFilePath());
            if (file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    return AjaxResult.error("服务器文件删除失败");
                }
            }

            // 2. 删除数据库记录（软删除）
            fileService.deleteFile(id);
            return AjaxResult.success("文件删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error("文件删除失败：" + e.getMessage());
        }
    }

    /**
     * 初始化分片上传
     */
    @PostMapping("/chunk/init")
    public AjaxResult initChunkUpload(
            @RequestParam("fileName") String fileName,
            @RequestParam("totalChunks") Integer totalChunks,
            @RequestParam("chunkSize") Long chunkSize,
            @RequestParam("totalSize") Long totalSize) {

        try {
            // 1. 校验文件类型
            String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            if (!allowedTypes.contains(fileExtension)) {
                return AjaxResult.warn("不支持的文件类型，允许类型：" + allowedTypes);
            }

            // 2. 生成上传会话 ID
            String uploadId = UUID.randomUUID().toString();

            // 3. 创建临时存储目录（按 uploadId 分目录）
            String tempDirPath = fileUploadUtil.getUploadRootPath() + "temp/" + uploadId + "/";
            File tempDir = new File(tempDirPath);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            return AjaxResult.success(new ChunkUploadResponse(uploadId, tempDirPath));

        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error("初始化分片上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传分片
     */
    @PostMapping("/chunk/{uploadId}/{chunkNumber}")
    public AjaxResult uploadChunk(
            @PathVariable String uploadId,
            @PathVariable Integer chunkNumber,
            @RequestParam("file") MultipartFile chunk) {

        try {
            String tempDirPath = fileUploadUtil.getUploadRootPath() + "temp/" + uploadId + "/";
            File chunkFile = new File(tempDirPath + chunkNumber);

            // 保存分片文件
            chunk.transferTo(chunkFile);

            return AjaxResult.success("分片 " + chunkNumber + " 上传成功");

        } catch (IOException e) {
            e.printStackTrace();
            return AjaxResult.error("分片上传失败：" + e.getMessage());
        }
    }

    /**
     * 合并分片
     */
    @PostMapping("/chunk/merge/{uploadId}")
    public AjaxResult mergeChunks(
            @PathVariable String uploadId,
            @RequestParam("fileName") String fileName,
            @RequestParam("totalChunks") Integer totalChunks,
            @RequestParam("contentType") String contentType,
            @RequestParam("totalSize") Long totalSize) {

        String tempDirPath = fileUploadUtil.getUploadRootPath() + "temp/" + uploadId + "/";
        File destFile = null; // 用于记录已创建的文件
        boolean fileCreated = false; // 标记文件是否已创建

        try {
            String dateDir = fileUploadUtil.createStorageDir();
            // 使用原始文件名（安全的）
            String finalFileName = fileUploadUtil.generateSafeFileName(fileName);
            String absolutePath = fileUploadUtil.getAbsolutePath(dateDir, finalFileName);
            String accessPath = fileUploadUtil.buildAccessPath(
                    dateDir.replace(fileUploadUtil.getUploadRootPath(), ""),
                    finalFileName
            );

            // 1. 创建目标文件
            destFile = new File(absolutePath);

            // 2. 合并分片（按顺序写入）
            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                for (int i = 0; i < totalChunks; i++) {
                    File chunkFile = new File(tempDirPath + i);
                    if (!chunkFile.exists()) {
                        return AjaxResult.warn("分片 " + i + " 不存在");
                    }

                    // 写入分片内容
                    Files.copy(chunkFile.toPath(), fos);
                }
            }

            fileCreated = true; // 标记文件创建成功

            // 3. 保存文件信息到数据库（关键：这必须在删除临时文件之前）
            FileEntity fileEntity = new FileEntity();
            fileEntity.setOriginalName(fileName); // 存储原始文件名

            // 自动识别Content-Type：优先使用上传时提供的类型，如果不可靠则根据文件扩展名识别
            String resolvedContentType = contentType;
            if (resolvedContentType == null || resolvedContentType.isEmpty() ||
                "application/octet-stream".equals(resolvedContentType) ||
                resolvedContentType.contains("application/")) {
                resolvedContentType = MimeTypeUtils.getMimeType(fileName);
            }
            fileEntity.setContentType(resolvedContentType);

            fileEntity.setFileSize(totalSize);
            fileEntity.setFilePath(absolutePath);
            fileEntity.setAccessPath(accessPath);
            fileEntity.setIsChunked(true);
            fileEntity.setTotalChunks(totalChunks);
            fileEntity.setChunkSize(totalSize / totalChunks);
            fileEntity.setUploadId(uploadId);
            fileEntity.initUploadTime(); // 设置上传时间

            Long fileId = fileService.saveFile(fileEntity);
            fileEntity.setId(fileId);

            // 4. 数据库保存成功后，删除临时分片文件
            for (int i = 0; i < totalChunks; i++) {
                File chunkFile = new File(tempDirPath + i);
                if (chunkFile.exists()) {
                    chunkFile.delete();
                }
            }
            // 删除临时目录
            File tempDir = new File(tempDirPath);
            if (tempDir.exists()) {
                tempDir.delete();
            }

            // 返回完整的文件信息，包括文件类型，便于前端识别
            return AjaxResult.success("文件合并成功", fileEntity);

        } catch (Exception e) {
            e.printStackTrace();
            // 事务回滚：清理所有文件
            if (fileCreated && destFile != null && destFile.exists()) {
                boolean deleted = destFile.delete();
                if (!deleted) {
                    System.err.println("警告：数据库保存失败，且无法删除已创建的文件：" + destFile.getAbsolutePath());
                }
            }
            // 注意：不需要删除临时分片文件，因为它们是用户上传的原始数据
            // 临时目录会在下次上传或定期清理任务中处理
            return AjaxResult.error("分片合并失败：" + e.getMessage());
        }
    }

    /**
     * 分片上传响应对象
     */
    public static class ChunkUploadResponse {
        public String uploadId;
        public String tempDirPath;

        public ChunkUploadResponse(String uploadId, String tempDirPath) {
            this.uploadId = uploadId;
            this.tempDirPath = tempDirPath;
        }
    }

    /**
     * 访问令牌信息
     */
    public static class AccessTokenInfo {
        public String token;
        public String accessUrl;
        public int expiresInMinutes;
        public String message;

        public AccessTokenInfo(String token, String accessUrl, int expiresInMinutes, String message) {
            this.token = token;
            this.accessUrl = accessUrl;
            this.expiresInMinutes = expiresInMinutes;
            this.message = message;
        }
    }

    /**
     * 定时清理过期令牌
     * 每10分钟清理一次过期无效的访问令牌
     */
    @Scheduled(fixedRate = 600000) // 10分钟
    public void cleanupExpiredTokens() {
        int beforeCount = sessionManager.getActiveTokenCount();
        sessionManager.cleanupExpiredTokens();
        int afterCount = sessionManager.getActiveTokenCount();

        if (beforeCount != afterCount) {
            System.out.println("文件访问令牌清理完成：清理了 " + (beforeCount - afterCount) + " 个过期令牌");
        }
    }
}