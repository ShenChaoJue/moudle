package com.ziwen.moudle.controller.mqtt;

import org.springframework.web.bind.annotation.*;
import com.ziwen.moudle.utils.MqttPublishUtil;
import com.ziwen.moudle.entity.DeviceStatus;
import com.ziwen.moudle.service.mqtt.DeviceStatusService;
import com.ziwen.moudle.common.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
/**
 * MQTT设备控制器
 * @author ziwen
 */
@RestController
@RequestMapping("/mqtt/device")
public class MqttDeviceController {

    @Autowired
    private MqttPublishUtil mqttPublishUtil;

    @Autowired
    private DeviceStatusService deviceStatusService;

    /**
     * 发送控制指令到指定设备
     * @param deviceId 设备ID
     * @param command 控制指令（如{"action":"open","param":1}）
     */
    @GetMapping("/control/{deviceId}")
    public AjaxResult controlDevice(@PathVariable String deviceId, @RequestParam String command) {
        // 构造控制主题（约定格式：device/{设备ID}/control）
        String topic = "device/" + deviceId + "/control";
        // 发布控制指令
        mqttPublishUtil.publish(topic, command);
        return AjaxResult.success("已发送控制指令到设备[" + deviceId + "]：" + command);
    }

    /**
     * 获取所有设备状态
     */
    @GetMapping("/status/all")
    public AjaxResult getAllDeviceStatus() {
        return AjaxResult.success(deviceStatusService.getAllDeviceStatus());
    }

    /**
     * 获取指定设备状态
     */
    @GetMapping("/status/{deviceId}")
    public AjaxResult getDeviceStatus(@PathVariable String deviceId) {
        DeviceStatus status = deviceStatusService.getDeviceStatus(deviceId);
        if (status == null) {
            return AjaxResult.error("设备不存在: " + deviceId);
        }
        return AjaxResult.success(status);
    }

    /**
     * 获取在线设备列表
     */
    @GetMapping("/status/online")
    public AjaxResult getOnlineDevices() {
        return AjaxResult.success(deviceStatusService.getOnlineDevices());
    }

    /**
     * 获取离线设备列表
     */
    @GetMapping("/status/offline")
    public AjaxResult getOfflineDevices() {
        return AjaxResult.success(deviceStatusService.getOfflineDevices());
    }

    /**
     * 获取设备统计信息
     */
    @GetMapping("/status/statistics")
    public AjaxResult getDeviceStatistics() {
        return AjaxResult.success(deviceStatusService.getDeviceStatistics());
    }

    /**
     * 更新设备信息
     */
    @PutMapping("/status/{deviceId}")
    public AjaxResult updateDeviceInfo(@PathVariable String deviceId, @RequestBody DeviceStatus deviceStatus) {
        if (!deviceId.equals(deviceStatus.getDeviceId())) {
            return AjaxResult.error("设备ID不匹配");
        }
        deviceStatusService.updateDeviceInfo(deviceStatus);
        return AjaxResult.success("设备信息更新成功: " + deviceId);
    }

    /**
     * 删除设备记录
     */
    @DeleteMapping("/status/{deviceId}")
    public AjaxResult deleteDevice(@PathVariable String deviceId) {
        deviceStatusService.deleteDevice(deviceId);
        return AjaxResult.success("设备记录删除成功: " + deviceId);
    }

    /**
     * 手动标记设备上线（测试用）
     */
    @PostMapping("/status/{deviceId}/online")
    public AjaxResult markDeviceOnline(@PathVariable String deviceId, @RequestParam(required = false) String payload) {
        deviceStatusService.deviceOnline(deviceId, payload != null ? payload : "manual_online");
        return AjaxResult.success("设备标记为上线: " + deviceId);
    }

    /**
     * 手动标记设备下线（测试用）
     */
    @PostMapping("/status/{deviceId}/offline")
    public AjaxResult markDeviceOffline(@PathVariable String deviceId, @RequestParam(required = false) String payload) {
        deviceStatusService.deviceOffline(deviceId, payload != null ? payload : "manual_offline");
        return AjaxResult.success("设备标记为下线: " + deviceId);
    }

    /**
     * 发送设备心跳（测试用）
     */
    @PostMapping("/status/{deviceId}/heartbeat")
    public AjaxResult sendHeartbeat(@PathVariable String deviceId, @RequestParam(required = false) String payload) {
        deviceStatusService.deviceHeartbeat(deviceId, payload != null ? payload : "manual_heartbeat");
        return AjaxResult.success("设备心跳已记录: " + deviceId);
    }

    /**
     * 调试接口：查看内存中所有设备状态详细信息
     */
    @GetMapping("/debug/device-status")
    public AjaxResult debugDeviceStatus() {
        List<DeviceStatus> allDevices = deviceStatusService.getAllDeviceStatus();
        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("totalDevices", allDevices.size());
        debugInfo.put("currentTime", LocalDateTime.now());
        debugInfo.put("heartbeatTimeout", "300秒（5分钟）");  // 显示超时阈值

        // 打印每个设备的详细信息
        List<Map<String, Object>> deviceDetails = new ArrayList<>();
        for (DeviceStatus device : allDevices) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("deviceId", device.getDeviceId());
            detail.put("status", device.getStatus());
            detail.put("deviceName", device.getDeviceName());
            detail.put("lastOnlineTime", device.getLastOnlineTime());
            detail.put("lastHeartbeatTime", device.getLastHeartbeatTime());
            detail.put("createTime", device.getCreateTime());
            detail.put("updateTime", device.getUpdateTime());

            // 计算距离最后心跳的秒数
            long secondsAgo = 0;
            if (device.getLastHeartbeatTime() != null) {
                secondsAgo = java.time.Duration.between(device.getLastHeartbeatTime(), LocalDateTime.now()).getSeconds();
            }
            detail.put("secondsSinceLastHeartbeat", secondsAgo);
            detail.put("isTimeout", secondsAgo > 300);

            deviceDetails.add(detail);
        }
        debugInfo.put("devices", deviceDetails);

        System.out.println("=== 设备状态调试信息 ===");
        System.out.println("总设备数: " + allDevices.size());
        System.out.println("心跳超时阈值: 300秒（5分钟）");
        for (DeviceStatus device : allDevices) {
            long secondsAgo = 0;
            if (device.getLastHeartbeatTime() != null) {
                secondsAgo = java.time.Duration.between(device.getLastHeartbeatTime(), LocalDateTime.now()).getSeconds();
            }
            String status = secondsAgo > 300 ? "⚠️超时" : "✅正常";
            System.out.println(String.format(
                "设备ID: %s, 状态: %s, 最后心跳: %s秒前 %s",
                device.getDeviceId(), device.getStatus(), secondsAgo, status
            ));
        }

        return AjaxResult.success(debugInfo);
    }
}