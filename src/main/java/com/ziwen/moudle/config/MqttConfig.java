package com.ziwen.moudle.config;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

import com.ziwen.moudle.listener.MqttMessageListener;

import org.springframework.messaging.MessageHandler;

@Configuration
@EnableIntegration
public class MqttConfig {

    @Autowired
    private MqttMessageListener mqttMessageListener;

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Value("${mqtt.connection-timeout}")
    private int connectionTimeout;

    @Value("${mqtt.keep-alive-interval}")
    private int keepAliveInterval;

    @Value("${mqtt.qos}")
    private int qos;

    @Value("${mqtt.retain}")
    private boolean retain;

    @Value("${mqtt.subscribe-topics}")
    private String[] subscribeTopics;

    /**
     * MQTT连接参数配置
     */
    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        // 设置MQTT服务器地址
        options.setServerURIs(brokerUrl.split(","));
        // 设置认证信息
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        // 连接超时
        options.setConnectionTimeout(connectionTimeout);
        // 心跳间隔
        options.setKeepAliveInterval(keepAliveInterval);
        // 断线重连
        options.setAutomaticReconnect(true);
        // 清理会话（false表示保留会话，接收离线消息）
        options.setCleanSession(false);

        // ==================== 配置 Last Will Testament (LWT) ====================
        // 设置遗嘱消息：设备意外断开时，broker 会自动发布此消息
        // 格式：device/{deviceId}/disconnect
        options.setWill("device/" + clientId + "/disconnect", "设备意外断开".getBytes(), qos, false);

        // 注意：遗嘱消息只会在以下情况触发：
        // 1. 设备异常断开（网络中断、程序崩溃等）
        // 2. 不会在正常调用 disconnect() 方法时触发

        return options;
    }

    /**
     * MQTT客户端工厂
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(mqttConnectOptions());
        return factory;
    }

    // ====================== 消息消费者（订阅设备上报） ======================
    /**
     * 订阅消息通道
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT消息驱动适配器（订阅主题）
     */
    @Bean
    public MessageProducer inbound(@Qualifier("mqttInputChannel") MessageChannel inputChannel) {
        // 初始化适配器，指定客户端ID、工厂、订阅主题
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId + "-in", mqttClientFactory(), subscribeTopics);
        // 设置消息转换器（指定默认QoS）
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(qos);
        // 指定消息接收后的通道
        adapter.setOutputChannel(inputChannel);
        return adapter;
    }

    /**
     * 将消息监听器绑定到消息通道
     */
    @Bean
    @org.springframework.integration.annotation.ServiceActivator(inputChannel = "mqttInputChannel")
    public org.springframework.messaging.MessageHandler mqttMessageHandler() {
        return mqttMessageListener;
    }

    // ====================== 消息生产者（发布控制指令） ======================
    /**
     * 发布消息通道
     */
    @Bean
    public MessageChannel mqttOutputChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT消息处理器（发布控制指令到设备）
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttOutputChannel")
    public MessageHandler mqttOutputHandler() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(clientId + "-out", mqttClientFactory());
        handler.setAsync(true); // 异步发布
        handler.setDefaultQos(qos);
        handler.setDefaultRetained(retain);
        return handler;
    }
}
