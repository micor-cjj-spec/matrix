package single.cjj.bizfi.utils;

import lombok.Data;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

public class CaptchaUtils {

    @Data
    public static class Captcha {
        private String key;
        private String code;
        private String base64Image;
    }

    public static Captcha generateCaptcha() {
        int width = 120;
        int height = 40;
        int codeCount = 4;
        int fontHeight = 32;

        char[] codeSequence = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890".toCharArray();
        BufferedImage buffImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = buffImg.createGraphics();

        Random random = new Random();
        g.setColor(Color.WHITE); // 背景
        g.fillRect(0, 0, width, height);
        g.setFont(new Font("Fixedsys", Font.BOLD, fontHeight));

        // 干扰线
        for (int i = 0; i < 10; i++) {
            g.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            g.drawLine(x, y, x1, y1);
        }

        StringBuilder code = new StringBuilder();
        int x = width / (codeCount + 2);

        for (int i = 0; i < codeCount; i++) {
            String ch = String.valueOf(codeSequence[random.nextInt(codeSequence.length)]);
            code.append(ch);
            g.setColor(new Color(random.nextInt(150), random.nextInt(150), random.nextInt(150)));
            g.drawString(ch, (i + 1) * x, height - 10);
        }

        g.dispose();

        // 转 Base64
        String base64Image;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(buffImg, "png", os);
            base64Image = Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("验证码生成失败", e);
        }

        // 封装返回
        Captcha captcha = new Captcha();
        captcha.setKey(UUID.randomUUID().toString());
        captcha.setCode(code.toString());
        captcha.setBase64Image(base64Image);
        return captcha;
    }
}
