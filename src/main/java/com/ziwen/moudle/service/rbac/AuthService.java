package com.ziwen.moudle.service.rbac;

import java.util.List;

import com.ziwen.moudle.dto.rbac.AuthDTO;

/**
 * 认证服务接口
 *
 * @author boot
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    AuthDTO.LoginResponse login(AuthDTO.LoginRequest request);

    /**
     * 用户登出
     *
     * @param token Token
     */
    void logout(String token);

    /**
     * 刷新Token
     *
     * @param refreshToken 刷新Token
     * @return 新的访问Token
     */
    AuthDTO.LoginResponse refreshToken(String refreshToken);

    /**
     * 获取当前用户信息
     *
     * @param token 访问Token
     * @return 用户信息
     */
    AuthDTO.UserInfo getCurrentUser(String token);

    /**
     * 获取当前用户菜单
     *
     * @param token 访问Token
     * @return 菜单列表
     */
    List<AuthDTO.MenuInfo> getCurrentMenus(String token);

    /**
     * 获取当前用户权限
     *
     * @param token 访问Token
     * @return 权限列表
     */
    List<String> getCurrentPermissions(String token);

    /**
     * 校验Token是否有效
     *
     * @param token 访问Token
     * @return 是否有效
     */
    boolean validateToken(String token);

}
