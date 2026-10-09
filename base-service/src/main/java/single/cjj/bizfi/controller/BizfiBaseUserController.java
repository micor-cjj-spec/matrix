package single.cjj.bizfi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import io.jsonwebtoken.Claims;
import single.cjj.bizfi.security.JwtUtils;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.entity.BizfiBaseUser;
import single.cjj.bizfi.service.BizfiBaseUserService;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 基础用户信息表 前端控制器
 * </p>
 *
 * @author micor
 * @since 2025-06-04
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class BizfiBaseUserController {
    @Autowired
    private BizfiBaseUserService baseUserService;

    @GetMapping("/account/{account}")
    public ApiResponse<BizfiBaseUser> getByAccount(@PathVariable("account") String account) {
        long start = System.currentTimeMillis();
        try {
            BizfiBaseUser userByAccount = baseUserService.getUserByAccount(account);
            log.info("getByAccount success, account={}, found={}, costMs={}", account, userByAccount != null, System.currentTimeMillis() - start);
            return ApiResponse.success(userByAccount);
        } catch (Exception e) {
            log.error("getByAccount failed, account={}, costMs={}", account, System.currentTimeMillis() - start, e);
            return ApiResponse.error("查询用户失败: " + e.getMessage());
        }
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> currentUser(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "Authorization", required = false) String bearer
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth.getPrincipal() == null
                || !StringUtils.hasText(bearer) || !bearer.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少有效登录身份");
        }
        String principal = auth.getName();
        if (!StringUtils.hasText(principal)
                || (StringUtils.hasText(userId) && !principal.equals(userId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "请求身份与已登录账号不一致");
        }
        Claims claims;
        try {
            claims = JwtUtils.parseToken(bearer.substring(7));
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录令牌无效");
        }
        if (!principal.equals(String.valueOf(claims.get("id")))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "登录身份与令牌不一致");
        }
        userId = principal;
        String tenantId = String.valueOf(claims.getOrDefault("tenantId", "default"));
        Object roleClaims = claims.get("roles");
        List<String> roles = roleClaims instanceof java.util.Collection<?> collection
                ? collection.stream().map(String::valueOf).toList() : List.of();
        BizfiBaseUser user = null;
        try {
            user = baseUserService.getUserById(Long.parseLong(userId));
        } catch (NumberFormatException exception) {
            log.warn("current user id is not numeric, userId={}", userId);
        }

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", userId);
        profile.put("tenantId", tenantId);
        profile.put("roles", roles);
        profile.put("displayName", resolveDisplayName(user, userId));
        profile.put("avatarUrl", user == null ? null : firstText(user.getFavatar(), user.getFheadsculpture()));
        profile.put("employeeNumber", user == null ? null : user.getFnumber());
        profile.put("email", user == null ? null : user.getFemail());
        profile.put("departmentId", user == null ? null : user.getFdptid());
        profile.put("positionId", user == null ? null : user.getFpositionid());
        return ApiResponse.success(profile);
    }

    // 新增
    @PostMapping
    public ApiResponse<BizfiBaseUser> addUser(@RequestBody BizfiBaseUser user) {
        return ApiResponse.success(baseUserService.addUser(user));
    }

    // 删除
    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> deleteUser(@PathVariable("fid") Long fid) {
        return ApiResponse.success(baseUserService.deleteUser(fid));
    }

    // 修改
    @PutMapping
    public ApiResponse<BizfiBaseUser> updateUser(@RequestBody BizfiBaseUser user) {
        return ApiResponse.success(baseUserService.updateUser(user));
    }

    /**
     * 分页/条件查询用户列表
     */
    @GetMapping("/list")
    public ApiResponse<IPage<BizfiBaseUser>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "ftruename", required = false) String ftruename,
            @RequestParam(value = "femail", required = false) String femail,
            @RequestParam(value = "fstatus", required = false) String fstatus
    ) {
        Map<String, Object> query = new HashMap<>();
        query.put("ftruename", ftruename);
        query.put("femail", femail);
        query.put("fstatus", fstatus);
        return ApiResponse.success(baseUserService.getUserList(page, size, query));
    }

    /**
     * 根据ID查详情
     */
    @GetMapping("/{fid}")
    public ApiResponse<BizfiBaseUser> getById(@PathVariable("fid") Long fid) {
        return ApiResponse.success(baseUserService.getUserById(fid));
    }

    /**
     * 批量删除
     */
    @PostMapping("/delete-batch")
    public ApiResponse<Boolean> deleteBatch(@RequestBody List<Long> fids) {
        return ApiResponse.success(baseUserService.deleteBatch(fids));
    }

    private String resolveDisplayName(BizfiBaseUser user, String fallback) {
        if (user == null) {
            return fallback;
        }
        return firstText(user.getFtruename(), user.getFnickname(), user.getFnumber(), fallback);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
