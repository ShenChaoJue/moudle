package com.ziwen.moudle.controller.captcha;

import com.ziwen.moudle.common.AjaxResult;
import com.ziwen.moudle.dto.captcha.CaptchaDTO;
import com.ziwen.moudle.service.captcha.CaptchaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 验证码 REST API
 *
 * @author System
 */
@Slf4j
@RestController
@RequestMapping("/api/captcha")
@Tag(name = "验证码管理", description = "验证码相关接口")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /**
     * 生成验证码
     */
    @GetMapping("/generate")
    @Operation(summary = "生成验证码", description = "获取验证码图片和验证码值")
    public AjaxResult generateCaptcha() {
        try {
            // 生成验证码图片和验证码值
            String[] captchaData = captchaService.generateCaptcha();

            // 构建返回对象
            CaptchaDTO captchaDTO = new CaptchaDTO();
            captchaDTO.setCaptchaImage(captchaData[0]); // Base64编码的图片
            captchaDTO.setCaptchaKey(java.util.UUID.randomUUID().toString()); // 生成唯一key
            captchaDTO.setCaptchaValue(captchaData[1]); // 验证码值

            // 注意：在生产环境中，建议不要返回 captchaValue
            // 这里为了演示方便才返回，实际使用时应该将验证码值存储在缓存中
            // 并通过 captchaKey 来关联验证

            return AjaxResult.success("验证码生成成功", captchaDTO);
        } catch (Exception e) {
            log.error("验证码生成失败", e);
            return AjaxResult.error("验证码生成失败");
        }
    }

    /**
     * 刷新验证码
     */
    @GetMapping("/refresh")
    @Operation(summary = "刷新验证码", description = "重新生成验证码图片")
    public AjaxResult refreshCaptcha() {
        return generateCaptcha();
    }

    /**
     * 验证验证码
     */
    @PostMapping("/verify")
    @Operation(summary = "验证验证码", description = "验证用户输入的验证码是否正确")
    public AjaxResult verifyCaptcha(@RequestParam String captchaValue,
                                    @RequestParam String inputCaptcha) {
        try {
            boolean isValid = captchaService.verifyCaptcha(inputCaptcha, captchaValue);

            if (isValid) {
                return AjaxResult.success("验证码正确");
            } else {
                return AjaxResult.warn("验证码错误");
            }
        } catch (Exception e) {
            log.error("验证码验证失败", e);
            return AjaxResult.error("验证码验证失败");
        }
    }
}
