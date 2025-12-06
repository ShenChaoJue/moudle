package com.ziwen.moudle.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 设备状态实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceStatus extends BaseEntity<DeviceStatus> {

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 设备状态：ONLINE-在线，OFFLINE-离线
     */
    private String status;

    /**
     * 最后上线时间
     */
    private LocalDateTime lastOnlineTime;

    /**
     * 最后下线时间
     */
    private LocalDateTime lastOfflineTime;

    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeatTime;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 连接客户端ID
     */
    private String clientId;

    /**
     * 设备版本
     */
    private String firmwareVersion;

    /**
     * 信号强度（RSSI）
     */
    private Integer signalStrength;

    /**
     * 电池电量（0-100）
     */
    private Integer batteryLevel;

    /**
     * 在线时长（秒）
     */
    private Long onlineDuration;

    /**
     * 备注
     */
    private String remark;
}