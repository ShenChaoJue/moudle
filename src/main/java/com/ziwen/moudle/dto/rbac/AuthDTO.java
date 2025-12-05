package com.ziwen.moudle.dto.rbac;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证 DTO
 *
 * @author boot
 */
public class AuthDTO {

    /**
     * 登录请求
     */
    @Data
    public static class LoginRequest {
        /**
         * 用户名
         */
        private String userName;

        /**
         * 密码
         */
        private String password;

        /**
         * 验证码（可选）
         */
        private String captcha;

        /**
         * 验证码Key（可选）
         */
        private String captchaKey;
    }

    /**
     * 登录响应
     */
    @Data
    public static class LoginResponse {
        /**
         * 访问Token
         */
        private String accessToken;

        /**
         * 刷新Token
         */
        private String refreshToken;

        /**
         * Token类型
         */
        private String tokenType = "Bearer";

        /**
         * 过期时间（秒）
         */
        private Long expiresIn;

        /**
         * 用户信息
         */
        private UserInfo userInfo;

        /**
         * 菜单列表
         */
        private List<MenuInfo> menus;
    }

    /**
     * 用户信息
     */
    @Data
    public static class UserInfo {
        /**
         * 用户ID
         */
        private Long id;

        /**
         * 用户名
         */
        private String userName;

        /**
         * 昵称
         */
        private String nickname;

        /**
         * 邮箱
         */
        private String email;

        /**
         * 手机号
         */
        private String mobile;

        /**
         * 角色名称列表
         */
        private List<String> roleNames;

        /**
         * 登录时间
         */
        private LocalDateTime loginTime;
    }

    /**
     * 菜单信息（用于前端菜单显示）
     */
    @Data
    public static class MenuInfo {
        /**
         * 菜单ID
         */
        private Long id;

        /**
         * 菜单名称
         */
        private String menuName;

        /**
         * 路径
         */
        private String path;

        /**
         * 类型
         */
        private String type;

        /**
         * 父ID
         */
        private Long parentId;

        /**
         * 排序
         */
        private Integer sort;

        /**
         * 菜单图标
         */
        private String icon;

        /**
         * 子菜单
         */
        private List<MenuInfo> children;
    }
}
