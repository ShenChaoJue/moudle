package com.ziwen.moudle.entity.rbac;

import com.ziwen.moudle.entity.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类（对应sys_user表）
 *
 * @author boot
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity<User> {

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 密码（加密）
     */
    private String password;
}
