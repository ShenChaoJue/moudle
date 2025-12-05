package com.ziwen.moudle.dto.rbac;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色 DTO（与sys_role表字段完全一致）
 *
 * @author boot
 */
@Data
public class RoleDTO {

    /**
     * 角色ID（雪花ID）
     */
    private Long id;

    /**
     * 角色名称
     */
    private String roleName;

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
     * 菜单ID列表（用于菜单分配）
     */
    private List<Long> menuIds;

    /**
     * 菜单名称列表（用于显示）
     */
    private List<String> menuNames;

    /**
     * 用户ID列表（用于显示）
     */
    private List<Long> userIds;

    /**
     * 用户名称列表（用于显示）
     */
    private List<String> userNames;
}
