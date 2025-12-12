package com.ziwen.moudle.mapper.file;

import com.ziwen.moudle.entity.file.FileChunkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件片段Mapper
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-07
 */
@Mapper
public interface FileChunkMapper {

    /**
     * 插入片段
     */
    void insert(FileChunkEntity chunk);

    /**
     * 根据ID查询
     */
    FileChunkEntity selectById(@Param("id") Long id);

    /**
     * 根据ID列表批量查询
     */
    List<FileChunkEntity> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 根据文件ID查询所有片段
     */
    List<FileChunkEntity> selectByFileId(@Param("fileId") Long fileId);

    /**
     * 删除文件的所有片段
     */
    void deleteByFileId(@Param("fileId") Long fileId);
}

