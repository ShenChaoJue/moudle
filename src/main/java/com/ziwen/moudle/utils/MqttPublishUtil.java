package com.ziwen.moudle.utils;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Component
public class MqttPublishUtil {

    @Resource
    private MessageChannel mqttOutputChannel;

    /**
     * 发布MQTT消息（控制设备）
     * @param topic 发布主题（如device/123/control）
     * @param payload 消息内容（控制指令，如JSON字符串）
     * @param qos QoS级别（0/1/2）
     * @param retain 是否保留消息
     */
    public void publish(String topic, String payload, int qos, boolean retain) {
        mqttOutputChannel.send(MessageBuilder.withPayload(payload)
                .setHeader("mqtt_topic", topic)
                .setHeader("mqtt_qos", qos)
                .setHeader("mqtt_retained", retain)
                .build());
    }

    /**
     * 重载方法：使用默认QoS和retain
     */
    public void publish(String topic, String payload) {
        publish(topic, payload, 1, false);
    }
}