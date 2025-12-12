# AI编码代理配置报告

## 项目概述

**项目名称**: Spring Boot 多功能模块系统
**项目路径**: D:\code\java\moudle\moudle
**项目类型**: Java/Spring Boot 企业级应用
**Java版本**: 17
**框架版本**: Spring Boot 4.0.0
**构建工具**: Maven

## 核心模块

### 1. RBAC权限管理
- **功能**: 用户、角色、菜单权限控制
- **技术栈**: MyBatis + MySQL
- **文件位置**: `src/main/java/com/ziwen/moudle/controller/rbac/`

### 2. 支付服务
- **功能**: 微信支付、支付宝支付
- **技术栈**: weixin-java-pay、alipay-sdk-java
- **配置**: 沙盒环境 + 生产环境

### 3. 文件管理
- **功能**: 文件上传、下载、分片处理
- **技术栈**: 多线程上传、向量检索
- **AI集成**: Milvus 向量数据库

### 4. MQTT服务
- **功能**: 设备通信、状态监控
- **技术栈**: Eclipse Paho + Moquette Broker
- **特性**: 实时消息、设备管理

### 5. AI服务
- **功能**: 多模态搜索、文档问答
- **技术栈**: 通义千问 DashScope API
- **集成**: Milvus + 向量嵌入

### 6. 验证码服务
- **功能**: 图形验证码生成与验证
- **技术栈**: Kaptcha

## 已生成配置文件

### 1. Claude Code 配置
**文件**: `.claude/settings.json`

**主要功能**:
- ✅ 权限控制 (允许/禁止/询问操作)
- ✅ UI界面配置 (主题、代码高亮、行号显示)
- ✅ 上下文管理 (文件内容包含、自动刷新)
- ✅ 特性启用 (自动补全、代码分析、错误检测)
- ✅ 项目特定信息 (语言、框架、版本、模块)

**权限设置**:
- **允许**: Maven操作、文件读写、创建目录
- **禁止**: 危险删除命令、强制推送、环境文件写入
- **询问**: 重要配置修改、生产部署

### 2. 项目指令文件
**文件**: `.claude/project-instructions.md`

**包含内容**:
- 📋 项目概述和技术栈说明
- 📦 包结构规范
- 🎯 命名规范 (类、方法、字段)
- 🔄 响应格式统一 (AjaxResult)
- ⚠️ 异常处理机制
- 🏗️ 分层架构指导
- 📁 文件上传规范
- 💰 支付集成规范
- 📡 MQTT服务规范
- 🤖 AI服务规范
- 🧪 测试规范
- 🔒 安全规范
- ⚡ 性能优化建议

### 3. 代码模板
**文件**: `.claude/code-templates.md`

**模板类型**:
- Controller 模板 (基础、分页查询)
- Service 模板 (接口、实现类)
- Mapper 模板 (接口、XML)
- DTO 模板 (基础、查询)
- 实体类模板 (BaseEntity)
- 工具类模板
- 枚举类模板
- 异常类模板
- 测试类模板 (Controller、Service)

### 4. 编辑器配置
**文件**: `.editorconfig`

**配置内容**:
- ✅ 统一编码格式 (UTF-8)
- ✅ 行尾符设置 (CRLF)
- ✅ 缩进规则 (空格4个)
- ✅ 最大行长度 (120字符)
- ✅ 不同文件类型特定配置

### 5. 代码规范检查
**文件**: `checkstyle.xml`

**检查项目**:
- 📝 头部注释要求
- 🏷️ 命名规范检查
- 📐 代码格式检查
- 🔍 复杂度检查
- 🚫 常见错误检查
- 💬 注释规范检查

**质量标准**:
- 文件长度限制: 1200行
- 最大行长度: 120字符
- 嵌套层级限制: 3层
- 返回数量限制: 3个

### 6. SonarQube 配置
**文件**: `.claude/sonar-project.properties`

**分析维度**:
- 🧪 代码覆盖率 (目标: 80%+)
- 🔒 安全热点检查 (最低分数: 0.7)
- 🐛 代码缺陷检测
- 📊 代码复杂度分析
- 🔄 重复代码检查
- 📏 代码规范检查

### 7. Pre-commit Hook
**文件**: `.claude/pre-commit-hook.sh`

**检查项目**:
- ✅ Maven项目编译检查
- 🧪 单元测试运行
- 📋 代码格式检查
- 🔒 敏感信息检查
- 📝 TODO/FIXME 检查
- 📁 大文件检查
- 📊 注释覆盖率检查
- 🇨🇳 中文注释检查
- 📂 项目结构检查
- 📏 命名规范检查

