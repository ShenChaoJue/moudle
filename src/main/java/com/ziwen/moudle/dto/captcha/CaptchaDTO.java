package com.ziwen.moudle.dto.captcha;

import lombok.Data;

/**
 * 验证码 DTO
 *
 * @author System
 */
@Data
public class CaptchaDTO {

    /**
     * 验证码图片（Base64编码）
     */
    private String captchaImage;

    /**
     * 验证码Key（用于关联验证码）
     */
    private String captchaKey;

    /**
     * 验证码值（开发模式下可以返回，生产环境建议删除此字段）
     */
    private String captchaValue;
}
