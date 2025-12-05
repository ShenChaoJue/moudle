package com.ziwen.moudle.service.impl.rbac;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.ziwen.moudle.dto.rbac.AuthDTO;
import com.ziwen.moudle.dto.rbac.MenuDTO;
import com.ziwen.moudle.dto.rbac.UserDTO;
import com.ziwen.moudle.service.rbac.MenuService;
import com.ziwen.moudle.service.rbac.UserService;
import com.ziwen.moudle.service.rbac.AuthService;
import com.ziwen.moudle.utils.PasswordUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 认证服务实现
 *
 * @author boot
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final MenuService menuService;

    public AuthServiceImpl(UserService userService, MenuService menuService) {
        this.userService = userService;
        this.menuService = menuService;
    }

    // Token黑名单（使用ConcurrentHashMap保证线程安全）
    // 生产环境建议使用Redis
    private static final ConcurrentMap<String, LocalDateTime> TOKEN_BLACKLIST = new ConcurrentHashMap<>();

    // 刷新Token存储（token -> 用户ID）
    // 生产环境建议使用Redis
    private static final ConcurrentMap<String, Long> REFRESH_TOKEN_MAP = new ConcurrentHashMap<>();

    @Override
    public AuthDTO.LoginResponse login(AuthDTO.LoginRequest request) {
        // 1. 校验用户名和密码
        if (request.getUserName() == null || request.getUserName().trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 2. 查询密码并验证
        String encryptedPassword = userService.getPasswordByUserName(request.getUserName());
        if (encryptedPassword == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 3. 验证密码
        boolean matches = PasswordUtil.matches(request.getPassword(), encryptedPassword);
        if (!matches) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 4. 查询用户信息
        List<UserDTO> users = userService.listUsers(request.getUserName(), null);
        if (users.isEmpty()) {
            throw new IllegalArgumentException("用户不存在");
        }

        UserDTO user = users.stream()
                .filter(u -> request.getUserName().equals(u.getUserName()))
                .findFirst()
                .orElse(null);

        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 5. 验证用户状态（未删除）
        if (user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            throw new IllegalArgumentException("用户已被删除");
        }

        // 6. 生成Token（简化版本，实际应使用JWT）
        String accessToken = generateAccessToken(user.getId(), user.getUserName());
        String refreshToken = generateRefreshToken(user.getId());

        // 7. 存储刷新Token
        REFRESH_TOKEN_MAP.put(refreshToken, user.getId());

        // 8. 查询用户角色和菜单
        List<String> roleNames = user.getRoleNames();
        List<MenuDTO> menus = menuService.getUserMenus(user.getId());

        // 9. 构建登录响应
        AuthDTO.LoginResponse response = new AuthDTO.LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(7200L); // 2小时

        // 用户信息
        AuthDTO.UserInfo userInfo = new AuthDTO.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUserName(user.getUserName());
        userInfo.setRoleNames(roleNames);
        userInfo.setLoginTime(LocalDateTime.now());
        response.setUserInfo(userInfo);

        // 菜单信息
        List<AuthDTO.MenuInfo> menuInfos = convertMenuDTOToMenuInfo(menus);
        response.setMenus(menuInfos);

        log.info("用户登录成功: {}", user.getUserName());

        return response;
    }

    @Override
    public void logout(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token不能为空");
        }

        // 将Token加入黑名单，过期时间为2小时后
        TOKEN_BLACKLIST.put(token, LocalDateTime.now().plusHours(2));

        log.info("用户登出，Token已加入黑名单");
    }

    @Override
    public AuthDTO.LoginResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new IllegalArgumentException("刷新Token不能为空");
        }

        // 1. 验证refreshToken是否存在
        Long userId = REFRESH_TOKEN_MAP.get(refreshToken);
        if (userId == null) {
            throw new IllegalArgumentException("刷新Token无效");
        }

        // 2. 查询用户信息
        UserDTO user = userService.getUser(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 3. 验证用户状态
        if (user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            throw new IllegalArgumentException("用户已被删除");
        }

        // 4. 生成新的accessToken
        String accessToken = generateAccessToken(user.getId(), user.getUserName());

        // 5. 构建响应
        AuthDTO.LoginResponse response = new AuthDTO.LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken); // 继续使用原来的refreshToken
        response.setTokenType("Bearer");
        response.setExpiresIn(7200L);

        // 用户信息
        AuthDTO.UserInfo userInfo = new AuthDTO.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUserName(user.getUserName());
        userInfo.setRoleNames(user.getRoleNames());
        userInfo.setLoginTime(LocalDateTime.now());
        response.setUserInfo(userInfo);

        // 菜单信息
        List<MenuDTO> menus = menuService.getUserMenus(user.getId());
        List<AuthDTO.MenuInfo> menuInfos = convertMenuDTOToMenuInfo(menus);
        response.setMenus(menuInfos);

        log.info("刷新Token成功: {}", user.getUserName());

        return response;
    }

    @Override
    public AuthDTO.UserInfo getCurrentUser(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token不能为空");
        }

        // 验证Token
        if (!validateToken(token)) {
            throw new IllegalArgumentException("Token无效或已过期");
        }

        // 解析Token获取用户ID和用户名
        TokenInfo tokenInfo = parseToken(token);
        if (tokenInfo == null) {
            throw new IllegalArgumentException("Token解析失败");
        }

        // 查询用户信息
        UserDTO user = userService.getUser(tokenInfo.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 构建用户信息
        AuthDTO.UserInfo userInfo = new AuthDTO.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUserName(user.getUserName());
        userInfo.setRoleNames(user.getRoleNames());
        userInfo.setLoginTime(LocalDateTime.now());

        return userInfo;
    }

    @Override
    public List<AuthDTO.MenuInfo> getCurrentMenus(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token不能为空");
        }

        // 验证Token
        if (!validateToken(token)) {
            throw new IllegalArgumentException("Token无效或已过期");
        }

        // 解析Token获取用户ID
        TokenInfo tokenInfo = parseToken(token);
        if (tokenInfo == null) {
            throw new IllegalArgumentException("Token解析失败");
        }

        // 查询用户菜单
        List<MenuDTO> menus = menuService.getUserMenus(tokenInfo.getUserId());
        return convertMenuDTOToMenuInfo(menus);
    }

    @Override
    public List<String> getCurrentPermissions(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token不能为空");
        }

        // 验证Token
        if (!validateToken(token)) {
            throw new IllegalArgumentException("Token无效或已过期");
        }

        // 解析Token获取用户ID
        TokenInfo tokenInfo = parseToken(token);
        if (tokenInfo == null) {
            throw new IllegalArgumentException("Token解析失败");
        }

        // 查询用户菜单，提取权限（路径）
        List<MenuDTO> menus = menuService.getUserMenus(tokenInfo.getUserId());
        return extractPermissions(menus);
    }

    @Override
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        // 检查是否在黑名单中
        if (TOKEN_BLACKLIST.containsKey(token)) {
            return false;
        }

        // 解析Token并检查是否过期
        TokenInfo tokenInfo = parseToken(token);
        if (tokenInfo == null) {
            return false;
        }

        LocalDateTime expireTime = tokenInfo.getExpireTime();
        return expireTime != null && expireTime.isAfter(LocalDateTime.now());
    }

    /**
     * 生成访问令牌（简化版本，实际应使用JWT）
     */
    private String generateAccessToken(Long userId, String userName) {
        // 生成Token格式：userId|username|expireTime (Base64编码)
        LocalDateTime expireTime = LocalDateTime.now().plusHours(2);
        String token = String.format("%d|%s|%s", userId, userName, expireTime.toString());
        return java.util.Base64.getEncoder().encodeToString(token.getBytes());
    }

    /**
     * 生成刷新令牌
     */
    private String generateRefreshToken(Long userId) {
        LocalDateTime expireTime = LocalDateTime.now().plusDays(7);
        String token = String.format("%d|refresh|%s", userId, expireTime.toString());
        return java.util.Base64.getEncoder().encodeToString(token.getBytes());
    }

    /**
     * 解析Token信息
     */
    private TokenInfo parseToken(String token) {
        try {
            String decoded = new String(java.util.Base64.getDecoder().decode(token));
            String[] parts = decoded.split("\\|");
            if (parts.length != 3) {
                return null;
            }

            Long userId = Long.valueOf(parts[0]);
            String userName = parts[1];
            LocalDateTime expireTime = LocalDateTime.parse(parts[2]);

            return new TokenInfo(userId, userName, expireTime);
        } catch (Exception e) {
            log.warn("Token解析失败", e);
            return null;
        }
    }

    /**
     * 提取权限列表（从菜单中获取路径）
     */
    private List<String> extractPermissions(List<MenuDTO> menus) {
        return menus.stream()
                .filter(menu -> menu.getPath() != null && !menu.getPath().trim().isEmpty())
                .map(MenuDTO::getPath)
                .collect(Collectors.toList());
    }

    /**
     * Token信息内部类
     */
    private static class TokenInfo {
        private final Long userId;
        private final String userName;
        private final LocalDateTime expireTime;

        public TokenInfo(Long userId, String userName, LocalDateTime expireTime) {
            this.userId = userId;
            this.userName = userName;
            this.expireTime = expireTime;
        }

        public Long getUserId() {
            return userId;
        }

        @SuppressWarnings("unused")
        public String getUserName() {
            return userName;
        }

        public LocalDateTime getExpireTime() {
            return expireTime;
        }
    }

    /**
     * 转换MenuDTO为AuthDTO.MenuInfo
     */
    private List<AuthDTO.MenuInfo> convertMenuDTOToMenuInfo(List<MenuDTO> menus) {
        return menus.stream()
                .map(this::convertMenuDTOToMenuInfo)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个MenuDTO为AuthDTO.MenuInfo
     */
    private AuthDTO.MenuInfo convertMenuDTOToMenuInfo(MenuDTO menuDTO) {
        if (menuDTO == null) {
            return null;
        }

        AuthDTO.MenuInfo menuInfo = new AuthDTO.MenuInfo();
        menuInfo.setId(menuDTO.getId());
        menuInfo.setMenuName(menuDTO.getMenuName());
        menuInfo.setPath(menuDTO.getPath());
        menuInfo.setType(menuDTO.getType());
        menuInfo.setParentId(menuDTO.getParentId());
        menuInfo.setSort(menuDTO.getSort());

        // 递归转换子菜单
        if (menuDTO.getChildren() != null && !menuDTO.getChildren().isEmpty()) {
            List<AuthDTO.MenuInfo> children = menuDTO.getChildren().stream()
                    .map(this::convertMenuDTOToMenuInfo)
                    .collect(Collectors.toList());
            menuInfo.setChildren(children);
        }

        return menuInfo;
    }
}
