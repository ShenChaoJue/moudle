package com.ziwen.moudle.entity.file;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

import com.ziwen.moudle.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = false)
public class FileEntity extends BaseEntity<FileEntity> {
    /** 原始文件名 */
    private String originalName;

    /** 文件类型（MIME类型） */
    private String contentType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 服务器存储路径（绝对路径） */
    private String filePath;

    /** 访问文件的相对路径/URL */
    private String accessPath;

    /** 上传时间 */
    private LocalDateTime uploadTime;

    /** 是否为分片上传 */
    private Boolean isChunked;

    /** 分片总数 */
    private Integer totalChunks;

    /** 分片大小（字节） */
    private Long chunkSize;

    /** 上传会话ID（分片上传使用） */
    private String uploadId;

    // 初始化上传时间（MyBatis 手动设置）
    public void initUploadTime() {
        this.uploadTime = LocalDateTime.now();
    }

}