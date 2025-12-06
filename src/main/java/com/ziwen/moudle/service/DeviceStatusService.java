package com.ziwen.moudle.service;

import com.ziwen.moudle.entity.DeviceStatus;
import java.util.List;
import java.util.Map;

/**
 * 设备状态服务接口
 */
public interface DeviceStatusService {

    /**
     * 设备上线
     * @param deviceId 设备ID
     * @param payload 上线消息内容（可包含IP、版本等信息）
     */
    void deviceOnline(String deviceId, String payload);

    /**
     * 设备下线
     * @param deviceId 设备ID
     * @param payload 下线消息内容
     */
    void deviceOffline(String deviceId, String payload);

    /**
     * 设备心跳
     * @param deviceId 设备ID
     * @param payload 心跳消息内容
     */
    void deviceHeartbeat(String deviceId, String payload);

    /**
     * 设备上报数据
     * @param deviceId 设备ID
     * @param payload 上报数据
     */
    void deviceReport(String deviceId, String payload);

    /**
     * 获取设备状态
     * @param deviceId 设备ID
     * @return 设备状态
     */
    DeviceStatus getDeviceStatus(String deviceId);

    /**
     * 获取所有设备状态
     * @return 设备状态列表
     */
    List<DeviceStatus> getAllDeviceStatus();

    /**
     * 获取在线设备列表
     * @return 在线设备列表
     */
    List<DeviceStatus> getOnlineDevices();

    /**
     * 获取离线设备列表
     * @return 离线设备列表
     */
    List<DeviceStatus> getOfflineDevices();

    /**
     * 获取设备统计信息
     * @return 统计信息
     */
    Map<String, Object> getDeviceStatistics();

    /**
     * 更新设备信息
     * @param deviceStatus 设备状态
     */
    void updateDeviceInfo(DeviceStatus deviceStatus);

    /**
     * 删除设备记录
     * @param deviceId 设备ID
     */
    void deleteDevice(String deviceId);
}