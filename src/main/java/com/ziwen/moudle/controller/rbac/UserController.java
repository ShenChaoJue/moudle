package com.ziwen.moudle.controller.rbac;

import com.ziwen.moudle.common.AjaxResult;
import com.ziwen.moudle.dto.rbac.UserDTO;
import com.ziwen.moudle.service.rbac.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 用户管理 REST API
 *
 * @author boot
 */
@Slf4j
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 查询用户列表
     */
    @GetMapping("/list")
    public AjaxResult list(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String status) {
        List<UserDTO> users = userService.listUsers(userName, status);
        return AjaxResult.success(users);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return AjaxResult.error("用户ID必须大于0");
        }

        UserDTO user = userService.getUser(id);
        if (user == null) {
            return AjaxResult.error("用户不存在: " + id);
        }
        return AjaxResult.success(user);
    }

    /**
     * 创建用户
     */
    @PostMapping
    public AjaxResult create(@RequestBody @Valid UserDTO userDTO) {
        if (userDTO.getUserName() == null || userDTO.getUserName().trim().isEmpty()) {
            return AjaxResult.error("用户名不能为空");
        }

        if (userDTO.getPassword() == null || userDTO.getPassword().length() < 6) {
            return AjaxResult.error("密码长度不能少于6位");
        }

        try {
            UserDTO created = userService.createUser(userDTO);
            return AjaxResult.success("用户创建成功", created);
        } catch (IllegalArgumentException e) {
            log.warn("创建用户失败: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable Long id, @RequestBody @Valid UserDTO userDTO) {
        if (id == null || id <= 0) {
            return AjaxResult.error("用户ID必须大于0");
        }

        if (userDTO.getUserName() == null || userDTO.getUserName().trim().isEmpty()) {
            return AjaxResult.error("用户名不能为空");
        }

        try {
            UserDTO updated = userService.updateUser(id, userDTO);
            return AjaxResult.success("用户更新成功", updated);
        } catch (IllegalArgumentException e) {
            log.warn("更新用户失败: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 删除用户（软删除）
     */
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return AjaxResult.error("用户ID必须大于0");
        }

        try {
            userService.deleteUser(id);
            return AjaxResult.success();
        } catch (IllegalArgumentException e) {
            log.warn("删除用户失败: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除用户异常", e);
            return AjaxResult.error("删除用户失败");
        }
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    public AjaxResult batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return AjaxResult.error("用户ID列表不能为空");
        }

        try {
            userService.batchDeleteUsers(ids);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("批量删除用户异常", e);
            return AjaxResult.error("批量删除用户失败");
        }
    }

    /**
     * 获取用户角色
     */
    @GetMapping("/{id}/roles")
    public AjaxResult getUserRoles(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return AjaxResult.error("用户ID必须大于0");
        }

        List<UserDTO> roles = userService.getUserRoles(id);
        return AjaxResult.success(roles);
    }

    /**
     * 分配用户角色
     */
    @PostMapping("/{id}/roles")
    public AjaxResult assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        if (id == null || id <= 0) {
            return AjaxResult.error("用户ID必须大于0");
        }

        if (roleIds == null || roleIds.isEmpty()) {
            return AjaxResult.error("角色ID列表不能为空");
        }

        try {
            userService.assignUserRoles(id, roleIds);
            return AjaxResult.success();
        } catch (IllegalArgumentException e) {
            log.warn("分配用户角色失败: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("分配用户角色异常", e);
            return AjaxResult.error("分配用户角色失败");
        }
    }

    /**
     * 重置用户密码
     */
    @PutMapping("/{id}/reset-password")
    public AjaxResult resetPassword(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return AjaxResult.error("用户ID必须大于0");
        }

        try {
            userService.resetPassword(id);
            return AjaxResult.success();
        } catch (IllegalArgumentException e) {
            log.warn("重置用户密码失败: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("重置用户密码异常", e);
            return AjaxResult.error("重置密码失败");
        }
    }

    /**
     * 修改用户密码
     */
    @PutMapping("/{id}/change-password")
    public AjaxResult changePassword(@PathVariable Long id,
                                     @RequestParam String oldPassword,
                                     @RequestParam String newPassword) {
        if (id == null || id <= 0) {
            return AjaxResult.error("用户ID必须大于0");
        }

        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return AjaxResult.error("原密码不能为空");
        }

        if (newPassword == null || newPassword.length() < 6) {
            return AjaxResult.error("新密码长度不能少于6位");
        }

        try {
            userService.changePassword(id, oldPassword, newPassword);
            return AjaxResult.success();
        } catch (IllegalArgumentException e) {
            log.warn("修改用户密码失败: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("修改用户密码异常", e);
            return AjaxResult.error("密码修改失败");
        }
    }
}
