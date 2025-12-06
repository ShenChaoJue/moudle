package com.ziwen.moudle.service.impl;

import com.ziwen.moudle.entity.DeviceStatus;
import com.ziwen.moudle.service.DeviceStatusService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备状态服务实现类（内存存储）
 */
@Service
public class DeviceStatusServiceImpl implements DeviceStatusService {

    // 设备状态存储（设备ID -> 设备状态）
    private final Map<String, DeviceStatus> deviceStatusMap = new ConcurrentHashMap<>();

    @Override
    public void deviceOnline(String deviceId, String payload) {
        DeviceStatus deviceStatus = deviceStatusMap.getOrDefault(deviceId, createDefaultDeviceStatus(deviceId));

        // 解析payload（假设是JSON格式，这里简单处理）
        Map<String, String> info = parsePayload(payload);

        deviceStatus.setStatus("ONLINE");
        deviceStatus.setLastOnlineTime(LocalDateTime.now());
        deviceStatus.setOnlineDuration(0L); // 重置在线时长

        // 更新设备信息
        if (info.containsKey("deviceName")) {
            deviceStatus.setDeviceName(info.get("deviceName"));
        }
        if (info.containsKey("deviceType")) {
            deviceStatus.setDeviceType(info.get("deviceType"));
        }
        if (info.containsKey("ipAddress")) {
            deviceStatus.setIpAddress(info.get("ipAddress"));
        }
        if (info.containsKey("clientId")) {
            deviceStatus.setClientId(info.get("clientId"));
        }
        if (info.containsKey("firmwareVersion")) {
            deviceStatus.setFirmwareVersion(info.get("firmwareVersion"));
        }

        deviceStatus.setUpdateTime(LocalDateTime.now());
        deviceStatusMap.put(deviceId, deviceStatus);

        System.out.println("设备上线: " + deviceId + " | 信息: " + payload);
    }

    @Override
    public void deviceOffline(String deviceId, String payload) {
        DeviceStatus deviceStatus = deviceStatusMap.get(deviceId);
        if (deviceStatus == null) {
            deviceStatus = createDefaultDeviceStatus(deviceId);
        }

        deviceStatus.setStatus("OFFLINE");
        deviceStatus.setLastOfflineTime(LocalDateTime.now());

        // 计算在线时长
        if (deviceStatus.getLastOnlineTime() != null) {
            long duration = java.time.Duration.between(
                deviceStatus.getLastOnlineTime(),
                LocalDateTime.now()
            ).getSeconds();
            deviceStatus.setOnlineDuration(duration);
        }

        deviceStatus.setUpdateTime(LocalDateTime.now());
        deviceStatusMap.put(deviceId, deviceStatus);

        System.out.println("设备下线: " + deviceId + " | 原因: " + payload);
    }

    @Override
    public void deviceHeartbeat(String deviceId, String payload) {
        DeviceStatus deviceStatus = deviceStatusMap.get(deviceId);
        if (deviceStatus == null) {
            deviceStatus = createDefaultDeviceStatus(deviceId);
            deviceStatus.setStatus("ONLINE");
            deviceStatus.setLastOnlineTime(LocalDateTime.now());
        }

        deviceStatus.setLastHeartbeatTime(LocalDateTime.now());

        // 解析心跳数据
        Map<String, String> info = parsePayload(payload);
        if (info.containsKey("batteryLevel")) {
            try {
                deviceStatus.setBatteryLevel(Integer.parseInt(info.get("batteryLevel")));
            } catch (NumberFormatException e) {
                // 忽略格式错误
            }
        }
        if (info.containsKey("signalStrength")) {
            try {
                deviceStatus.setSignalStrength(Integer.parseInt(info.get("signalStrength")));
            } catch (NumberFormatException e) {
                // 忽略格式错误
            }
        }

        deviceStatus.setUpdateTime(LocalDateTime.now());
        deviceStatusMap.put(deviceId, deviceStatus);

        System.out.println("设备心跳: " + deviceId + " | 数据: " + payload);
    }

    @Override
    public void deviceReport(String deviceId, String payload) {
        DeviceStatus deviceStatus = deviceStatusMap.get(deviceId);
        if (deviceStatus == null) {
            deviceStatus = createDefaultDeviceStatus(deviceId);
            deviceStatus.setStatus("ONLINE");
            deviceStatus.setLastOnlineTime(LocalDateTime.now());
        }

        deviceStatus.setUpdateTime(LocalDateTime.now());
        deviceStatusMap.put(deviceId, deviceStatus);

        System.out.println("设备上报: " + deviceId + " | 数据: " + payload);
    }

    @Override
    public DeviceStatus getDeviceStatus(String deviceId) {
        return deviceStatusMap.get(deviceId);
    }

    @Override
    public List<DeviceStatus> getAllDeviceStatus() {
        return new ArrayList<>(deviceStatusMap.values());
    }

    @Override
    public List<DeviceStatus> getOnlineDevices() {
        List<DeviceStatus> onlineDevices = new ArrayList<>();
        for (DeviceStatus device : deviceStatusMap.values()) {
            if ("ONLINE".equals(device.getStatus())) {
                onlineDevices.add(device);
            }
        }
        return onlineDevices;
    }

    @Override
    public List<DeviceStatus> getOfflineDevices() {
        List<DeviceStatus> offlineDevices = new ArrayList<>();
        for (DeviceStatus device : deviceStatusMap.values()) {
            if ("OFFLINE".equals(device.getStatus()) || device.getStatus() == null) {
                offlineDevices.add(device);
            }
        }
        return offlineDevices;
    }

    @Override
    public Map<String, Object> getDeviceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        int total = deviceStatusMap.size();
        int online = getOnlineDevices().size();
        int offline = getOfflineDevices().size();

        stats.put("totalDevices", total);
        stats.put("onlineDevices", online);
        stats.put("offlineDevices", offline);
        stats.put("onlineRate", total > 0 ? (online * 100.0 / total) : 0);
        stats.put("updateTime", LocalDateTime.now());

        return stats;
    }

    @Override
    public void updateDeviceInfo(DeviceStatus deviceStatus) {
        if (deviceStatus.getDeviceId() == null) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
        deviceStatus.setUpdateTime(LocalDateTime.now());
        deviceStatusMap.put(deviceStatus.getDeviceId(), deviceStatus);
    }

    @Override
    public void deleteDevice(String deviceId) {
        deviceStatusMap.remove(deviceId);
    }

    /**
     * 创建默认设备状态
     */
    private DeviceStatus createDefaultDeviceStatus(String deviceId) {
        DeviceStatus deviceStatus = new DeviceStatus();
        deviceStatus.setDeviceId(deviceId);
        deviceStatus.setDeviceName("设备-" + deviceId);
        deviceStatus.setDeviceType("UNKNOWN");
        deviceStatus.setCreateTime(LocalDateTime.now());
        deviceStatus.setUpdateTime(LocalDateTime.now());
        deviceStatus.setIsDeleted(0);
        return deviceStatus;
    }

    /**
     * 解析payload（简单实现，实际应该用JSON解析）
     */
    private Map<String, String> parsePayload(String payload) {
        Map<String, String> result = new HashMap<>();
        if (payload == null || payload.trim().isEmpty()) {
            return result;
        }

        // 简单解析：假设是key=value格式，用逗号分隔
        String[] pairs = payload.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                result.put(kv[0].trim(), kv[1].trim());
            }
        }
        return result;
    }
}