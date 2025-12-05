package com.ziwen.moudle.dto.rbac;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单 DTO（与sys_menu表字段完全一致）
 *
 * @author boot
 */
@Data
public class MenuDTO {

    /**
     * 菜单ID（雪花ID）
     */
    private Long id;

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

    /**
     * 是否删除：0-未删除，1-删除
     */
    private Integer isDeleted;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 子菜单列表（用于树形结构）
     */
    private List<MenuDTO> children;

    /**
     * 角色ID列表（用于角色菜单关联）
     */
    private List<Long> roleIds;
}
