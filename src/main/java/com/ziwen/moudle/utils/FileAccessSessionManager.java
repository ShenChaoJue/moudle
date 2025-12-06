package com.ziwen.moudle.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文件访问会话管理器
 * 用于管理文件访问令牌，支持强制撤销访问权限
 */
@Component
public class FileAccessSessionManager {

    /**
     * 会话令牌存储
     * key: 令牌ID, value: 会话信息
     */
    private final ConcurrentHashMap<String, FileAccessSession> sessions = new ConcurrentHashMap<>();

    /**
     * 创建文件访问令牌
     * @param fileId 文件ID
     * @param userId 用户ID（可为空表示无需登录）
     * @param expiresInMinutes 过期时间（分钟）
     * @return 访问令牌
     */
    public String createAccessToken(Long fileId, String userId, int expiresInMinutes) {
        String token = UUID.randomUUID().toString().replace("-", "");
        FileAccessSession session = new FileAccessSession(
            token,
            fileId,
            userId,
            LocalDateTime.now().plusMinutes(expiresInMinutes)
        );
        sessions.put(token, session);
        return token;
    }

    /**
     * 验证令牌是否有效
     * @param token 访问令牌
     * @return 令牌有效返回会话信息，无效返回null
     */
    public FileAccessSession validateToken(String token) {
        FileAccessSession session = sessions.get(token);
        if (session == null) {
            return null;
        }

        // 检查是否过期
        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            sessions.remove(token);
            return null;
        }

        return session;
    }

    /**
     * 撤销令牌（强制停止访问）
     * @param token 要撤销的令牌
     * @return 撤销成功返回true，令牌不存在或已过期返回false
     */
    public boolean revokeToken(String token) {
        return sessions.remove(token) != null;
    }

    /**
     * 撤销文件的所有访问令牌（管理员功能）
     * @param fileId 文件ID
     * @return 撤销的令牌数量
     */
    public int revokeFileTokens(Long fileId) {
        AtomicInteger count = new AtomicInteger(0);
        sessions.entrySet().removeIf(entry -> {
            if (entry.getValue().getFileId().equals(fileId)) {
                count.incrementAndGet();
                return true;
            }
            return false;
        });
        return count.get();
    }

    /**
     * 清理过期令牌（定期调用）
     */
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        sessions.entrySet().removeIf(entry -> now.isAfter(entry.getValue().getExpiresAt()));
    }

    /**
     * 获取活跃令牌数量
     */
    public int getActiveTokenCount() {
        return (int) sessions.values().stream()
            .filter(session -> LocalDateTime.now().isBefore(session.getExpiresAt()))
            .count();
    }

    /**
     * 文件访问会话信息
     */
    public static class FileAccessSession {
        private final String token;
        private final Long fileId;
        private final String userId;
        private final LocalDateTime expiresAt;
        private final LocalDateTime createdAt;

        public FileAccessSession(String token, Long fileId, String userId, LocalDateTime expiresAt) {
            this.token = token;
            this.fileId = fileId;
            this.userId = userId;
            this.expiresAt = expiresAt;
            this.createdAt = LocalDateTime.now();
        }

        // Getters
        public String getToken() { return token; }
        public Long getFileId() { return fileId; }
        public String getUserId() { return userId; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}
