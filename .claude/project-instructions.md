# 项目特定指令 - Spring Boot 多功能模块系统

## 项目概述
这是一个基于 Spring Boot 4.0.0 的多功能企业级应用，包含以下核心模块：
- **RBAC权限管理**: 用户、角色、菜单权限控制
- **支付服务**: 微信支付、支付宝支付集成
- **文件管理**: 文件上传、下载、分片处理、AI向量检索
- **MQTT服务**: 设备通信、状态监控、消息推送
- **AI服务**: 多模态搜索、向量数据库、文档问答
- **验证码服务**: 图形验证码生成与验证

## 技术栈
- **Java**: 17
- **框架**: Spring Boot 4.0.0
- **构建工具**: Maven
- **数据库**: MySQL 8.0
- **ORM**: MyBatis
- **向量数据库**: Milvus
- **AI服务**: 通义千问 DashScope API
- **消息队列**: MQTT (Eclipse Paho + Moquette Broker)

## 核心编码规范

### 1. 包结构规范
```
com.ziwen.moudle/
├── config/          # 配置类
├── controller/      # 控制器层
├── service/         # 业务逻辑层
│   ├── impl/        # 实现类
├── mapper/          # 数据访问层 (MyBatis)
├── entity/          # 实体类
├── dto/             # 数据传输对象
├── common/          # 通用类
├── constant/        # 常量类
├── enums/           # 枚举类
├── utils/           # 工具类
├── factory/         # 工厂类
├── listener/        # 监听器
└── MoudleApplication.java # 启动类
```

### 2. 命名规范

#### Java 类命名
- **控制器**: `XxxController.java` (如: `UserController.java`)
- **服务接口**: `XxxService.java` (如: `UserService.java`)
- **服务实现**: `XxxServiceImpl.java` (如: `UserServiceImpl.java`)
- **实体类**: `Xxx.java` (如: `User.java`)
- **DTO**: `XxxDTO.java` (如: `UserDTO.java`)
- **配置类**: `XxxConfig.java` (如: `MybatisConfig.java`)
- **工具类**: `XxxUtil.java` (如: `PasswordUtil.java`)

#### 方法命名
- **CRUD操作**: `save()`, `update()`, `deleteById()`, `getById()`, `list()`
- **业务方法**: 使用动词+名词的方式 (如: `checkUserPermission()`)
- **获取方法**: `getXxx()`, `findXxx()`, `queryXxx()`
- **判断方法**: `isXxx()`, `hasXxx()`, `checkXxx()`

#### 数据库字段命名
- 使用下划线命名法: `user_name`, `create_time`
- Java实体类使用驼峰命名法: `userName`, `createTime`

### 3. 响应格式规范

#### 统一响应类 AjaxResult
所有API响应必须使用 `AjaxResult` 类:
```java
// 成功响应
AjaxResult.success(); // 默认成功消息
AjaxResult.success("操作成功");
AjaxResult.success(data); // 带数据
AjaxResult.success("操作成功", data); // 带消息和数据

// 错误响应
AjaxResult.error("操作失败");
AjaxResult.error(500, "服务器内部错误");
AjaxResult.warn("参数不完整");
```

#### 响应数据结构
```json
{
  "code": 200,        // 状态码
  "msg": "操作成功",   // 消息
  "data": {           // 数据 (可选)
    // 具体数据
  }
}
```

### 4. 异常处理规范

#### 全局异常处理
- 使用 `@ControllerAdvice` 和 `@ExceptionHandler`
- 自定义业务异常 `BusinessException`
- 统一错误码管理 `ResultCode`

#### 异常处理示例
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public AjaxResult handleBusinessException(BusinessException e) {
        log.error("业务异常", e);
        return AjaxResult.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e) {
        log.error("系统异常", e);
        return AjaxResult.error("系统异常，请联系管理员");
    }
}
```

### 5. 业务逻辑分层

#### Controller 层
- 仅负责HTTP请求处理和响应
- 不包含业务逻辑
- 参数校验
- 调用Service层方法

#### Service 层
- 包含核心业务逻辑
- 处理事务管理
- 调用Mapper层和外部服务
- 返回DTO对象

#### Mapper 层
- 负责数据库操作
- 使用MyBatis注解或XML映射
- 不处理业务逻辑

### 6. 文件上传规范

#### 配置参数
```yaml
file:
  upload:
    path: d:/uploads/  # 文件存储路径
    access-path: /files/  # 访问虚拟路径
    allowed-types: pdf,doc,docx,jpg,png,mp4  # 允许的文件类型
    auto-chunk:
      threshold: 10485760  # 10MB 自动分片
    chunk:
      size: 1048576  # 1MB 分片大小
