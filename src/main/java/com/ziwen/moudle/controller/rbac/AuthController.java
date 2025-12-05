package com.ziwen.moudle.controller.rbac;

import com.ziwen.moudle.common.AjaxResult;
import com.ziwen.moudle.dto.rbac.AuthDTO;
import com.ziwen.moudle.service.rbac.AuthService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 认证 REST API
 *
 * @author boot
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody AuthDTO.LoginRequest request) {
        if (request.getUserName() == null || request.getUserName().trim().isEmpty()) {
            return AjaxResult.warn("用户名不能为空");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return AjaxResult.warn("密码不能为空");
        }

        try {
            AuthDTO.LoginResponse response = authService.login(request);
            return AjaxResult.success("登录成功", response);
        } catch (IllegalArgumentException e) {
            log.warn("用户登录失败: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("用户登录异常", e);
            return AjaxResult.error("登录失败");
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public AjaxResult logout(@RequestParam String token) {
        if (token == null || token.trim().isEmpty()) {
            return AjaxResult.warn("Token不能为空");
        }

        try {
            authService.logout(token);
            return AjaxResult.success("登出成功");
        } catch (Exception e) {
            log.error("用户登出异常", e);
            return AjaxResult.error("登出失败");
        }
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public AjaxResult refresh(@RequestParam String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return AjaxResult.warn("刷新Token不能为空");
        }

        try {
            AuthDTO.LoginResponse response = authService.refreshToken(refreshToken);
            return AjaxResult.success("Token刷新成功", response);
        } catch (IllegalArgumentException e) {
            log.warn("Token刷新失败: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("Token刷新异常", e);
            return AjaxResult.error("Token刷新失败");
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current-user")
    public AjaxResult getCurrentUser(@RequestHeader("Authorization") String token) {
        if (token == null || token.trim().isEmpty()) {
            return AjaxResult.warn("Token不能为空");
        }

        try {
            AuthDTO.UserInfo userInfo = authService.getCurrentUser(token);
            return AjaxResult.success(userInfo);
        } catch (IllegalArgumentException e) {
            log.warn("获取当前用户信息失败: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取当前用户信息异常", e);
            return AjaxResult.error("获取用户信息失败");
        }
    }

    /**
     * 获取当前用户菜单
     */
    @GetMapping("/current-menus")
    public AjaxResult getCurrentMenus(@RequestHeader("Authorization") String token) {
        if (token == null || token.trim().isEmpty()) {
            return AjaxResult.warn("Token不能为空");
        }

        try {
            List<AuthDTO.MenuInfo> menus = authService.getCurrentMenus(token);
            return AjaxResult.success(menus);
        } catch (Exception e) {
            log.error("获取当前用户菜单异常", e);
            return AjaxResult.error("获取用户菜单失败");
        }
    }

    /**
     * 获取当前用户权限
     */
    @GetMapping("/current-permissions")
    public AjaxResult getCurrentPermissions(@RequestHeader("Authorization") String token) {
        if (token == null || token.trim().isEmpty()) {
            return AjaxResult.warn("Token不能为空");
        }

        try {
            List<String> permissions = authService.getCurrentPermissions(token);
            return AjaxResult.success(permissions);
        } catch (Exception e) {
            log.error("获取当前用户权限异常", e);
            return AjaxResult.error("获取用户权限失败");
        }
    }

}
