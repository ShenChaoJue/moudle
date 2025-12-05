package com.ziwen.moudle.service.rbac;

import java.util.List;

import com.ziwen.moudle.dto.rbac.RoleDTO;

/**
 * 角色服务接口
 *
 * @author boot
 */
public interface RoleService {

    /**
     * 查询角色列表
     *
     * @param roleName 角色名称（可选）
     * @param status 状态（可选）
     * @return 角色列表
     */
    List<RoleDTO> listRoles(String roleName, String status);

    /**
     * 获取角色详情
     *
     * @param id 角色ID
     * @return 角色详情
     */
    RoleDTO getRole(Long id);

    /**
     * 创建角色
     *
     * @param roleDTO 角色信息
     * @return 创建后的角色信息
     */
    RoleDTO createRole(RoleDTO roleDTO);

    /**
     * 更新角色
     *
     * @param id 角色ID
     * @param roleDTO 角色信息
     * @return 更新后的角色信息
     */
    RoleDTO updateRole(Long id, RoleDTO roleDTO);

    /**
     * 删除角色（软删除）
     *
     * @param id 角色ID
     */
    void deleteRole(Long id);

    /**
     * 批量删除角色
     *
     * @param ids 角色ID列表
     */
    void batchDeleteRoles(List<Long> ids);

    /**
     * 获取角色菜单
     *
     * @param id 角色ID
     * @return 菜单列表
     */
    List<RoleDTO> getRoleMenus(Long id);

    /**
     * 分配角色菜单
     *
     * @param id 角色ID
     * @param menuIds 菜单ID列表
     */
    void assignRoleMenus(Long id, List<Long> menuIds);

    /**
     * 获取角色用户
     *
     * @param id 角色ID
     * @return 用户列表
     */
    List<RoleDTO> getRoleUsers(Long id);

    /**
     * 检查角色名称是否已存在
     *
     * @param roleName 角色名称
     * @param excludeId 排除的角色ID（用于更新时检查）
     * @return 是否存在
     */
    boolean checkRoleName(String roleName, Long excludeId);
}
