package com.ziwen.moudle.service.impl.rbac;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.ziwen.moudle.dto.rbac.RoleDTO;
import com.ziwen.moudle.entity.rbac.Role;
import com.ziwen.moudle.entity.rbac.RoleMenu;
import com.ziwen.moudle.service.rbac.RoleService;

import java.util.List;
import java.util.stream.Collectors;

import com.ziwen.moudle.mapper.rbac.RoleMapper;
import com.ziwen.moudle.mapper.rbac.RoleMenuMapper;

/**
 * 角色服务实现
 *
 * @author boot
 */
@Slf4j
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    public RoleServiceImpl(RoleMapper roleMapper, RoleMenuMapper roleMenuMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @Override
    public List<RoleDTO> listRoles(String roleName, String status) {
        List<Role> roles = roleMapper.selectRoleList(roleName);
        return roles.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public RoleDTO getRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            log.warn("角色不存在: {}", id);
            return null;
        }
        RoleDTO dto = convertToDto(role);
        // 查询角色关联的菜单
        List<Long> menuIds = roleMapper.selectRoleMenuIds(id);
        List<String> menuNames = roleMapper.selectRoleMenuNames(id);
        dto.setMenuIds(menuIds);
        dto.setMenuNames(menuNames);

        // 查询角色关联的用户
        List<Long> userIds = roleMapper.selectRoleUserIds(id);
        List<String> userNames = roleMapper.selectRoleUserNames(id);
        dto.setUserIds(userIds);
        dto.setUserNames(userNames);

        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleDTO createRole(RoleDTO roleDTO) {
        // 1. 参数校验
        if (roleDTO == null || roleDTO.getRoleName() == null) {
            throw new IllegalArgumentException("角色名称不能为空");
        }

        // 2. 检查角色名是否已存在
        if (checkRoleName(roleDTO.getRoleName(), null)) {
            throw new IllegalArgumentException("角色名已存在: " + roleDTO.getRoleName());
        }

        // 3. 保存到数据库
        Role role = convertToEntity(roleDTO);
        roleMapper.insert(role);

        // 4. 分配菜单权限
        if (!CollectionUtils.isEmpty(roleDTO.getMenuIds())) {
            assignRoleMenus(role.getId(), roleDTO.getMenuIds());
        }

        // 5. 返回角色信息
        return getRole(role.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleDTO updateRole(Long id, RoleDTO roleDTO) {
        // 1. 校验角色是否存在
        Role existRole = roleMapper.selectById(id);
        if (existRole == null) {
            log.warn("角色不存在: {}", id);
            throw new IllegalArgumentException("角色不存在");
        }

        // 2. 检查角色名是否重复
        if (checkRoleName(roleDTO.getRoleName(), id)) {
            throw new IllegalArgumentException("角色名已存在: " + roleDTO.getRoleName());
        }

        // 3. 更新角色信息
        Role role = convertToEntity(roleDTO);
        role.setId(id);
        roleMapper.updateById(role);

        // 4. 更新菜单权限
        assignRoleMenus(id, roleDTO.getMenuIds());

        // 5. 返回角色信息
        return getRole(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        // 1. 校验角色是否存在
        Role role = roleMapper.selectById(id);
        if (role == null) {
            log.warn("角色不存在: {}", id);
            throw new IllegalArgumentException("角色不存在");
        }

        // 2. 软删除角色（is_deleted = 1）
        Role updateRole = new Role();
        updateRole.setId(id);
        updateRole.setIsDeleted(1);
        roleMapper.updateById(updateRole);

        // 3. 软删除角色菜单关联
        roleMenuMapper.deleteRoleMenus(id);

        log.info("角色删除成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteRoles(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        // 1. 批量软删除角色
        ids.forEach(id -> {
            Role updateRole = new Role();
            updateRole.setId(id);
            updateRole.setIsDeleted(1);
            roleMapper.updateById(updateRole);
        });

        // 2. 批量软删除角色菜单关联
        roleMenuMapper.deleteRoleMenusBatch(ids);
    }

    @Override
    public List<RoleDTO> getRoleMenus(Long id) {
        // 查询角色关联的菜单列表
        List<String> menuNames = roleMapper.selectRoleMenuNames(id);
        RoleDTO dto = new RoleDTO();
        dto.setId(id);
        dto.setMenuNames(menuNames);
        return List.of(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoleMenus(Long id, List<Long> menuIds) {
        // 1. 校验角色是否存在
        Role role = roleMapper.selectById(id);
        if (role == null) {
            log.warn("角色不存在: {}", id);
            throw new IllegalArgumentException("角色不存在");
        }

        // 2. 软删除原有的菜单关联
        roleMenuMapper.deleteRoleMenus(id);

        // 3. 批量保存新的菜单关联
        if (!CollectionUtils.isEmpty(menuIds)) {
            List<RoleMenu> roleMenus = menuIds.stream()
                    .map(menuId -> {
                        RoleMenu roleMenu = new RoleMenu();
                        roleMenu.setRoleId(id);
                        roleMenu.setMenuId(menuId);
                        roleMenu.setIsDeleted(0);
                        return roleMenu;
                    })
                    .collect(Collectors.toList());
            roleMenuMapper.insertBatch(roleMenus);
        }

        log.info("角色菜单分配成功: {}", id);
    }

    @Override
    public List<RoleDTO> getRoleUsers(Long id) {
        // 查询角色关联的用户列表
        List<String> userNames = roleMapper.selectRoleUserNames(id);
        RoleDTO dto = new RoleDTO();
        dto.setId(id);
        dto.setUserNames(userNames);
        return List.of(dto);
    }

    @Override
    public boolean checkRoleName(String roleName, Long excludeId) {
        int count = roleMapper.checkRoleNameExists(roleName, excludeId);
        return count > 0;
    }

    /**
     * 转换Role为RoleDTO
     */
    private RoleDTO convertToDto(Role role) {
        if (role == null) {
            return null;
        }
        RoleDTO dto = new RoleDTO();
        BeanUtils.copyProperties(role, dto);
        return dto;
    }

    /**
     * 转换RoleDTO为Role
     */
    private Role convertToEntity(RoleDTO dto) {
        if (dto == null) {
            return null;
        }
        Role role = new Role();
        BeanUtils.copyProperties(dto, role);
        return role;
    }
}
