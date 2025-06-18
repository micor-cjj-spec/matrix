package single.cjj.bizfi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.dto.EmailSendRequest;
import single.cjj.bizfi.dto.LoginRequest;
import single.cjj.bizfi.dto.LoginResponse;
import single.cjj.bizfi.dto.RegisterRequest;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.service.BizfiAuthLoginService;
import single.cjj.bizfi.utils.CaptchaUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private BizfiAuthLoginService bizfiAuthLoginService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("/email/send")
    public ApiResponse<Boolean> sendEmailCode(@RequestBody EmailSendRequest request) {
        return ApiResponse.success(bizfiAuthLoginService.sendEmailCode(request));
    }

    @PostMapping("/register")
    public ApiResponse<Boolean> register(@RequestBody RegisterRequest request) {
        return ApiResponse.success(bizfiAuthLoginService.register(request));
    }

    @GetMapping("/check")
    public ApiResponse<Boolean> check(@RequestParam String type, @RequestParam String value) {
        return ApiResponse.success(bizfiAuthLoginService.check(type, value));
    }

    /**
     * 获取图形验证码
     */
    @GetMapping(value = "/captcha", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> getCaptcha() {
        CaptchaUtils.Captcha captcha = CaptchaUtils.generateCaptcha();

        // 存入 Redis，3 分钟有效
        redisTemplate.opsForValue().set("captcha:" + captcha.getKey(), captcha.getCode(), 3, TimeUnit.MINUTES);

        Map<String, Object> result = new HashMap<>();
        result.put("captchaKey", captcha.getKey());
        result.put("img", "data:image/png;base64," + captcha.getBase64Image());

        return ApiResponse.success(result);
    }

    /**
     * 用户名 + 密码 登录
     */
    @PostMapping("/login/account")
    public ApiResponse<LoginResponse> loginByAccount(@RequestBody LoginRequest request) {
        LoginResponse response = bizfiAuthLoginService.loginByAccount(request);
        return ApiResponse.success(response);
    }

    /**
     * 手机号 + 验证码 登录
     */
    @PostMapping("/login/phone")
    public ApiResponse<LoginResponse> loginByPhone(@RequestBody LoginRequest request) {
        LoginResponse response = bizfiAuthLoginService.loginByPhone(request);
        return ApiResponse.success(response);
    }


}
