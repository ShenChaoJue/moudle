package com.ziwen.moudle.controller.rbac;

import com.ziwen.moudle.common.AjaxResult;
import com.ziwen.moudle.dto.rbac.RoleDTO;
import com.ziwen.moudle.service.rbac.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 角色管理 REST API
 *
 * @author boot
 */
@Slf4j
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 查询角色列表
     */
    @GetMapping("/list")
    public AjaxResult list(
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String status) {
        List<RoleDTO> roles = roleService.listRoles(roleName, status);
        return AjaxResult.success(roles);
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return AjaxResult.error("角色ID必须大于0");
        }

        RoleDTO role = roleService.getRole(id);
        if (role == null) {
            return AjaxResult.error("角色不存在: " + id);
        }
        return AjaxResult.success(role);
    }

    /**
     * 创建角色
     */
    @PostMapping
    public AjaxResult create(@RequestBody @Valid RoleDTO roleDTO) {
        if (roleDTO.getRoleName() == null || roleDTO.getRoleName().trim().isEmpty()) {
            return AjaxResult.error("角色名称不能为空");
        }

        try {
            RoleDTO created = roleService.createRole(roleDTO);
            return AjaxResult.success("角色创建成功", created);
        } catch (IllegalArgumentException e) {
            log.warn("创建角色失败: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable Long id, @RequestBody @Valid RoleDTO roleDTO) {
        if (id == null || id <= 0) {
            return AjaxResult.error("角色ID必须大于0");
        }

        if (roleDTO.getRoleName() == null || roleDTO.getRoleName().trim().isEmpty()) {
            return AjaxResult.error("角色名称不能为空");
        }

        try {
            RoleDTO updated = roleService.updateRole(id, roleDTO);
            return AjaxResult.success("角色更新成功", updated);
        } catch (IllegalArgumentException e) {
            log.warn("更新角色失败: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 删除角色（软删除）
     */
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return AjaxResult.error("角色ID必须大于0");
        }

        try {
            roleService.deleteRole(id);
            return AjaxResult.success();
        } catch (IllegalArgumentException e) {
            log.warn("删除角色失败: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除角色异常", e);
            return AjaxResult.error("删除角色失败");
        }
    }

    /**
     * 批量删除角色
     */
    @DeleteMapping("/batch")
    public AjaxResult batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return AjaxResult.error("角色ID列表不能为空");
        }

        try {
            roleService.batchDeleteRoles(ids);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("批量删除角色异常", e);
            return AjaxResult.error("批量删除角色失败");
        }
    }

    /**
     * 获取角色菜单
     */
    @GetMapping("/{id}/menus")
    public AjaxResult getRoleMenus(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return AjaxResult.error("角色ID必须大于0");
        }

        List<RoleDTO> menus = roleService.getRoleMenus(id);
        return AjaxResult.success(menus);
    }

    /**
     * 分配角色菜单
     */
    @PostMapping("/{id}/menus")
    public AjaxResult assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        if (id == null || id <= 0) {
            return AjaxResult.error("角色ID必须大于0");
        }

        if (menuIds == null || menuIds.isEmpty()) {
            return AjaxResult.error("菜单ID列表不能为空");
        }

        try {
            roleService.assignRoleMenus(id, menuIds);
            return AjaxResult.success();
        } catch (IllegalArgumentException e) {
            log.warn("分配角色菜单失败: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("分配角色菜单异常", e);
            return AjaxResult.error("分配角色菜单失败");
        }
    }

    /**
     * 获取角色用户
     */
    @GetMapping("/{id}/users")
    public AjaxResult getRoleUsers(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return AjaxResult.error("角色ID必须大于0");
        }

        List<RoleDTO> users = roleService.getRoleUsers(id);
        return AjaxResult.success(users);
    }

    /**
     * 检查角色名称是否已存在
     */
    @GetMapping("/check-code")
    public AjaxResult checkRoleCode(@RequestParam String roleName, @RequestParam(required = false) Long excludeId) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return AjaxResult.error("角色名称不能为空");
        }

        boolean exists = roleService.checkRoleName(roleName, excludeId);
        return AjaxResult.success(exists);
    }
}
