package com.ziwen.moudle.config;

import io.moquette.broker.Server;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.MemoryConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Properties;

/**
 * 嵌入式MQTT Broker配置
 * 实现"Java程序 + 内置服务器 + 设备"的一体化控制
 *
 * 功能：
 * 1. 启动时自动启动MQTT服务器
 * 2. 提供完整的MQTT协议支持
 * 3. 支持设备连接、发布、订阅
 * 4. 与应用共享内存，实现一体化控制
 */
@Configuration
public class EmbeddedMqttBrokerConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedMqttBrokerConfig.class);

    private Server mqttBroker;
    private boolean isRunning = false;

    /**
     * 启动嵌入式MQTT Broker
     */
    @PostConstruct
    public void startEmbeddedMqttBroker() {
        try {
            log.info("正在启动嵌入式MQTT Broker...");

            mqttBroker = new Server();

            // 配置属性
            Properties props = new Properties();
            props.setProperty("port", "1883");                     // MQTT TCP端口
            props.setProperty("host", "0.0.0.0");                  // 监听所有地址
            props.setProperty("allow_anonymous", "true");         // 允许匿名连接
            props.setProperty("persistent_store", "false");       // 不使用持久化存储（内存模式）
            props.setProperty("websocket_port", "8083");          // WebSocket端口
            props.setProperty("max_bytes_in_message", "65536");   // 最大消息大小
            props.setProperty("keep_alive", "60");                // 保活时间
            props.setProperty("clean_session", "true");           // 清理会话

            IConfig config = new MemoryConfig(props);

            // 启动Broker
            mqttBroker.startServer(config);
            isRunning = true;

            log.info("================================================");
            log.info("✅ 嵌入式MQTT Broker启动成功！");
            log.info("🔌 TCP连接地址: tcp://127.0.0.1:1883");
            log.info("🌐 WebSocket地址: ws://127.0.0.1:8083");
            log.info("👤 认证方式: 匿名访问（允许所有连接）");
            log.info("💾 存储模式: 内存存储");
            log.info("================================================");

            // 打印使用指南
            printUsageGuide();

        } catch (IOException e) {
            log.error("启动嵌入式MQTT Broker失败", e);
            throw new RuntimeException("无法启动MQTT Broker: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("启动嵌入式MQTT Broker时发生未知错误", e);
            throw new RuntimeException("MQTT Broker启动异常: " + e.getMessage(), e);
        }
    }

    /**
     * 停止嵌入式MQTT Broker
     */
    @PreDestroy
    public void stopEmbeddedMqttBroker() {
        if (mqttBroker != null && isRunning) {
            try {
                mqttBroker.stopServer();
                isRunning = false;
                log.info("嵌入式MQTT Broker已停止");
            } catch (Exception e) {
                log.error("停止MQTT Broker时出错", e);
            }
        }
    }

    /**
     * 检查Broker是否运行
     */
    public boolean isBrokerRunning() {
        return isRunning;
    }

    /**
     * 获取Broker实例（用于高级操作）
     */
    public Server getMqttBroker() {
        return mqttBroker;
    }

    /**
     * 打印使用指南
     */
    private void printUsageGuide() {
        log.info("");
        log.info("📋 一体化控制使用指南：");
        log.info("1. 设备连接配置：");
        log.info("   - 地址: 127.0.0.1:1883");
        log.info("   - 协议: MQTT v3.1.1");
        log.info("   - 认证: 无需认证");

        log.info("");
        log.info("2. 主题规范：");
        log.info("   - 设备上报: device/{设备ID}/report");
        log.info("   - 设备控制: device/{设备ID}/control");
        log.info("   - 设备上线: device/{设备ID}/connect");
        log.info("   - 设备下线: device/{设备ID}/disconnect");
        log.info("   - 设备心跳: device/{设备ID}/heartbeat");

        log.info("");
        log.info("3. 测试命令：");
        log.info("   # 使用mosquitto_pub测试（需安装Mosquitto）");
        log.info("   mosquitto_pub -h 127.0.0.1 -p 1883 -t \"device/test001/connect\" -m \"上线测试\"");
        log.info("   mosquitto_pub -h 127.0.0.1 -p 1883 -t \"device/test001/report\" -m \"温度=25.5\"");

        log.info("");
        log.info("4. API接口：");
        log.info("   - 控制设备: GET /mqtt/device/control/{deviceId}?command=指令");
        log.info("   - 查看状态: GET /mqtt/device/status/all");
        log.info("   - 统计信息: GET /mqtt/device/status/statistics");

        log.info("");
        log.info("5. 一体化优势：");
        log.info("   ✅ 无需安装外部MQTT服务器");
        log.info("   ✅ 应用与MQTT服务器共享内存，延迟低");
        log.info("   ✅ 统一配置管理");
        log.info("   ✅ 便于部署和分发");
        log.info("");
    }

    /**
     * 获取Broker状态信息
     */
    public String getBrokerStatus() {
        if (!isRunning) {
            return "MQTT Broker未运行";
        }

        return String.format("""
            MQTT Broker运行状态：
            - 状态: 运行中
            - 端口: 1883 (TCP), 8083 (WebSocket)
            - 连接: 允许匿名
            - 存储: 内存模式
            - 启动时间: %s
            """, java.time.LocalDateTime.now());
    }
}