```

#### 文件命名规范
- 使用UUID生成唯一文件名
- 保留原始文件扩展名
- 按日期分目录存储

### 7. 支付集成规范

#### 微信支付
- 使用 `weixin-java-pay` 库
- 证书文件放在 `classpath:cert/` 目录
- 异步通知处理和验签

#### 支付宝支付
- 使用 `alipay-sdk-java` 库
- 沙盒环境测试和生产环境切换
- 统一支付工厂模式管理

### 8. MQTT 服务规范

#### 配置参数
```yaml
mqtt:
  broker-url: tcp://127.0.0.1:1883
  client-id: spring-boot-mqtt-client
  subscribe-topics: device/+/report,device/+/status
  qos: 1
  retain: false
```

#### 主题命名规范
- 设备上报: `device/{deviceId}/report`
- 设备状态: `device/{deviceId}/status`
- 设备控制: `device/{deviceId}/control`
- 心跳检测: `device/{deviceId}/heartbeat`

### 9. AI 服务规范

#### 向量数据库 (Milvus)
- 集合名称: `multimodal_search`
- 向量维度: 1024
- 使用 IVF_FLAT 索引

#### 嵌入模型
- 文本嵌入: `tongyi-embedding-vision-plus`
- 视觉语言模型: `qwen-vl-plus`

#### 文档处理流程
1. 文档上传 → 文本提取
2. 文本分块 → 向量化
3. 存储到 Milvus
4. 相似度检索
5. 生成回答

### 10. 测试规范

#### 单元测试
- Service层必须编写单元测试
- 使用 `@SpringBootTest` 和 `@Test`
- Mock外部依赖

#### 集成测试
- Controller层进行集成测试
- 测试数据库连接和事务
- 验证API响应格式

### 11. 安全规范

#### 敏感信息
- API密钥存储在配置文件或环境变量
- 支付证书文件加密存储
- 定期轮换密钥

#### SQL注入防护
- 使用预编译语句
- MyBatis参数绑定
- 避免SQL拼接

#### XSS防护
- 输入参数校验
- 输出内容转义
- 使用HttpOnly Cookie

### 12. 性能优化

#### 数据库优化
- 添加适当索引
- 使用分页查询
- 避免N+1查询问题

#### 缓存策略
- 使用Spring Cache
- Redis缓存热点数据
- 合理设置缓存过期时间

#### JVM调优
```bash
# 开发环境
-Xms256m -Xmx1024m -XX:+UseG1GC

# 生产环境
-Xms1024m -Xmx4096m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### 13. 部署规范

#### 环境配置
- 开发环境: `application-dev.yml`
- 测试环境: `application-test.yml`
- 生产环境: `application-prod.yml`

#### 启动脚本
- Windows: `startup.bat`
- Linux/Mac: `startup.sh`
- 包含JVM参数配置

## AI 编码助手指导原则

### 代码生成要求
1. **遵循项目架构**: 严格按照MVC分层架构
2. **使用统一响应**: 所有API返回AjaxResult对象
3. **添加详细注释**: 方法和复杂逻辑添加中文注释
4. **异常处理**: 每个方法都要有适当的异常处理
5. **参数校验**: Controller层进行参数校验
6. **事务管理**: Service层方法添加事务注解
7. **日志记录**: 关键业务操作添加日志

### 数据库操作规范
1. **Mapper接口**: 使用@Mapper注解
2. **SQL编写**: XML文件或注解方式
3. **结果映射**: 使用resultMap或@Results
4. **分页查询**: 使用PageHelper插件

### API设计规范
1. **RESTful风格**: GET查询、POST创建、PUT更新、DELETE删除
2. **路径命名**: `/api/module/action` (如: `/api/user/save`)
3. **参数传递**: GET使用@PathVariable，POST使用@RequestBody
4. **统一前缀**: 所有API添加`/api`前缀

### 日志规范
```java
// 使用SLF4J + Logback
private static final Logger logger = LoggerFactory.getLogger(ClassName.class);

// 日志级别
logger.debug("调试信息");
logger.info("业务信息: {}", data);
logger.warn("警告信息: {}", message);
logger.error("异常信息", exception);
```

### 文档要求
1. **Swagger注解**: Controller和DTO添加Swagger文档
2. **方法注释**: 重要方法添加Javadoc注释
3. **README**: 模块功能说明和部署指南

## 注意事项

⚠️ **禁止操作**:
- 不要修改支付相关核心逻辑
- 不要删除现有的权限控制
- 不要修改数据库连接配置
- 不要修改MQTT连接参数

⚠️ **敏感信息**:
- 支付密钥、证书文件
- 数据库连接密码
- API访问密钥
- 用户隐私数据

⚠️ **重要配置**:
- application.yml 中的数据库连接
- JVM内存参数设置
- 文件上传路径配置
- MQTT服务器地址

遵循以上规范，确保代码质量和系统稳定性。