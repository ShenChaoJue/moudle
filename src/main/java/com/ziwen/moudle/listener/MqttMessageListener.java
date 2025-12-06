package com.ziwen.moudle.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import com.ziwen.moudle.service.mqtt.DeviceStatusService;

import java.time.LocalDateTime;

/**
 * MQTT消息监听处理器
 */
@Component
public class MqttMessageListener implements MessageHandler {

    @Autowired
    private DeviceStatusService deviceStatusService;

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        try {
            // 获取消息体
            String payload = message.getPayload().toString();
            // 获取消息主题
            String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
            // 获取QoS
            Integer qos = (Integer) message.getHeaders().get(MqttHeaders.RECEIVED_QOS);

            System.out.println("=== MQTT消息监听器接收消息 ===");
            System.out.println("时间: " + LocalDateTime.now());
            System.out.println("主题: " + topic);
            System.out.println("内容: " + payload);
            System.out.println("QoS: " + qos);

            // 解析主题格式：device/{deviceId}/{action}
            if (topic != null && topic.startsWith("device/")) {
                String[] topicParts = topic.split("/");
                if (topicParts.length >= 3) {
                    String deviceId = topicParts[1];
                    String action = topicParts[2];

                    System.out.println("解析结果 - 设备ID: " + deviceId + ", 操作: " + action);

                    // 根据action类型处理消息
                    switch (action) {
                        case "connect":
                            // 设备上线
                            System.out.println("【设备上线】处理开始...");
                            deviceStatusService.deviceOnline(deviceId, payload);
                            System.out.println("【设备上线】处理完成: " + deviceId);
                            break;
                        case "disconnect":
                            // 设备下线（LWT遗言）
                            System.out.println("【设备下线】处理开始...");
                            deviceStatusService.deviceOffline(deviceId, payload);
                            System.out.println("【设备下线】处理完成: " + deviceId);
                            break;
                        case "heartbeat":
                            // 设备心跳
                            System.out.println("【设备心跳】处理开始...");
                            deviceStatusService.deviceHeartbeat(deviceId, payload);
                            System.out.println("【设备心跳】处理完成: " + deviceId);
                            break;
                        case "report":
                            // 设备数据上报
                            System.out.println("【设备上报】处理开始...");
                            deviceStatusService.deviceReport(deviceId, payload);
                            System.out.println("【设备上报】处理完成: " + deviceId);
                            break;
                        case "status":
                            // 设备状态上报
                            System.out.println("【设备状态】处理开始...");
                            deviceStatusService.deviceReport(deviceId, payload);
                            System.out.println("【设备状态】处理完成: " + deviceId);
                            break;
                        case "response":
                            // 设备应答消息（对控制指令的响应）
                            System.out.println("【设备应答】处理开始...");
                            System.out.println("设备 " + deviceId + " 应答: " + payload);
                            // 这里可以添加应答处理逻辑，如：
                            // 1. 记录应答日志
                            // 2. 更新设备控制状态
                            // 3. 通知前端应答结果
                            System.out.println("【设备应答】处理完成: " + deviceId);
                            break;
                        default:
                            System.out.println("【未知类型】主题: " + topic + " | QoS: " + qos + " | 内容: " + payload);
                    }
                } else {
                    System.out.println("【格式错误】主题格式不正确: " + topic);
                }
            } else {
                System.out.println("【忽略消息】主题不符合device/*格式: " + topic);
            }
            System.out.println("=== MQTT消息处理结束 ===\n");
        } catch (Exception e) {
            System.err.println("MQTT消息处理异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
