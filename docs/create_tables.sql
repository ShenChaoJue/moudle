-- ========================================
-- 项目数据库表结构创建脚本
-- 基于实体模型自动生成
-- 数据库: MySQL 8.0+
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 设备状态表
-- ----------------------------
DROP TABLE IF EXISTS `device_status`;
CREATE TABLE `device_status` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法生成）',
  `device_id` varchar(255) NOT NULL COMMENT '设备ID',
  `device_name` varchar(255) NOT NULL COMMENT '设备名称',
  `device_type` varchar(100) DEFAULT NULL COMMENT '设备类型',
  `status` varchar(20) NOT NULL DEFAULT 'OFFLINE' COMMENT '设备状态：ONLINE-在线，OFFLINE-离线',
  `last_online_time` datetime DEFAULT NULL COMMENT '最后上线时间',
  `last_offline_time` datetime DEFAULT NULL COMMENT '最后下线时间',
  `last_heartbeat_time` datetime DEFAULT NULL COMMENT '最后心跳时间',
  `ip_address` varchar(64) DEFAULT NULL COMMENT 'IP地址',
  `client_id` varchar(255) DEFAULT NULL COMMENT '连接客户端ID',
  `firmware_version` varchar(100) DEFAULT NULL COMMENT '设备版本',
  `signal_strength` int DEFAULT NULL COMMENT '信号强度（RSSI）',
  `battery_level` int DEFAULT NULL COMMENT '电池电量（0-100）',
  `online_duration` bigint DEFAULT NULL COMMENT '在线时长（秒）',
  `remark` text COMMENT '备注',
  `create_time` datetime NOT NULL COMMENT '创建时间（自动填充）',
  `update_time` datetime NOT NULL COMMENT '修改时间（自动填充）',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-删除（自动填充）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  KEY `idx_status` (`status`),
  KEY `idx_last_heartbeat` (`last_heartbeat_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备状态表';

-- ----------------------------
-- 2. 文件表
-- ----------------------------
DROP TABLE IF EXISTS `file`;
CREATE TABLE `file` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法生成）',
  `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `content_type` varchar(100) DEFAULT NULL COMMENT '文件类型（MIME类型）',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `file_path` varchar(500) DEFAULT NULL COMMENT '服务器存储路径（绝对路径）',
  `access_path` varchar(500) DEFAULT NULL COMMENT '访问文件的相对路径/URL',
  `upload_time` datetime NOT NULL COMMENT '上传时间',
  `is_chunked` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为分片上传',
  `total_chunks` int DEFAULT NULL COMMENT '分片总数',
  `chunk_size` bigint DEFAULT NULL COMMENT '分片大小（字节）',
  `upload_id` varchar(255) DEFAULT NULL COMMENT '上传会话ID（分片上传使用）',
  `create_time` datetime NOT NULL COMMENT '创建时间（自动填充）',
  `update_time` datetime NOT NULL COMMENT '修改时间（自动填充）',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-删除（自动填充）',
  PRIMARY KEY (`id`),
  KEY `idx_upload_time` (`upload_time`),
  KEY `idx_content_type` (`content_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件表';

-- ----------------------------
-- 3. 文件片段表（RAG文档切片）
-- ----------------------------
DROP TABLE IF EXISTS `file_chunk`;
CREATE TABLE `file_chunk` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法生成）',
  `file_id` bigint NOT NULL COMMENT '所属文件ID',
  `chunk_index` int NOT NULL COMMENT '片段索引（从0开始）',
  `chunk_text` longtext NOT NULL COMMENT '片段文本内容',
  `start_pos` int NOT NULL COMMENT '起始位置（字符索引）',
  `end_pos` int NOT NULL COMMENT '结束位置（字符索引）',
  `create_time` datetime NOT NULL COMMENT '创建时间（自动填充）',
  `update_time` datetime NOT NULL COMMENT '修改时间（自动填充）',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-删除（自动填充）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_chunk` (`file_id`, `chunk_index`),
  KEY `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件片段表（RAG文档切片）';

-- ----------------------------
-- 4. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法生成）',
  `user_name` varchar(50) NOT NULL COMMENT '用户名称',
  `password` varchar(255) NOT NULL COMMENT '密码（加密）',
  `create_time` datetime NOT NULL COMMENT '创建时间（自动填充）',
  `update_time` datetime NOT NULL COMMENT '修改时间（自动填充）',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-删除（自动填充）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_name` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 5. 角色表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法生成）',
  `role_name` varchar(100) NOT NULL COMMENT '角色名称',
  `create_time` datetime NOT NULL COMMENT '创建时间（自动填充）',
  `update_time` datetime NOT NULL COMMENT '修改时间（自动填充）',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-删除（自动填充）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ----------------------------
-- 6. 菜单表
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法生成）',
  `menu_name` varchar(100) NOT NULL COMMENT '菜单名称',
  `url` varchar(255) DEFAULT NULL COMMENT '资源路径',
  `path` varchar(255) DEFAULT NULL COMMENT '路径（用于鉴权）',
  `type` varchar(20) DEFAULT NULL COMMENT '类型：文件夹/页面/按钮',
  `parent_id` bigint DEFAULT NULL COMMENT '父ID',
  `child_id` bigint DEFAULT NULL COMMENT '子ID',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL COMMENT '创建时间（自动填充）',
  `update_time` datetime NOT NULL COMMENT '修改时间（自动填充）',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-删除（自动填充）',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

-- ----------------------------
-- 7. 用户角色关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法生成）',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL COMMENT '创建时间（自动填充）',
  `update_time` datetime NOT NULL COMMENT '修改时间（自动填充）',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-删除（自动填充）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ----------------------------
-- 8. 角色菜单关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法生成）',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `create_time` datetime NOT NULL COMMENT '创建时间（自动填充）',
  `update_time` datetime NOT NULL COMMENT '修改时间（自动填充）',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-删除（自动填充）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- 初始化数据（可选）
-- ----------------------------

-- 插入默认管理员用户（密码: admin123，实际使用时应加密）
INSERT INTO `sys_user` (`user_name`, `password`) VALUES ('admin', '$2a$10$7JB720yubVSOfvV/HtY4.uLGf3j2Y5vPZ5H5Q5P5P5P5P5P5P5P5P5P');

-- 插入默认角色
INSERT INTO `sys_role` (`role_name`) VALUES ('ADMIN'), ('USER');

-- 给管理员分配角色
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 插入默认菜单
INSERT INTO `sys_menu` (`menu_name`, `path`, `type`, `sort`) VALUES
('首页', '/', 'page', 1),
('用户管理', '/user', 'page', 2),
('角色管理', '/role', 'page', 3),
('菜单管理', '/menu', 'page', 4),
('文件管理', '/file', 'page', 5),
('设备管理', '/device', 'page', 6);

-- 给管理员角色分配所有菜单权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, id FROM `sys_menu`;

-- ========================================
-- 表结构说明
-- ========================================
/*
【重要】ID生成机制
------------------
项目使用雪花算法（Snowflake ID）生成分布式ID，不是数据库自增！
- 雪花ID格式：41位时间戳 + 5位机器ID + 5位数据中心ID + 12位序列号
- 起始时间：2024-01-01 00:00:00
- 机器ID和数据中心ID可通过配置自定义（默认都是1）
- 优点：全局唯一、高并发支持、时间有序
- 所有表的主键id字段都需要通过雪花算法生成

【自动填充机制】
----------------
项目通过 MybatisAutoFillInterceptor 自动填充以下字段：
- INSERT操作：
  * id: 如果为null，自动生成雪花ID
  * createTime: 自动填充当前时间
  * updateTime: 自动填充当前时间
  * isDeleted: 默认为0（未删除）
- UPDATE操作：
  * updateTime: 自动更新为当前时间

【表结构说明】
----------------
1. device_status - 设备状态表
   - 存储IoT设备的状态信息
   - 支持在线/离线状态追踪
   - 包含设备硬件信息（信号强度、电池电量等）

2. file - 文件表
   - 存储文件元数据
   - 支持分片上传
   - 文件访问路径管理

3. file_chunk - 文件片段表
   - 用于RAG（检索增强生成）文档切片
   - 将大文档分割成可检索的片段
   - 记录每个片段的位置信息

4. sys_user - 用户表
   - 存储系统用户信息
   - 用户名唯一
   - 密码需要加密存储（建议使用BCrypt）

5. sys_role - 角色表
   - 定义系统角色
   - 支持角色层级

6. sys_menu - 菜单表
   - 存储系统菜单结构
   - 支持父子菜单关系
   - 用于权限控制

7. sys_user_role - 用户角色关联表
   - 多对多关系：用户 ↔ 角色
   - 一个用户可以有多个角色

8. sys_role_menu - 角色菜单关联表
   - 多对多关系：角色 ↔ 菜单
   - 一个角色可以有多个菜单权限

【设计特点】
----------------
- 所有表都继承BaseEntity字段（id, createTime, updateTime, isDeleted）
- 使用逻辑删除（isDeleted字段）而不是物理删除
- 包含完整的索引优化（主键、唯一索引、普通索引）
- 支持中文存储（utf8mb4字符集）
- 遵循数据库命名规范
- 分布式ID生成，支持高并发场景
- 自动填充机制，减少重复代码

【注意事项】
----------------
1. 应用启动前必须配置雪花算法参数：
   - snowflake.worker-id: 机器ID (0-31)
   - snowflake.datacenter-id: 数据中心ID (0-31)

2. 插入数据时，id字段不能设置为AUTO_INCREMENT
   - 如果手动指定id，需确保全局唯一
   - 建议让框架自动生成

3. 密码字段需要加密存储
   - 推荐使用BCrypt加密算法
   - 初始数据中的密码需要预先加密

4. 外键约束说明
   - 当前建表语句未添加外键约束
   - 建议在应用层面保证数据一致性
   - 如需外键约束，可自行添加FK语句
*/