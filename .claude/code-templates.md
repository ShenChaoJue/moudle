# 代码模板 - Spring Boot 多功能模块系统

## Controller 模板

### 基础 Controller
```java
package com.ziwen.moudle.controller.xxx;

import com.ziwen.moudle.common.AjaxResult;
import com.ziwen.moudle.dto.xxx.XxxDTO;
import com.ziwen.moudle.service.xxx.XxxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * [模块名称]控制器
 *
 * @author zi-wen
 * @version 1.0
 * @since 2024-xx-xx
 */
@Slf4j
@RestController
@RequestMapping("/api/xxx")
@RequiredArgsConstructor
@Tag(name = "[模块名称]", description = "[模块描述]")
public class XxxController {

    private final XxxService xxxService;

    /**
     * [功能描述]
     *
     * @param request [参数描述]
     * @return AjaxResult
     */
    @Operation(summary = "[功能名称]", description = "[功能描述]")
    @PostMapping("/save")
    public AjaxResult save(@RequestBody XxxDTO request) {
        try {
            // 参数校验
            if (request == null) {
                return AjaxResult.warn("参数不能为空");
            }

            // 调用业务服务
            xxxService.save(request);

            return AjaxResult.success("操作成功");
        } catch (Exception e) {
            log.error("保存失败", e);
            return AjaxResult.error("操作失败: " + e.getMessage());
        }
    }

    /**
     * [功能描述]
     *
     * @param id [参数描述]
     * @return AjaxResult
     */
    @Operation(summary = "[功能名称]", description = "[功能描述]")
    @GetMapping("/get/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        try {
            if (id == null || id <= 0) {
                return AjaxResult.warn("ID参数不正确");
            }

            XxxDTO result = xxxService.getById(id);
            return AjaxResult.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询失败", e);
            return AjaxResult.error("查询失败: " + e.getMessage());
        }
    }
}
```

### 分页查询 Controller
```java
/**
 * 分页查询
 *
 * @param request 查询参数
 * @return AjaxResult
 */
@Operation(summary = "分页查询", description = "分页查询列表")
@PostMapping("/list")
public AjaxResult list(@RequestBody XxxQueryDTO request) {
    try {
        if (request == null) {
            request = new XxxQueryDTO();
        }

        // 设置默认值
        if (request.getPageNum() == null || request.getPageNum() <= 0) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() <= 0) {
            request.setPageSize(10);
        }

        IPage<XxxDTO> result = xxxService.list(request);
        return AjaxResult.success("查询成功", result);
    } catch (Exception e) {
        log.error("分页查询失败", e);
        return AjaxResult.error("查询失败: " + e.getMessage());
    }
}
```

## Service 模板

### Service 接口
```java
package com.ziwen.moudle.service.xxx;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ziwen.moudle.dto.xxx.XxxDTO;
import com.ziwen.moudle.entity.xxx.Xxx;

/**
 * [模块名称]服务接口
 *
 * @author zi-wen
 * @version 1.0
 * @since 2024-xx-xx
 */
public interface XxxService extends IService<Xxx> {

    /**
     * 保存
     *
     * @param request 请求参数
     */
    void save(XxxDTO request);

    /**
     * 根据ID查询
     *
     * @param id ID
     * @return XxxDTO
     */
    XxxDTO getById(Long id);

    /**
     * 分页查询
     *
     * @param request 查询参数
     * @return IPage<XxxDTO>
     */
    IPage<XxxDTO> list(XxxQueryDTO request);

    /**
     * 更新
     *
     * @param request 请求参数
     */
    void update(XxxDTO request);

    /**
     * 删除
     *
     * @param id ID
     */
    void delete(Long id);
}
```

