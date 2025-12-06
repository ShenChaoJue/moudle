package com.ziwen.moudle.service.impl.captcha;

import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.util.Config;
import com.ziwen.moudle.service.captcha.CaptchaService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Properties;

/**
 * 验证码服务实现类
 *
 * @author System
 */
@Slf4j
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private final Producer captchaProducer;

    public CaptchaServiceImpl(
            @Value("${captcha.width:110}") int width,
            @Value("${captcha.height:40}") int height,
            @Value("${captcha.length:4}") int length,
            @Value("${captcha.font-size:30}") int fontSize) {

        // 配置验证码参数
        Properties properties = new Properties();
        properties.setProperty("kaptcha.border", "yes");
        properties.setProperty("kaptcha.border.color", "105,179,90");
        properties.setProperty("kaptcha.textproducer.font.color", "blue");
        properties.setProperty("kaptcha.image.width", String.valueOf(width));
        properties.setProperty("kaptcha.image.height", String.valueOf(height));
        properties.setProperty("kaptcha.textproducer.font.size", String.valueOf(fontSize));
        properties.setProperty("kaptcha.textproducer.char.length", String.valueOf(length));
        properties.setProperty("kaptcha.textproducer.font.names", "Arial,Courier");
        properties.setProperty("kaptcha.background.clear.from", "white");
        properties.setProperty("kaptcha.background.clear.to", "white");

        Config config = new Config(properties);
        this.captchaProducer = config.getProducerImpl();
    }

    @Override
    public String[] generateCaptcha() {
        // 生成验证码文字
        String captchaText = captchaProducer.createText();

        // 生成验证码图片
        BufferedImage captchaImage = captchaProducer.createImage(captchaText);

        // 将图片转换为Base64编码
        String base64Image = convertImageToBase64(captchaImage);

        return new String[]{base64Image, captchaText};
    }

    @Override
    public BufferedImage generateCaptchaImage() {
        String captchaText = captchaProducer.createText();
        return captchaProducer.createImage(captchaText);
    }

    @Override
    public String getCaptchaText(BufferedImage image) {
        // 这个方法主要用于从现有图片中获取验证码文字
        // 但由于Kaptcha生成的验证码文字在图片生成后无法直接获取，
        // 我们在实际使用中通常需要将验证码值一起返回给前端
        // 这里可以留空或者抛出异常，提示使用方式
        throw new UnsupportedOperationException("请使用 generateCaptcha() 方法获取验证码文字");
    }

    @Override
    public boolean verifyCaptcha(String inputCaptcha, String actualCaptcha) {
        if (inputCaptcha == null || actualCaptcha == null) {
            return false;
        }
        // 验证码比较（忽略大小写）
        return inputCaptcha.trim().equalsIgnoreCase(actualCaptcha.trim());
    }

    /**
     * 将图片转换为Base64编码字符串
     *
     * @param image 验证码图片
     * @return Base64编码的图片字符串
     */
    private String convertImageToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            throw new RuntimeException("验证码图片转换失败", e);
        }
    }
}
