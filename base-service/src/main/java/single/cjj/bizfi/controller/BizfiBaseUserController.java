package single.cjj.bizfi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.entity.BizfiBaseUser;
import single.cjj.bizfi.service.BizfiBaseUserService;

import java.util.HashMap;
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
@RestController
@RequestMapping("/user")
public class BizfiBaseUserController {
    @Autowired
    private BizfiBaseUserService baseUserService;
    @GetMapping("/account/{account}")
    public ApiResponse<BizfiBaseUser> getByAccount(@PathVariable("account") String account) {
        BizfiBaseUser userByAccount = baseUserService.getUserByAccount(account);
        return ApiResponse.success(userByAccount);
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
}
