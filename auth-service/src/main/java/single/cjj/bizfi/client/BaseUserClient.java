package single.cjj.bizfi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.entity.BizfiBaseUser;

@FeignClient(name = "base-service")
public interface BaseUserClient {

    // 查（根据账号/工号/手机号/邮箱，任选其一）
    @GetMapping("/user/account/{account}")
    ApiResponse<BizfiBaseUser> getByAccount(@PathVariable("account") String account);

    // 增
    @PostMapping("/user")
    ApiResponse<BizfiBaseUser> addUser(@RequestBody BizfiBaseUser user);

    // 改
    @PutMapping("/user")
    ApiResponse<BizfiBaseUser> updateUser(@RequestBody BizfiBaseUser user);

    // 删
    @DeleteMapping("/user/{fid}")
    ApiResponse<Boolean> deleteUser(@PathVariable("fid") Long fid);
}
