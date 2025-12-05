package com.ziwen.moudle.entity.rbac;

import com.ziwen.moudle.entity.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色菜单关联实体类（对应sys_role_menu表）
 *
 * @author boot
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleMenu extends BaseEntity<RoleMenu> {

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 菜单ID
     */
    private Long menuId;
}
