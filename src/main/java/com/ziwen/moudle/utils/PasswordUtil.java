package com.ziwen.moudle.utils;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码工具类
 * 提供密码加密和验证功能（使用SHA-256 + 随机盐）
 *
 * @author boot
 */
@Slf4j
public class PasswordUtil {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    /**
     * 加密密码
     *
     * @param rawPassword 原始密码
     * @return 加密后的密码（格式：salt:hash）
     */
    public static String encrypt(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        byte[] salt = generateSalt();
        String saltBase64 = Base64.getEncoder().encodeToString(salt);

        String hash = hash(rawPassword, salt);

        return saltBase64 + ":" + hash;
    }

    /**
     * 验证密码
     *
     * @param rawPassword     原始密码
     * @param encryptedPassword 加密后的密码（格式：salt:hash）
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encryptedPassword) {
        if (rawPassword == null || encryptedPassword == null) {
            return false;
        }

        try {
            String[] parts = encryptedPassword.split(":");
            if (parts.length != 2) {
                log.warn("加密密码格式不正确");
                return false;
            }

            String saltBase64 = parts[0];
            String expectedHash = parts[1];

            byte[] salt = Base64.getDecoder().decode(saltBase64);
            String actualHash = hash(rawPassword, salt);

            return expectedHash.equals(actualHash);
        } catch (Exception e) {
            log.error("密码验证失败", e);
            return false;
        }
    }

    /**
     * 生成随机盐
     */
    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * 计算哈希值
     */
    private static String hash(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);

            // 将盐和密码拼接后计算哈希
            byte[] combined = new byte[salt.length + password.getBytes(StandardCharsets.UTF_8).length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(password.getBytes(StandardCharsets.UTF_8), 0, combined, salt.length, password.getBytes(StandardCharsets.UTF_8).length);

            byte[] hashedBytes = md.digest(combined);

            // 转换为Base64编码
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("算法 {} 不存在", ALGORITHM, e);
            throw new RuntimeException("密码加密失败", e);
        }
    }
}
