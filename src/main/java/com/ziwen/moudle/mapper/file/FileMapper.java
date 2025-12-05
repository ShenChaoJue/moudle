package com.ziwen.moudle.mapper.file;

import com.ziwen.moudle.entity.file.FileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件Mapper接口
 *
 * @author system
 */
@Mapper
public interface FileMapper {

    /**
     * 查询文件列表
     *
     * @return 文件列表
     */
    List<FileEntity> selectFileList();

    /**
     * 根据ID查询文件
     *
     * @param id 文件ID
     * @return 文件信息
     */
    FileEntity selectById(@Param("id") Long id);

    /**
     * 插入文件
     *
     * @param file 文件信息
     */
    void insert(FileEntity file);

    /**
     * 根据ID更新文件
     *
     * @param file 文件信息
     */
    void updateById(FileEntity file);

    /**
     * 根据ID删除文件（软删除）
     *
     * @param id 文件ID
     */
    void deleteById(@Param("id") Long id);
}
