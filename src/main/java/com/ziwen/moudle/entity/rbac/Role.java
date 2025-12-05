package com.ziwen.moudle.entity.rbac;

import com.ziwen.moudle.entity.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体类（对应sys_role表）
 *
 * @author boot
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseEntity<Role> {

    /**
     * 角色名称
     */
    private String roleName;
}
