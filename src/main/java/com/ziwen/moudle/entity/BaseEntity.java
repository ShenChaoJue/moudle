package com.ziwen.moudle.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通用基础实体类
 * 包含所有实体类的通用字段
 *
 * @param <T> 实体类型
 * @author boot
 */
@Data
public abstract class BaseEntity<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否删除：0-未删除，1-删除
     */
    private Integer isDeleted;
}
