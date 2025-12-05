package com.ziwen.moudle.service.impl.rbac;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.ziwen.moudle.dto.rbac.MenuDTO;
import com.ziwen.moudle.entity.rbac.Menu;
import com.ziwen.moudle.mapper.rbac.MenuMapper;
import com.ziwen.moudle.mapper.rbac.RoleMenuMapper;
import com.ziwen.moudle.service.rbac.MenuService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 *
 * @author boot
 */
@Slf4j
@Service
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;
    public MenuServiceImpl(MenuMapper menuMapper, RoleMenuMapper roleMenuMapper) {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
    }
    @Override
    public List<MenuDTO> getMenuTree(String menuName, String type) {
        // 1. 查询所有菜单（根据条件过滤）
        List<Menu> allMenus = menuMapper.selectAllMenus();

        // 2. 过滤菜单
        List<Menu> filteredMenus = allMenus.stream()
                .filter(menu -> {
                    if (menuName != null && !menu.getMenuName().contains(menuName)) {
                        return false;
                    }
                    if (type != null && !type.equals(menu.getType())) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // 3. 构建树形结构
        return buildMenuTree(filteredMenus, null);
    }

    @Override
    public List<MenuDTO> listMenus(String menuName, String type) {
        List<Menu> menus = menuMapper.selectMenuList(menuName, type);
        return menus.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public MenuDTO getMenu(Long id) {
        Menu menu = menuMapper.selectById(id);
        if (menu == null) {
            log.warn("菜单不存在: {}", id);
            return null;
        }
        MenuDTO dto = convertToDto(menu);
        // 查询关联的角色
        List<Long> roleIds = menuMapper.selectMenuRoleIds(id);
        dto.setRoleIds(roleIds);
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MenuDTO createMenu(MenuDTO menuDTO) {
        // 1. 参数校验
        if (menuDTO == null || menuDTO.getMenuName() == null) {
            throw new IllegalArgumentException("菜单名称不能为空");
        }

        // 2. 检查菜单名是否已存在（同父级下）
        if (checkMenuName(menuDTO.getMenuName(), menuDTO.getParentId(), null)) {
            throw new IllegalArgumentException("菜单名已存在: " + menuDTO.getMenuName());
        }

        // 3. 保存到数据库
        Menu menu = convertToEntity(menuDTO);
        menuMapper.insert(menu);

        // 4. 返回菜单信息
        return getMenu(menu.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MenuDTO updateMenu(Long id, MenuDTO menuDTO) {
        // 1. 校验菜单是否存在
        Menu existMenu = menuMapper.selectById(id);
        if (existMenu == null) {
            log.warn("菜单不存在: {}", id);
            throw new IllegalArgumentException("菜单不存在");
        }

        // 2. 检查菜单名是否重复（同父级下）
        if (checkMenuName(menuDTO.getMenuName(), menuDTO.getParentId(), id)) {
            throw new IllegalArgumentException("菜单名已存在: " + menuDTO.getMenuName());
        }

        // 3. 更新菜单信息
        Menu menu = convertToEntity(menuDTO);
        menu.setId(id);
        menuMapper.updateById(menu);

        // 4. 返回菜单信息
        return getMenu(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        // 1. 校验菜单是否存在
        Menu menu = menuMapper.selectById(id);
        if (menu == null) {
            log.warn("菜单不存在: {}", id);
            throw new IllegalArgumentException("菜单不存在");
        }

        // 2. 检查是否有子菜单
        List<Menu> children = menuMapper.selectChildren(id);
        if (!CollectionUtils.isEmpty(children)) {
            throw new IllegalArgumentException("存在子菜单，无法删除");
        }

        // 3. 软删除菜单（is_deleted = 1）
        Menu updateMenu = new Menu();
        updateMenu.setId(id);
        updateMenu.setIsDeleted(1);
        menuMapper.updateById(updateMenu);

        // 4. 软删除角色菜单关联
        roleMenuMapper.deleteMenuRoles(id);

        log.info("菜单删除成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteMenus(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        // 1. 批量软删除菜单
        ids.forEach(id -> {
            Menu updateMenu = new Menu();
            updateMenu.setId(id);
            updateMenu.setIsDeleted(1);
            menuMapper.updateById(updateMenu);
        });

        // 2. 批量软删除角色菜单关联
        roleMenuMapper.deleteMenuRolesBatch(ids);
    }

    @Override
    public List<MenuDTO> getChildren(Long id) {
        List<Menu> children = menuMapper.selectChildren(id);
        return children.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenuSort(Long id, Integer sort) {
        // 1. 校验菜单是否存在
        Menu menu = menuMapper.selectById(id);
        if (menu == null) {
            log.warn("菜单不存在: {}", id);
            throw new IllegalArgumentException("菜单不存在");
        }

        // 2. 更新排序字段
        Menu updateMenu = new Menu();
        updateMenu.setId(id);
        updateMenu.setSort(sort);
        menuMapper.updateById(updateMenu);

        log.info("菜单排序更新成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveMenu(Long id, Long parentId, Integer sort) {
        // 1. 校验菜单是否存在
        Menu menu = menuMapper.selectById(id);
        if (menu == null) {
            log.warn("菜单不存在: {}", id);
            throw new IllegalArgumentException("菜单不存在");
        }

        // 2. 校验目标父菜单是否存在
        if (parentId != null) {
            Menu parentMenu = menuMapper.selectById(parentId);
            if (parentMenu == null) {
                throw new IllegalArgumentException("父菜单不存在");
            }
        }

        // 3. 更新父ID和排序
        Menu updateMenu = new Menu();
        updateMenu.setId(id);
        updateMenu.setParentId(parentId);
        updateMenu.setSort(sort);
        menuMapper.updateById(updateMenu);

        log.info("菜单移动成功: {}", id);
    }

    @Override
    public List<MenuDTO> getUserMenus(Long userId) {
        // 1. 查询用户关联的角色
        // 2. 查询角色关联的菜单
        // 3. 构建菜单树
        List<Menu> menus = menuMapper.selectMenusByUserId(userId);
        return buildMenuTree(menus, null);
    }

    @Override
    public List<String> getAllMenuPaths() {
        return menuMapper.selectAllMenuPaths();
    }

    @Override
    public boolean checkMenuName(String menuName, Long parentId, Long excludeId) {
        int count = menuMapper.checkMenuNameExists(menuName, parentId, excludeId);
        return count > 0;
    }

    /**
     * 构建菜单树
     */
    private List<MenuDTO> buildMenuTree(List<Menu> menus, Long parentId) {
        List<MenuDTO> tree = new ArrayList<>();

        for (Menu menu : menus) {
            if (menu.getParentId() == null ? parentId == null : menu.getParentId().equals(parentId)) {
                MenuDTO menuDTO = convertToDto(menu);
                // 递归构建子菜单
                List<MenuDTO> children = buildMenuTree(menus, menu.getId());
                menuDTO.setChildren(children);
                tree.add(menuDTO);
            }
        }

        return tree;
    }

    /**
     * 转换Menu为MenuDTO
     */
    private MenuDTO convertToDto(Menu menu) {
        if (menu == null) {
            return null;
        }
        MenuDTO dto = new MenuDTO();
        BeanUtils.copyProperties(menu, dto);
        return dto;
    }

    /**
     * 转换MenuDTO为Menu
     */
    private Menu convertToEntity(MenuDTO dto) {
        if (dto == null) {
            return null;
        }
        Menu menu = new Menu();
        BeanUtils.copyProperties(dto, menu);
        return menu;
    }
}
