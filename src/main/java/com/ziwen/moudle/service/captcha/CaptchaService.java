package com.ziwen.moudle.service.captcha;

import java.awt.image.BufferedImage;

/**
 * 验证码服务接口
 *
 * @author System
 */
public interface CaptchaService {

    /**
     * 生成验证码图片和验证码值
     *
     * @return 包含验证码图片和验证码值的数组
     *         [0] - Base64编码的验证码图片
     *         [1] - 验证码值
     */
    String[] generateCaptcha();

    /**
     * 生成验证码图片
     *
     * @return 验证码图片
     */
    BufferedImage generateCaptchaImage();

    /**
     * 获取验证码值（从图片中）
     *
     * @param image 验证码图片
     * @return 验证码值
     */
    String getCaptchaText(BufferedImage image);

    /**
     * 验证验证码是否正确
     *
     * @param inputCaptcha 用户输入的验证码
     * @param actualCaptcha 实际验证码值
     * @return 验证结果
     */
    boolean verifyCaptcha(String inputCaptcha, String actualCaptcha);
}
