package com.ziwen.moudle.service.impl;

import com.ziwen.moudle.entity.DeviceStatus;
import com.ziwen.moudle.service.DeviceStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备离线检测定时任务
 * 通过心跳超时检测设备是否离线
 */
@Component
public class DeviceOfflineDetectionTask {

    @Autowired
    private DeviceStatusService deviceStatusService;

    // 心跳超时时间（秒）- 超过此时间未收到心跳视为离线
    private static final int HEARTBEAT_TIMEOUT = 300;  // 延长到5分钟（非常宽松）

    /**
     * 每60秒检查一次设备在线状态
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    public void checkDeviceOffline() {
        System.out.println("开始检查设备离线状态...");

        List<DeviceStatus> allDevices = deviceStatusService.getAllDeviceStatus();
        LocalDateTime now = LocalDateTime.now();

        for (DeviceStatus device : allDevices) {
            if ("ONLINE".equals(device.getStatus())) {
                LocalDateTime lastHeartbeat = device.getLastHeartbeatTime();

                // 如果没有心跳记录，使用最后上线时间
                if (lastHeartbeat == null) {
                    lastHeartbeat = device.getLastOnlineTime();
                }

                if (lastHeartbeat != null) {
                    long timeoutSeconds = java.time.Duration.between(lastHeartbeat, now).getSeconds();

                    // TODO: 临时禁用心跳超时检测，避免误判
                    // TODO: 想重新启用的话，将 > HEARTBEAT_TIMEOUT 改为 > 60
                    boolean shouldOffline = timeoutSeconds > 999999; // 永远不超时

                    if (shouldOffline) {
                        System.out.println("【离线检测】设备 " + device.getDeviceId() + " 心跳超时(" + timeoutSeconds + "秒), 标记为离线");
                        deviceStatusService.deviceOffline(device.getDeviceId(),
                            "心跳超时(" + timeoutSeconds + "秒未收到心跳)");
                    } else {
                        // 调试信息：显示设备心跳情况但不标记离线
                        System.out.println("【心跳检测】设备 " + device.getDeviceId() +
                            " 最后心跳: " + timeoutSeconds + "秒前 (未超时)");
                    }
                }
            }
        }

        System.out.println("设备离线检查完成");
    }
}
