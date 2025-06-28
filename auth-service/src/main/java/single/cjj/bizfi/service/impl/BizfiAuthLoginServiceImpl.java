package single.cjj.bizfi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.client.BaseUserClient;
import single.cjj.bizfi.dto.EmailSendRequest;
import single.cjj.bizfi.dto.LoginRequest;
import single.cjj.bizfi.dto.LoginResponse;
import single.cjj.bizfi.dto.RegisterRequest;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.entity.BizfiAuthLogin;
import single.cjj.bizfi.entity.BizfiBaseUser;
import single.cjj.bizfi.exception.BizException;
import single.cjj.bizfi.mapper.BizfiAuthLoginMapper;
import single.cjj.bizfi.service.BizfiAuthLoginService;
import single.cjj.bizfi.utils.EmailUtils;
import single.cjj.bizfi.utils.JwtUtils;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class BizfiAuthLoginServiceImpl implements BizfiAuthLoginService {

    @Autowired
    private BaseUserClient baseUserClient;

    @Autowired
    private BizfiAuthLoginMapper bizfiAuthLoginMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private EmailUtils emailUtils;

    /**
     * 账号密码登录（支持工号、邮箱、手机号作为账号），含验证码校验
     *
     * @param request 登录请求参数
     * @return 登录响应信息
     */
    @Override
    public LoginResponse loginByAccount(LoginRequest request) {
        String account = request.getUsername();
        String password = request.getPassword();
        String captcha = request.getCaptcha();
        String captchaKey = request.getCaptchaKey();

        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            throw new BizException("请输入正确的账号密码");
        }

        if (!StringUtils.hasText(captcha) || !StringUtils.hasText(captchaKey)) {
            throw new BizException("请输入验证码");
        }

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

        // 远程获取用户
        ApiResponse<BizfiBaseUser> userResp = baseUserClient.getByAccount(account);
        BizfiBaseUser user = userResp != null ? userResp.getData() : null;
        if (user == null) {
            throw new BizException("用户不存在");
        }

        // 查询密码表
        BizfiAuthLogin authLogin = bizfiAuthLoginMapper.selectOne(
                new LambdaQueryWrapper<BizfiAuthLogin>()
                        .eq(BizfiAuthLogin::getFuserid, user.getFid())
        );

        if (authLogin == null) {
            throw new BizException("该用户没有维护密码，请使用其他方式登录");
        }

        String inputPasswordMd5 = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!inputPasswordMd5.equalsIgnoreCase(authLogin.getFpassword())) {
            throw new BizException("密码错误");
        }

        // 生成登录 Token，并写入 Redis，1 小时未操作自动过期
        String token = JwtUtils.generateToken(user.getFid(), user.getFid());
        redisTemplate.opsForValue()
                .set("token:" + token, String.valueOf(user.getFid()), 1, TimeUnit.HOURS);

        LoginResponse response = new LoginResponse();
        response.setFid(user.getFid());
        response.setUserName(user.getFnumber());
        response.setPhoneNumber(user.getFphone());
        response.setEmail(user.getFemail());
        response.setToken(token);
        response.setExpireIn(JwtUtils.EXPIRE / 1000);

        return response;
    }

    /**
     * 手机号+验证码登录（待实现/留空）
     *
     * @param request 登录请求参数
     * @return 登录响应（此处为null）
     */
    @Override
    public LoginResponse loginByPhone(LoginRequest request) {
        // 你已有手机号+验证码登录接口逻辑，此处不做处理
        return null;
    }

    /**
     * 发送注册邮箱验证码，写入Redis并发邮件
     *
     * @param request 邮箱验证码发送请求
     * @return 是否发送成功
     */
    @Override
    public Boolean sendEmailCode(EmailSendRequest request) {
        String email = request.getEmail();
        if (!StringUtils.hasText(email)) {
            throw new BizException("邮箱不能为空");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new BizException("邮箱格式不正确");
        }
        String code = String.format("%06d", new Random().nextInt(999999));
        redisTemplate.opsForValue().set("email:code:" + email, code, 5, TimeUnit.MINUTES);
        emailUtils.sendSimpleMail(email, "注册验证码", "您的注册验证码为：" + code + "，5分钟内有效");
        return true;
    }

    /**
     * 邮箱注册用户（远程创建基础资料，维护本地密码），支持唯一性校验
     *
     * @param request 注册请求参数
     * @return 注册是否成功
     */
    @Override
    public Boolean register(RegisterRequest request) {
        String email = request.getEmail();
        String password = request.getPassword();
        String code = request.getCode();

        if (!StringUtils.hasText(email) || !StringUtils.hasText(password) || !StringUtils.hasText(code)) {
            throw new BizException("参数不能为空");
        }
        String redisKey = "email:code:" + email;
        String cacheCode = redisTemplate.opsForValue().get(redisKey);
        if (cacheCode == null || !cacheCode.equalsIgnoreCase(code)) {
            throw new BizException("验证码错误或已过期");
        }
        // 查重（远程）
        if (!check("email", email)) {
            throw new BizException("邮箱已注册");
        }

        BizfiBaseUser user = new BizfiBaseUser();
        user.setFemail(email);
        user.setFstatus("1"); // 启用
        // 远程新增
        ApiResponse<BizfiBaseUser> addResp = baseUserClient.addUser(user);
        BizfiBaseUser createdUser = addResp != null ? addResp.getData() : null;
        if (createdUser == null || createdUser.getFid() == null) {
            throw new BizException("用户注册失败");
        }

        BizfiAuthLogin authLogin = new BizfiAuthLogin();
        authLogin.setFuserid(createdUser.getFid());
        String md5pwd = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        authLogin.setFpassword(md5pwd);
        bizfiAuthLoginMapper.insert(authLogin);

        redisTemplate.delete(redisKey);
        return true;
    }

    /**
     * 基础资料唯一性检查（邮箱、用户名、手机号等）
     *
     * @param type  检查类型（email/userName/phone）
     * @param value 检查值
     * @return true=唯一，false=已存在
     */
    @Override
    public Boolean check(String type, String value) {
        // 远程查用户是否已存在
        ApiResponse<BizfiBaseUser> userResp = null;
        if ("email".equalsIgnoreCase(type) || "userName".equalsIgnoreCase(type) || "username".equalsIgnoreCase(type) || "phone".equalsIgnoreCase(type)) {
            userResp = baseUserClient.getByAccount(value);
        } else {
            throw new BizException("不支持的类型");
        }
        // 没查到才是唯一（true）
        return userResp == null || userResp.getData() == null;
    }
}
