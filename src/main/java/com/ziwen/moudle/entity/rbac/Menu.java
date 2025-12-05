package com.ziwen.moudle.entity.rbac;

import com.ziwen.moudle.entity.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单实体类（对应sys_menu表）
 *
 * @author boot
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Menu extends BaseEntity<Menu> {

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 资源路径
     */
    private String url;

    /**
     * 路径（用于鉴权）
     */
    private String path;

    /**
     * 类型：文件夹/页面/按钮
     */
    private String type;

    /**
     * 父ID
     */
    private Long parentId;

    /**
     * 子ID
     */
    private Long childId;

    /**
     * 排序
     */
    private Integer sort;
}
