package single.cjj.bizfi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.entity.BizfiBaseUser;
import single.cjj.bizfi.service.BizfiBaseUserService;

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
    @GetMapping("/{account}")
    public ApiResponse<BizfiBaseUser> getByAccount(@PathVariable("account") String account) {
        BizfiBaseUser userByAccount = baseUserService.getUserByAccount(account);
        return ApiResponse.success(userByAccount);
    }
}
