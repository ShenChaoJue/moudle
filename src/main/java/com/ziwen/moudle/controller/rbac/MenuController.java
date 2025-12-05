package com.ziwen.moudle.controller.rbac;

import com.ziwen.moudle.common.AjaxResult;
import com.ziwen.moudle.dto.rbac.MenuDTO;
import com.ziwen.moudle.service.rbac.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 菜单管理 REST API
 *
 * @author boot
 */
@Slf4j
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /**
     * 查询菜单列表（树形结构）
     */
    @GetMapping("/tree")
    public AjaxResult getMenuTree(
            @RequestParam(required = false) String menuName,
            @RequestParam(required = false) String type) {
        List<MenuDTO> menus = menuService.getMenuTree(menuName, type);
        return AjaxResult.success(menus);
    }

    /**
     * 查询平铺菜单列表
     */
    @GetMapping("/list")
    public AjaxResult list(
            @RequestParam(required = false) String menuName,
            @RequestParam(required = false) String type) {
        List<MenuDTO> menus = menuService.listMenus(menuName, type);
        return AjaxResult.success(menus);
    }

    /**
     * 获取菜单详情
     */
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return AjaxResult.error("菜单ID必须大于0");
        }

        MenuDTO menu = menuService.getMenu(id);
        if (menu == null) {
            return AjaxResult.error("菜单不存在: " + id);
        }
        return AjaxResult.success(menu);
    }

    /**
     * 创建菜单
     */
    @PostMapping
    public AjaxResult create(@RequestBody @Valid MenuDTO menuDTO) {
        if (menuDTO.getMenuName() == null || menuDTO.getMenuName().trim().isEmpty()) {
            return AjaxResult.error("菜单名称不能为空");
        }

        try {
            MenuDTO created = menuService.createMenu(menuDTO);
            return AjaxResult.success("菜单创建成功", created);
        } catch (IllegalArgumentException e) {
            log.warn("创建菜单失败: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 更新菜单
     */
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable Long id, @RequestBody @Valid MenuDTO menuDTO) {
        if (id == null || id <= 0) {
            return AjaxResult.error("菜单ID必须大于0");
        }

        if (menuDTO.getMenuName() == null || menuDTO.getMenuName().trim().isEmpty()) {
            return AjaxResult.error("菜单名称不能为空");
        }

        try {
            MenuDTO updated = menuService.updateMenu(id, menuDTO);
            return AjaxResult.success("菜单更新成功", updated);
        } catch (IllegalArgumentException e) {
            log.warn("更新菜单失败: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 删除菜单（软删除）
     */
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return AjaxResult.error("菜单ID必须大于0");
        }

        try {
            menuService.deleteMenu(id);
            return AjaxResult.success();
        } catch (IllegalArgumentException e) {
            log.warn("删除菜单失败: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除菜单异常", e);
            return AjaxResult.error("删除菜单失败");
        }
    }

    /**
     * 批量删除菜单
     */
    @DeleteMapping("/batch")
    public AjaxResult batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return AjaxResult.error("菜单ID列表不能为空");
        }

        try {
            menuService.batchDeleteMenus(ids);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("批量删除菜单异常", e);
            return AjaxResult.error("批量删除菜单失败");
        }
    }

    /**
     * 获取子菜单
     */
    @GetMapping("/{id}/children")
    public AjaxResult getChildren(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return AjaxResult.error("菜单ID必须大于0");
        }

        List<MenuDTO> children = menuService.getChildren(id);
        return AjaxResult.success(children);
    }

    /**
     * 更新菜单排序
     */
    @PutMapping("/{id}/sort")
    public AjaxResult updateSort(@PathVariable Long id, @RequestParam Integer sort) {
        if (id == null || id <= 0) {
            return AjaxResult.error("菜单ID必须大于0");
        }

        if (sort == null) {
            return AjaxResult.error("排序值不能为空");
        }

        try {
            menuService.updateMenuSort(id, sort);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("更新菜单排序异常", e);
            return AjaxResult.error("更新菜单排序失败");
        }
    }

    /**
     * 移动菜单（调整父子关系）
     */
    @PutMapping("/{id}/move")
    public AjaxResult moveMenu(@PathVariable Long id,
                                 @RequestParam Long parentId,
                                 @RequestParam Integer sort) {
        if (id == null || id <= 0) {
            return AjaxResult.error("菜单ID必须大于0");
        }

        if (parentId == null) {
            return AjaxResult.error("父ID不能为空");
        }

        if (sort == null) {
            return AjaxResult.error("排序值不能为空");
        }

        try {
            menuService.moveMenu(id, parentId, sort);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("移动菜单异常", e);
            return AjaxResult.error("移动菜单失败");
        }
    }

    /**
     * 根据用户ID获取菜单树（用于权限控制）
     */
    @GetMapping("/user/{userId}")
    public AjaxResult getUserMenus(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return AjaxResult.error("用户ID必须大于0");
        }

        List<MenuDTO> menus = menuService.getUserMenus(userId);
        return AjaxResult.success(menus);
    }

    /**
     * 获取所有菜单路径（用于权限验证）
     */
    @GetMapping("/paths")
    public AjaxResult getAllPaths() {
        List<String> paths = menuService.getAllMenuPaths();
        return AjaxResult.success(paths);
    }
}
