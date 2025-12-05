package com.ziwen.moudle.mapper.rbac;

import com.ziwen.moudle.entity.rbac.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色Mapper接口
 *
 * @author boot
 */
@Mapper
public interface RoleMapper {

    /**
     * 查询角色列表
     */
    List<Role> selectRoleList(@Param("roleName") String roleName);

    /**
     * 根据ID查询角色
     */
    Role selectById(@Param("id") Long id);

    /**
     * 查询角色关联的菜单ID列表
     */
    List<Long> selectRoleMenuIds(@Param("roleId") Long roleId);

    /**
     * 查询角色关联的菜单名称列表
     */
    List<String> selectRoleMenuNames(@Param("roleId") Long roleId);

    /**
     * 查询角色关联的用户ID列表
     */
    List<Long> selectRoleUserIds(@Param("roleId") Long roleId);

    /**
     * 查询角色关联的用户名称列表
     */
    List<String> selectRoleUserNames(@Param("roleId") Long roleId);

    /**
     * 插入角色
     */
    void insert(Role role);

    /**
     * 根据ID更新角色
     */
    void updateById(Role role);

    /**
     * 检查角色名称是否存在
     */
    int checkRoleNameExists(@Param("roleName") String roleName, @Param("excludeId") Long excludeId);
}
