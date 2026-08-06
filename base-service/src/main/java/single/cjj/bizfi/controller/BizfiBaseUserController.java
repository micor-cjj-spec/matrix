package single.cjj.bizfi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
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
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles
    ) {
        BizfiBaseUser user = null;
        try {
            user = baseUserService.getUserById(Long.parseLong(userId));
        } catch (NumberFormatException exception) {
            log.warn("current user id is not numeric, userId={}", userId);
        }

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", userId);
        profile.put("tenantId", tenantId);
        profile.put("roles", StringUtils.hasText(roles) ? List.of(roles.split(",")) : List.of());
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
