package com.ziwen.moudle.service.impl.file;

import com.ziwen.moudle.entity.file.FileEntity;
import com.ziwen.moudle.mapper.file.FileMapper;
import com.ziwen.moudle.service.file.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文件服务实现
 *
 * @author system
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private final FileMapper fileMapper;

    public FileServiceImpl(FileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    @Override
    public List<FileEntity> listFiles() {
        return fileMapper.selectFileList();
    }

    @Override
    public FileEntity getFile(Long id) {
        if (id == null) {
            log.warn("文件ID不能为空");
            return null;
        }
        return fileMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveFile(FileEntity file) {
        if (file == null) {
            log.warn("文件信息不能为空");
            throw new IllegalArgumentException("文件信息不能为空");
        }

        // 设置默认字段
        if (file.getIsDeleted() == null) {
            file.setIsDeleted(0);
        }

        fileMapper.insert(file);
        log.info("文件保存成功: {}", file.getId());
        return file.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFile(FileEntity file) {
        if (file == null || file.getId() == null) {
            log.warn("文件信息或ID不能为空");
            throw new IllegalArgumentException("文件信息或ID不能为空");
        }

        // 校验文件是否存在
        FileEntity existFile = fileMapper.selectById(file.getId());
        if (existFile == null) {
            log.warn("文件不存在: {}", file.getId());
            throw new IllegalArgumentException("文件不存在");
        }

        fileMapper.updateById(file);
        log.info("文件更新成功: {}", file.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long id) {
        if (id == null) {
            log.warn("文件ID不能为空");
            throw new IllegalArgumentException("文件ID不能为空");
        }

        // 校验文件是否存在
        FileEntity existFile = fileMapper.selectById(id);
        if (existFile == null) {
            log.warn("文件不存在: {}", id);
            throw new IllegalArgumentException("文件不存在");
        }

        // 软删除
        FileEntity updateFile = new FileEntity();
        updateFile.setId(id);
        updateFile.setIsDeleted(1);
        fileMapper.updateById(updateFile);

        log.info("文件删除成功: {}", id);
    }
}
