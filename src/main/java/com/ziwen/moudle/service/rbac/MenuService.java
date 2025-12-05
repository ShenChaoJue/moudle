package com.ziwen.moudle.service.rbac;

import java.util.List;

import com.ziwen.moudle.dto.rbac.MenuDTO;

/**
 * 菜单服务接口
 *
 * @author boot
 */
public interface MenuService {

    /**
     * 获取菜单树（树形结构）
     *
     * @param menuName 菜单名称（可选）
     * @param type 类型（可选）
     * @return 菜单树
     */
    List<MenuDTO> getMenuTree(String menuName, String type);

    /**
     * 查询平铺菜单列表
     *
     * @param menuName 菜单名称（可选）
     * @param type 类型（可选）
     * @return 菜单列表
     */
    List<MenuDTO> listMenus(String menuName, String type);

    /**
     * 获取菜单详情
     *
     * @param id 菜单ID
     * @return 菜单详情
     */
    MenuDTO getMenu(Long id);

    /**
     * 创建菜单
     *
     * @param menuDTO 菜单信息
     * @return 创建后的菜单信息
     */
    MenuDTO createMenu(MenuDTO menuDTO);

    /**
     * 更新菜单
     *
     * @param id 菜单ID
     * @param menuDTO 菜单信息
     * @return 更新后的菜单信息
     */
    MenuDTO updateMenu(Long id, MenuDTO menuDTO);

    /**
     * 删除菜单（软删除）
     *
     * @param id 菜单ID
     */
    void deleteMenu(Long id);

    /**
     * 批量删除菜单
     *
     * @param ids 菜单ID列表
     */
    void batchDeleteMenus(List<Long> ids);

    /**
     * 获取子菜单
     *
     * @param id 父菜单ID
     * @return 子菜单列表
     */
    List<MenuDTO> getChildren(Long id);

    /**
     * 更新菜单排序
     *
     * @param id 菜单ID
     * @param sort 排序值
     */
    void updateMenuSort(Long id, Integer sort);

    /**
     * 移动菜单（调整父子关系）
     *
     * @param id 菜单ID
     * @param parentId 新的父ID
     * @param sort 新的排序值
     */
    void moveMenu(Long id, Long parentId, Integer sort);

    /**
     * 根据用户ID获取菜单树（用于权限控制）
     *
     * @param userId 用户ID
     * @return 用户菜单树
     */
    List<MenuDTO> getUserMenus(Long userId);

    /**
     * 获取所有菜单路径（用于权限验证）
     *
     * @return 菜单路径列表
     */
    List<String> getAllMenuPaths();

    /**
     * 检查菜单名称是否已存在
     *
     * @param menuName 菜单名称
     * @param parentId 父ID
     * @param excludeId 排除的菜单ID（用于更新时检查）
     * @return 是否存在
     */
    boolean checkMenuName(String menuName, Long parentId, Long excludeId);
}