### Service 实现类
```java
package com.ziwen.moudle.service.impl.xxx;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.ziwen.moudle.dto.xxx.XxxDTO;
import com.ziwen.moudle.entity.xxx.Xxx;
import com.ziwen.moudle.mapper.xxx.XxxMapper;
import com.ziwen.moudle.service.xxx.XxxService;
import com.ziwen.moudle.common.BusinessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * [模块名称]服务实现类
 *
 * @author zi-wen
 * @version 1.0
 * @since 2024-xx-xx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XxxServiceImpl extends ServiceImpl<XxxMapper, Xxx> implements XxxService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(XxxDTO request) {
        try {
            // 业务校验
            validateSave(request);

            // 转换为实体
            Xxx entity = convertToEntity(request);

            // 设置创建信息
            entity.setCreateTime(LocalDateTime.now());

            // 保存
            save(entity);

            log.info("保存成功: {}", entity.getId());
        } catch (Exception e) {
            log.error("保存失败", e);
            throw new BusinessException("保存失败: " + e.getMessage());
        }
    }

    @Override
    public XxxDTO getById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("ID参数不正确");
        }

        Xxx entity = getById(id);
        if (entity == null) {
            throw new BusinessException("数据不存在");
        }

        return convertToDTO(entity);
    }

    @Override
    public IPage<XxxDTO> list(XxxQueryDTO request) {
        // 构建查询条件
        LambdaQueryWrapper<Xxx> wrapper = new LambdaQueryWrapper<>();

        // 添加查询条件
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(Xxx::getName, request.getKeyword());
        }

        // 排序
        wrapper.orderByDesc(Xxx::getCreateTime);

        // 分页查询
        Page<Xxx> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<Xxx> result = page(page, wrapper);

        // 转换为DTO
        return result.convert(this::convertToDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(XxxDTO request) {
        if (request.getId() == null || request.getId() <= 0) {
            throw new BusinessException("ID参数不正确");
        }

        try {
            // 业务校验
            validateUpdate(request);

            Xxx entity = convertToEntity(request);
            entity.setUpdateTime(LocalDateTime.now());

            updateById(entity);

            log.info("更新成功: {}", entity.getId());
        } catch (Exception e) {
            log.error("更新失败", e);
            throw new BusinessException("更新失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("ID参数不正确");
        }

        try {
            // 业务校验
            Xxx entity = getById(id);
            if (entity == null) {
                throw new BusinessException("数据不存在");
            }

            // 删除
            removeById(id);

            log.info("删除成功: {}", id);
        } catch (Exception e) {
            log.error("删除失败", e);
            throw new BusinessException("删除失败: " + e.getMessage());
        }
    }

    /**
     * 保存前校验
     */
    private void validateSave(XxxDTO request) {
        if (request == null) {
            throw new BusinessException("参数不能为空");
        }

        // 添加业务校验逻辑
        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException("名称不能为空");
        }
    }

    /**
     * 更新前校验
     */
    private void validateUpdate(XxxDTO request) {
        if (request == null) {
            throw new BusinessException("参数不能为空");
        }

        if (request.getId() == null || request.getId() <= 0) {
            throw new BusinessException("ID不能为空");
        }

        // 添加业务校验逻辑
    }

    /**
     * DTO转实体
     */
    private Xxx convertToEntity(XxxDTO dto) {
        // 使用BeanUtils或MapStruct
        // 这里是示例代码
        Xxx entity = new Xxx();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        // ... 其他字段映射
        return entity;
    }

    /**
     * 实体转DTO
     */
    private XxxDTO convertToDTO(Xxx entity) {
        XxxDTO dto = new XxxDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        // ... 其他字段映射
        return dto;
    }
}
```

## Mapper 模板

### Mapper 接口
```java
package com.ziwen.moudle.mapper.xxx;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ziwen.moudle.entity.xxx.Xxx;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * [模块名称]Mapper接口
 *
 * @author zi-wen
 * @version 1.0
 * @since 2024-xx-xx
 */
@Mapper
public interface XxxMapper extends BaseMapper<Xxx> {

    /**
     * 自定义查询方法
     *
     * @param param 参数
     * @return List<Xxx>
     */
    @Select("SELECT * FROM xxx_table WHERE status = #{status} AND create_time >= #{startTime}")
    List<Xxx> selectByCondition(@Param("status") Integer status, @Param("startTime") LocalDateTime startTime);

    /**
     * 自定义更新方法
     *
     * @param id ID
     * @param status 状态
     * @return 影响行数
     */
    @Update("UPDATE xxx_table SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
```

### Mapper XML 模板
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ziwen.moudle.mapper.xxx.XxxMapper">

    <resultMap id="BaseResultMap" type="com.ziwen.moudle.entity.xxx.Xxx">
        <id column="id" property="id" jdbcType="BIGINT"/>
        <result column="name" property="name" jdbcType="VARCHAR"/>
        <result column="status" property="status" jdbcType="INTEGER"/>
        <result column="create_time" property="createTime" jdbcType="TIMESTAMP"/>
        <result column="update_time" property="updateTime" jdbcType="TIMESTAMP"/>
    </resultMap>

    <sql id="Base_Column_List">
        id, name, status, create_time, update_time
    </sql>

    <!-- 自定义查询 -->
    <select id="selectByCondition" parameterType="map" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List"/>
        FROM xxx_table
        WHERE 1 = 1
        <if test="status != null">
            AND status = #{status}
        </if>
        <if test="startTime != null">
            AND create_time >= #{startTime}
        </if>
        ORDER BY create_time DESC
    </select>

    <!-- 自定义更新 -->
    <update id="updateStatus">
        UPDATE xxx_table
        SET status = #{status}, update_time = NOW()
        WHERE id = #{id}
    </update>

</mapper>
```

## DTO 模板

### 基础 DTO
```java
package com.ziwen.moudle.dto.xxx;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * [模块名称]数据传输对象
 *
 * @author zi-wen
 * @version 1.0
 * @since 2024-xx-xx
 */
