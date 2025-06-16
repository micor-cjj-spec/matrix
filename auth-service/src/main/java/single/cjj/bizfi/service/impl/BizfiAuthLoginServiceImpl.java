package single.cjj.bizfi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.client.BaseUserClient;
import single.cjj.bizfi.dto.LoginRequest;
import single.cjj.bizfi.dto.LoginResponse;
import single.cjj.bizfi.entity.BizfiAuthLogin;
import single.cjj.bizfi.entity.BizfiBaseUser;
import single.cjj.bizfi.exception.BizException;
import single.cjj.bizfi.mapper.BizfiAuthLoginMapper;
import single.cjj.bizfi.service.BizfiAuthLoginService;

import java.nio.charset.StandardCharsets;

@Service
public class BizfiAuthLoginServiceImpl implements BizfiAuthLoginService {

    @Autowired
    private BaseUserClient baseUserClient;

    @Autowired
    private BizfiAuthLoginMapper bizfiAuthLoginMapper;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public LoginResponse loginByAccount(LoginRequest request) {
        String account = request.getUsername();
        String password = request.getPassword();
        String captcha = request.getCaptcha();
        String captchaKey = request.getCaptchaKey();

        // 1️⃣ 参数校验
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            throw new BizException("请输入正确的账号密码");
        }

        if (!StringUtils.hasText(captcha) || !StringUtils.hasText(captchaKey)) {
            throw new BizException("请输入验证码");
        }

        // 2️⃣ 验证码校验
        String redisKey = "captcha:" + captchaKey;
        String cachedCaptcha = redisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(cachedCaptcha)) {
            throw new BizException("验证码已失效，请重新获取");
        }

        if (!captcha.equalsIgnoreCase(cachedCaptcha)) {
            throw new BizException("验证码错误");
        }

        // 删除验证码，防止重用
        redisTemplate.delete(redisKey);

        // 3️⃣ 查询用户
        BizfiBaseUser user = baseUserClient.getByAccount(account).getData();
        if (user == null) {
            throw new BizException("用户不存在");
        }

        // 4️⃣ 查询密码
        BizfiAuthLogin authLogin = bizfiAuthLoginMapper.selectOne(
                new LambdaQueryWrapper<BizfiAuthLogin>()
                        .eq(BizfiAuthLogin::getFuserid, user.getFid())
        );

        if (authLogin == null) {
            throw new BizException("改用户没有维护密码，请使用其他方式登录");
        }

        // 5️⃣ 密码校验
        String inputPasswordMd5 = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!inputPasswordMd5.equalsIgnoreCase(authLogin.getFpassword())) {
            throw new BizException("密码错误");
        }

        // 6️⃣ 返回结果
        LoginResponse response = new LoginResponse();
        response.setFid(user.getFid());
        response.setUserName(user.getFnumber());
        response.setPhoneNumber(user.getFphone());
        response.setEmail(user.getFemail());

        return response;
    }



    @Override
    public LoginResponse loginByPhone(LoginRequest request) {
        // 你已有手机号+验证码登录接口逻辑，此处不做处理
        return null;
    }
}
