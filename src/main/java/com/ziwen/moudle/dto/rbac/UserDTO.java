package com.ziwen.moudle.dto.rbac;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户 DTO（与sys_user表字段完全一致）
 *
 * @author boot
 */
@Data
public class UserDTO {

    /**
     * 用户ID（雪花ID）
     */
    private Long id;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 密码（加密）
     */
    private String password;

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
     * 角色ID列表（用于角色分配）
     */
    private List<Long> roleIds;

    /**
     * 角色名称列表（用于显示）
     */
    private List<String> roleNames;
}
