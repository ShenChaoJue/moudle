package com.ziwen.moudle.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 雪花算法ID生成器
 *
 * @author ziwen
 */
@Slf4j
@Component
public class SnowflakeIdGenerator {

    /**
     * 起始时间戳 (2024-01-01 00:00:00)
     */
    private static final long START_TIMESTAMP = 1704038400000L;

    /**
     * 机器ID占用的位数 (5位)
     */
    private static final long WORKER_ID_BITS = 5L;

    /**
     * 数据中心ID占用的位数 (5位)
     */
    private static final long DATACENTER_ID_BITS = 5L;

    /**
     * 支持的最大机器ID (31)
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 支持的最大数据中心ID (31)
     */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /**
     * 序列号占用的位数 (12位)
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 机器ID左移位数 (12位)
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 数据中心ID左移位数 (17位)
     */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 时间戳左移位数 (22位)
     */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    /**
     * 序列号的掩码 (4095)
     */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 工作机器ID (0-31)
     */
    private final long workerId;

    /**
     * 数据中心ID (0-31)
     */
    private final long datacenterId;

    /**
     * 序列号 (0-4095)
     */
    private long sequence = 0L;

    /**
     * 上一次时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 构造函数
     *
     * @param workerId     工作机器ID
     * @param datacenterId 数据中心ID
     */
    public SnowflakeIdGenerator(@Value("${snowflake.worker-id:1}") long workerId,
                               @Value("${snowflake.datacenter-id:1}") long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("工作机器ID不能大于%d或小于0", MAX_WORKER_ID));
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("数据中心ID不能大于%d或小于0", MAX_DATACENTER_ID));
        }

        this.workerId = workerId;
        this.datacenterId = datacenterId;
        log.info("雪花ID生成器初始化完成 - 工作机器ID: {}, 数据中心ID: {}", workerId, datacenterId);
    }

    /**
     * 生成下一个ID
     *
     * @return ID
     */
    public synchronized long nextId() {
        long timestamp = currentTime();

        // 如果当前时间小于上一次时间，说明系统时钟回拨了
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                try {
                    // 等待5毫秒重试
                    wait(offset << 1);
                    timestamp = currentTime();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException("系统时钟回拨，拒绝生成ID");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("系统时钟回拨，等待被中断", e);
                }
            } else {
                throw new RuntimeException("系统时钟回拨超过5毫秒，拒绝生成ID");
            }
        }

        // 如果是同一时间生成的，则进行序列号自增
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 序列号溢出 (4096)
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 时间戳改变，序列号重置
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 组装ID
        long id = ((timestamp - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;

        log.debug("生成雪花ID: {}", id);
        return id;
    }

    /**
     * 阻塞到下一毫秒
     *
     * @param lastTimestamp 上一次时间戳
     * @return 当前时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = currentTime();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTime();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳
     *
     * @return 当前时间戳
     */
    private long currentTime() {
        return System.currentTimeMillis();
    }
}