@Data
public class XxxDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 名称
     */
    @NotBlank(message = "名称不能为空")
    private String name;

    /**
     * 状态
     */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
```

### 查询 DTO
```java
package com.ziwen.moudle.dto.xxx;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * [模块名称]查询数据传输对象
 *
 * @author zi-wen
 * @version 1.0
 * @since 2024-xx-xx
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class XxxQueryDTO extends XxxDTO {

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    /**
     * 关键词
     */
    private String keyword;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;
}
```

## 实体类模板

### BaseEntity
```java
package com.ziwen.moudle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体类
 *
 * @author zi-wen
 * @version 1.0
 * @since 2024-xx-xx
 */
@Data
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
```

### 具体实体类
```java
package com.ziwen.moudle.entity.xxx;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ziwen.moudle.entity.BaseEntity;

/**
 * [模块名称]实体类
 *
 * @author zi-wen
 * @version 1.0
 * @since 2024-xx-xx
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("xxx_table")
public class Xxx extends BaseEntity {

    /**
     * 名称
     */
    private String name;

    /**
     * 状态
     */
    private Integer status;
}
```

## 工具类模板

### 业务工具类
```java
package com.ziwen.moudle.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * [模块名称]工具类
 *
 * @author zi-wen
 * @version 1.0
 * @since 2024-xx-xx
 */
@Slf4j
@Component
public class XxxUtil {

    /**
     * 生成唯一标识
     *
     * @return 唯一标识
     */
    public static String generateUniqueId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 验证参数
     *
     * @param param 参数
     * @return 是否有效
     */
    public static boolean validateParam(String param) {
        return StringUtils.hasText(param) && param.trim().length() > 0;
    }
}
```

## 枚举类模板

### 状态枚举
```java
package com.ziwen.moudle.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * [模块名称]状态枚举
 *
 * @author zi-wen
 * @version 1.0
 * @since 2024-xx-xx
 */
@Getter
@AllArgsConstructor
public enum XxxStatusEnum {

    /**
     * 禁用
     */
    DISABLED(0, "禁用"),

    /**
     * 启用
     */
    ENABLED(1, "启用"),

    /**
     * 删除
     */
    DELETED(2, "已删除");

    private final Integer code;
    private final String desc;

    /**
     * 根据编码获取描述
     *
     * @param code 编码
     * @return 描述
     */
    public static String getDescByCode(Integer code) {
        for (XxxStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status.getDesc();
            }
        }
        return "未知";
    }
}
```

## 异常类模板

### 业务异常
```java
package com.ziwen.moudle.common;

/**
 * 业务异常类
 *
 * @author zi-wen
 * @version 1.0
 * @since 2024-xx-xx
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## 测试类模板

### Controller 测试
```java
package com.ziwen.moudle.controller.xxx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziwen.moudle.dto.xxx.XxxDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * [模块名称]控制器测试
 *
 * @author zi-wen
 * @version 1.0
 * @since 2024-xx-xx
 */
@WebMvcTest(XxxController.class)
class XxxControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private XxxService xxxService;

    private ObjectMapper objectMapper;
    private XxxDTO testDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        testDTO = new XxxDTO();
        testDTO.setName("测试名称");
        testDTO.setStatus(1);
    }

    @Test
    void save_Success() throws Exception {
        // 准备测试数据
        when(xxxService.save(any(XxxDTO.class))).thenAnswer(invocation -> {
            // 测试逻辑
            return null;
        });

        // 执行测试
        mockMvc.perform(post("/api/xxx/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"));
    }
}
```

### Service 测试
```java
package com.ziwen.moudle.service.impl.xxx;

import com.ziwen.moudle.dto.xxx.XxxDTO;
import com.ziwen.moudle.entity.xxx.Xxx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * [模块名称]服务测试
 *
 * @author zi-wen
 * @version 1.0
 * @since 2024-xx-xx
 */
class XxxServiceTest {

    @InjectMocks
    private XxxServiceImpl xxxService;

    @Mock
    private XxxMapper xxxMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_Success() {
        // 准备测试数据
        XxxDTO request = new XxxDTO();
        request.setName("测试名称");
        request.setStatus(1);

        Xxx entity = new Xxx();
        entity.setId(1L);
        entity.setName("测试名称");
        entity.setStatus(1);

        // 设置mock行为
        when(xxxMapper.insert(any(Xxx.class))).thenReturn(1);

        // 执行测试
        xxxService.save(request);

        // 验证结果
        verify(xxxMapper, times(1)).insert(any(Xxx.class));
    }
}
```

使用这些模板可以确保项目代码的一致性和规范性。所有代码都遵循了项目的架构模式和编码规范。