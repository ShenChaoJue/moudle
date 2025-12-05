package com.ziwen.moudle.entity.rbac;

import com.ziwen.moudle.entity.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户角色关联实体类（对应sys_user_role表）
 *
 * @author boot
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserRole extends BaseEntity<UserRole> {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色ID
     */
    private Long roleId;
}
