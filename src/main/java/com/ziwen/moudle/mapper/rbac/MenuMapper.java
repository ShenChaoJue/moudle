package com.ziwen.moudle.mapper.rbac;

import com.ziwen.moudle.entity.rbac.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单Mapper接口
 *
 * @author boot
 */
@Mapper
public interface MenuMapper {

    /**
     * 查询所有菜单
     */
    List<Menu> selectAllMenus();

    /**
     * 查询菜单列表（带过滤条件）
     */
    List<Menu> selectMenuList(@Param("menuName") String menuName, @Param("type") String type);

    /**
     * 根据菜单ID查询关联的角色ID列表
     */
    List<Long> selectMenuRoleIds(@Param("menuId") Long menuId);

    /**
     * 查询子菜单
     */
    List<Menu> selectChildren(@Param("parentId") Long parentId);

    /**
     * 根据用户ID查询用户菜单权限
     */
    List<Menu> selectMenusByUserId(@Param("userId") Long userId);

    /**
     * 查询所有菜单路径
     */
    List<String> selectAllMenuPaths();

    /**
     * 检查菜单名称是否存在
     */
    int checkMenuNameExists(@Param("menuName") String menuName, @Param("parentId") Long parentId, @Param("excludeId") Long excludeId);

    /**
     * 根据ID查询菜单
     */
    Menu selectById(@Param("id") Long id);

    /**
     * 插入菜单
     */
    void insert(Menu menu);

    /**
     * 根据ID更新菜单
     */
    void updateById(Menu menu);
}
