package com.ziwen.moudle.entity.file;

import com.ziwen.moudle.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件片段实体 - RAG 文档切片
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileChunkEntity extends BaseEntity<FileChunkEntity> {
    
    /**
     * 所属文件ID
     */
    private Long fileId;
    
    /**
     * 片段索引（从0开始）
     */
    private Integer chunkIndex;
    
    /**
     * 片段文本内容
     */
    private String chunkText;
    
    /**
     * 起始位置（字符索引）
     */
    private Integer startPos;
    
    /**
     * 结束位置（字符索引）
     */
    private Integer endPos;
}