package com.ziwen.moudle.service.file;

import com.ziwen.moudle.entity.file.FileEntity;

import java.util.List;

/**
 * 文件服务接口
 *
 * @author system
 */
public interface FileService {

    /**
     * 查询文件列表
     *
     * @return 文件列表
     */
    List<FileEntity> listFiles();

    /**
     * 根据ID查询文件
     *
     * @param id 文件ID
     * @return 文件信息
     */
    FileEntity getFile(Long id);

    /**
     * 保存文件
     *
     * @param file 文件信息
     * @return 文件ID
     */
    Long saveFile(FileEntity file);

    /**
     * 更新文件
     *
     * @param file 文件信息
     */
    void updateFile(FileEntity file);

    /**
     * 删除文件
     *
     * @param id 文件ID
     */
    void deleteFile(Long id);
}
