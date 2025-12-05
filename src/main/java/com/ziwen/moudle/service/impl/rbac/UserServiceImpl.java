package com.ziwen.moudle.service.impl.rbac;

import com.ziwen.moudle.dto.rbac.UserDTO;
import com.ziwen.moudle.entity.rbac.User;
import com.ziwen.moudle.entity.rbac.UserRole;
import com.ziwen.moudle.mapper.rbac.UserMapper;
import com.ziwen.moudle.mapper.rbac.UserRoleMapper;
import com.ziwen.moudle.service.rbac.UserService;
import com.ziwen.moudle.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 *
 * @author boot
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    public UserServiceImpl(UserMapper userMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public List<UserDTO> listUsers(String userName, String status) {
        // 查询用户列表
        List<User> users = userMapper.selectUserList(userName);
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO getUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            log.warn("用户不存在: {}", id);
            return null;
        }
        UserDTO dto = convertToDto(user);
        // 查询用户角色
        List<Long> roleIds = userMapper.selectUserRoleIds(id);
        List<String> roleNames = userMapper.selectUserRoleNames(id);
        dto.setRoleIds(roleIds);
        dto.setRoleNames(roleNames);
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO createUser(UserDTO userDTO) {
        // 1. 参数校验
        if (userDTO == null || userDTO.getUserName() == null) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        // 2. 检查用户名是否已存在
        if (checkUserName(userDTO.getUserName(), null)) {
            throw new IllegalArgumentException("用户名已存在: " + userDTO.getUserName());
        }

        // 3. 加密密码
        String encodedPassword = PasswordUtil.encrypt(userDTO.getPassword());
        userDTO.setPassword(encodedPassword);

        // 4. 保存到数据库
        User user = convertToEntity(userDTO);
        userMapper.insert(user);

        // 5. 分配角色
        if (!CollectionUtils.isEmpty(userDTO.getRoleIds())) {
            assignUserRoles(user.getId(), userDTO.getRoleIds());
        }

        // 6. 返回用户信息
        return getUser(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        // 1. 校验用户是否存在
        User existUser = userMapper.selectById(id);
        if (existUser == null) {
            log.warn("用户不存在: {}", id);
            throw new IllegalArgumentException("用户不存在");
        }

        // 2. 检查用户名是否重复
        if (checkUserName(userDTO.getUserName(), id)) {
            throw new IllegalArgumentException("用户名已存在: " + userDTO.getUserName());
        }

        // 3. 更新用户信息（不更新密码）
        User user = convertToEntity(userDTO);
        user.setId(id);
        user.setPassword(null); // 不更新密码
        userMapper.updateById(user);

        // 4. 更新角色关联
        assignUserRoles(id, userDTO.getRoleIds());

        // 5. 返回用户信息
        return getUser(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        // 1. 校验用户是否存在
        User user = userMapper.selectById(id);
        if (user == null) {
            log.warn("用户不存在: {}", id);
            throw new IllegalArgumentException("用户不存在");
        }

        // 2. 软删除用户（is_deleted = 1）
        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setIsDeleted(1);
        userMapper.updateById(updateUser);

        // 3. 软删除用户角色关联
        userRoleMapper.deleteUserRoles(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteUsers(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        // 1. 批量软删除用户
        ids.forEach(id -> {
            User updateUser = new User();
            updateUser.setId(id);
            updateUser.setIsDeleted(1);
            userMapper.updateById(updateUser);
        });

        // 2. 批量软删除用户角色关联
        userRoleMapper.deleteUserRolesBatch(ids);
    }

    @Override
    public List<UserDTO> getUserRoles(Long id) {
        // 查询用户关联的角色列表
        List<String> roleNames = userMapper.selectUserRoleNames(id);
        UserDTO dto = new UserDTO();
        dto.setId(id);
        dto.setRoleNames(roleNames);
        return List.of(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRoles(Long id, List<Long> roleIds) {
        // 1. 校验用户是否存在
        User user = userMapper.selectById(id);
        if (user == null) {
            log.warn("用户不存在: {}", id);
            throw new IllegalArgumentException("用户不存在");
        }

        // 2. 软删除原有的角色关联
        userRoleMapper.deleteUserRoles(id);

        // 3. 批量保存新的角色关联
        if (!CollectionUtils.isEmpty(roleIds)) {
            List<UserRole> userRoles = roleIds.stream()
                    .map(roleId -> {
                        UserRole userRole = new UserRole();
                        userRole.setUserId(id);
                        userRole.setRoleId(roleId);
                        userRole.setIsDeleted(0);
                        return userRole;
                    })
                    .collect(Collectors.toList());
            userRoleMapper.insertBatch(userRoles);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id) {
        // 1. 校验用户是否存在
        User user = userMapper.selectById(id);
        if (user == null) {
            log.warn("用户不存在: {}", id);
            throw new IllegalArgumentException("用户不存在");
        }

        // 2. 生成默认密码（如123456）
        String defaultPassword = "123456";
        String encodedPassword = PasswordUtil.encrypt(defaultPassword);

        // 3. 更新密码
        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setPassword(encodedPassword);
        userMapper.updateById(updateUser);

        log.info("用户密码已重置: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long id, String oldPassword, String newPassword) {
        // 1. 校验用户是否存在
        User user = userMapper.selectById(id);
        if (user == null) {
            log.warn("用户不存在: {}", id);
            throw new IllegalArgumentException("用户不存在");
        }

        // 2. 验证旧密码是否正确
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码不正确");
        }

        // 3. 加密新密码并更新
        String encodedPassword = PasswordUtil.encrypt(newPassword);
        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setPassword(encodedPassword);
        userMapper.updateById(updateUser);

        log.info("用户密码已修改: {}", id);
    }

    @Override
    public boolean checkUserName(String userName, Long excludeId) {
        int count = userMapper.checkUserNameExists(userName, excludeId);
        return count > 0;
    }

    @Override
    public String getPasswordByUserName(String userName) {
        return userMapper.selectPasswordByUserName(userName);
    }

    /**
     * 转换User为UserDTO
     */
    private UserDTO convertToDto(User user) {
        if (user == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }

    /**
     * 转换UserDTO为User
     */
    private User convertToEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        return user;
    }
}