### 8. Maven 配置扩展
**文件**: `.claude/maven-config.xml`

**包含插件**:
- 🔨 编译插件 (Java 17)
- 🧪 测试插件 (Surefire/Failsafe)
- 📊 代码覆盖率 (JaCoCo)
- 📋 代码规范 (Checkstyle)
- 🐛 静态分析 (SpotBugs)
- 📏 代码分析 (PMD)
- 🔍 代码审查 (SonarQube)
- 📦 依赖管理 (Dependency)
- 🚫 强制规则 (Enforcer)

## 配置使用指南

### 1. 安装 Claude Code 配置
```bash
# 复制配置到Claude Code目录
cp .claude/settings.json ~/.claude/settings.json

# 或者在项目中直接使用 (已配置)
```

### 2. 设置 Pre-commit Hook
```bash
# Linux/Mac
cp .claude/pre-commit-hook.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

# Windows (使用Git Bash)
cp .claude/pre-commit-hook.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

### 3. 集成 Maven 配置
```xml
<!-- 将 .claude/maven-config.xml 中的插件配置复制到 pom.xml 的 <build><plugins> 部分 -->
```

### 4. 运行代码质量检查
```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 代码格式检查
mvn checkstyle:check

# 静态分析
mvn spotbugs:check

# 代码覆盖率
mvn jacoco:report

# SonarQube分析
mvn sonar:sonar
```

## AI编码助手使用指导

### 编码前准备
1. **阅读项目指令**: `.claude/project-instructions.md`
2. **参考代码模板**: `.claude/code-templates.md`
3. **了解项目结构**: 按照包结构规范组织代码

### 编码过程要求
1. **遵循命名规范**: Controller/Service/DTO/Entity
2. **使用统一响应**: AjaxResult类
3. **添加中文注释**: 提高代码可读性
4. **处理异常**: 使用BusinessException
5. **事务管理**: Service层添加@Transactional

### 代码提交前检查
1. **运行测试**: 确保所有测试通过
2. **代码格式**: 通过Checkstyle检查
3. **静态分析**: 无SpotBugs警告
4. **覆盖率**: 达到80%以上
5. **敏感信息**: 检查配置文件安全

## 质量标准

### 代码质量指标
- **代码覆盖率**: ≥ 80%
- **代码复杂度**: < 10 (每个方法)
- **代码规范**: 100% 符合Checkstyle
- **安全热点**: 分数 ≥ 0.7
- **重复代码**: < 3%

### 文档要求
- **注释覆盖率**: ≥ 30%
- **API文档**: Swagger注解完整
- **中文注释**: 重要逻辑必须有
- **README**: 模块功能说明

### 性能标准
- **响应时间**: API < 500ms
- **数据库查询**: < 100ms
- **文件上传**: 支持分片、断点续传
- **内存使用**: JVM参数优化

## 风险提示

### ⚠️ 禁止操作
1. **修改支付核心逻辑**: 可能导致资金安全风险
2. **删除权限控制**: 影响系统安全
3. **修改数据库配置**: 可能导致连接失败
4. **修改MQTT参数**: 可能影响设备通信

### ⚠️ 敏感信息
1. **支付密钥**: 仅测试环境可明文
2. **数据库密码**: 生产环境必须加密
3. **API密钥**: 使用环境变量管理
4. **用户数据**: 遵循隐私保护法规

### ⚠️ 重要配置
1. **application.yml**: 数据库连接配置
2. **JVM参数**: 内存和性能调优
3. **文件路径**: 确保权限正确
4. **MQTT地址**: 确保网络可达

## 后续优化建议

### 短期优化 (1-2周)
1. **完善测试用例**: 提升代码覆盖率
2. **添加API文档**: Swagger注解补充
3. **性能优化**: 数据库查询优化
4. **日志完善**: 关键业务添加日志

### 中期优化 (1个月)
1. **安全加固**: 添加身份认证和授权
2. **缓存优化**: Redis缓存热点数据
3. **监控告警**: 添加系统监控和报警
4. **文档完善**: API文档和部署文档

### 长期优化 (3个月)
1. **微服务拆分**: 按业务模块拆分
2. **容器化部署**: Docker + Kubernetes
3. **CI/CD流水线**: 自动化测试和部署
4. **灰度发布**: 支持版本回滚

## 联系方式

**项目负责人**: zi-wen
**技术栈**: Spring Boot 4.0.0 + MySQL + MyBatis
**版本**: v1.0
**最后更新**: 2024-12-12

---

*此配置文件确保AI编码助手能够为项目生成高质量、符合规范的代码。建议定期更新配置以适应项目发展需要。*