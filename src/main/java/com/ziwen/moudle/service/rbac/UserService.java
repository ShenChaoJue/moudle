package com.ziwen.moudle.service.rbac;
import java.util.List;

import com.ziwen.moudle.dto.rbac.UserDTO;

/**
 * 用户服务接口
 *
 * @author boot
 */
public interface UserService {

    /**
     * 查询用户列表
     *
     * @param userName 用户名称（可选）
     * @param status 状态（可选）
     * @return 用户列表
     */
    List<UserDTO> listUsers(String userName, String status);

    /**
     * 获取用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    UserDTO getUser(Long id);

    /**
     * 创建用户
     *
     * @param userDTO 用户信息
     * @return 创建后的用户信息
     */
    UserDTO createUser(UserDTO userDTO);

    /**
     * 更新用户
     *
     * @param id 用户ID
     * @param userDTO 用户信息
     * @return 更新后的用户信息
     */
    UserDTO updateUser(Long id, UserDTO userDTO);

    /**
     * 删除用户（软删除）
     *
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 批量删除用户
     *
     * @param ids 用户ID列表
     */
    void batchDeleteUsers(List<Long> ids);

    /**
     * 获取用户角色
     *
     * @param id 用户ID
     * @return 角色列表
     */
    List<UserDTO> getUserRoles(Long id);

    /**
     * 分配用户角色
     *
     * @param id 用户ID
     * @param roleIds 角色ID列表
     */
    void assignUserRoles(Long id, List<Long> roleIds);

    /**
     * 重置用户密码
     *
     * @param id 用户ID
     */
    void resetPassword(Long id);

    /**
     * 修改用户密码
     *
     * @param id 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long id, String oldPassword, String newPassword);

    /**
     * 检查用户名是否已存在
     *
     * @param userName 用户名
     * @param excludeId 排除的用户ID（用于更新时检查）
     * @return 是否存在
     */
    boolean checkUserName(String userName, Long excludeId);

    /**
     * 根据用户名查询密码
     *
     * @param userName 用户名
     * @return 加密后的密码
     */
    String getPasswordByUserName(String userName);
}
