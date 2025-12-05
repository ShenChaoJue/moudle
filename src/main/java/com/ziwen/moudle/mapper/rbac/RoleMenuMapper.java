package com.ziwen.moudle.mapper.rbac;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ziwen.moudle.entity.rbac.RoleMenu;

import java.util.List;

/**
 * 角色菜单关联Mapper接口
 *
 * @author boot
 */
@Mapper
public interface RoleMenuMapper {

    /**
     * 删除菜单关联的角色
     */
    int deleteMenuRoles(@Param("menuId") Long menuId);

    /**
     * 批量删除菜单关联的角色
     */
    int deleteMenuRolesBatch(@Param("menuIds") List<Long> menuIds);

    /**
     * 删除角色关联的菜单
     */
    int deleteRoleMenus(@Param("roleId") Long roleId);

    /**
     * 批量删除角色关联的菜单
     */
    int deleteRoleMenusBatch(@Param("roleIds") List<Long> roleIds);

    /**
     * 批量插入角色菜单关联
     */
    int insertBatch(@Param("roleMenus") List<RoleMenu> roleMenus);
}
