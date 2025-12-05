package com.ziwen.moudle.mapper.rbac;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ziwen.moudle.entity.rbac.User;

import java.util.List;

/**
 * 用户Mapper接口
 *
 * @author boot
 */
@Mapper
public interface UserMapper {

    /**
     * 查询用户列表
     *
     * @param userName 用户名（可选）
     * @return 用户列表
     */
    List<User> selectUserList(@Param("userName") String userName);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    User selectById(@Param("id") Long id);

    /**
     * 查询用户角色ID列表
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> selectUserRoleIds(@Param("userId") Long userId);

    /**
     * 查询用户角色名称列表
     *
     * @param userId 用户ID
     * @return 角色名称列表
     */
    List<String> selectUserRoleNames(@Param("userId") Long userId);

    /**
     * 插入用户
     *
     * @param user 用户信息
     */
    void insert(User user);

    /**
     * 根据ID更新用户
     *
     * @param user 用户信息
     */
    void updateById(User user);

    /**
     * 检查用户名是否存在
     *
     * @param userName 用户名
     * @param excludeId 排除的用户ID（用于更新时排除自己）
     * @return 存在数量
     */
    int checkUserNameExists(@Param("userName") String userName, @Param("excludeId") Long excludeId);

    /**
     * 根据用户名查询密码
     *
     * @param userName 用户名
     * @return 密码（加密后的）
     */
    String selectPasswordByUserName(@Param("userName") String userName);
}
