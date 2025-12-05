package com.ziwen.moudle.mapper.rbac;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ziwen.moudle.entity.rbac.UserRole;

import java.util.List;

/**
 * 用户角色关联Mapper接口
 *
 * @author boot
 */
@Mapper
public interface UserRoleMapper {

    /**
     * 删除用户角色关联（软删除）
     *
     * @param userId 用户ID
     */
    void deleteUserRoles(@Param("userId") Long userId);

    /**
     * 批量删除用户角色关联（软删除）
     *
     * @param userIds 用户ID列表
     */
    void deleteUserRolesBatch(@Param("userIds") List<Long> userIds);

    /**
     * 批量插入用户角色关联
     *
     * @param userRoles 用户角色关联列表
     */
    void insertBatch(List<UserRole> userRoles);
}
