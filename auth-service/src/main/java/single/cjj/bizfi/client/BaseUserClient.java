package single.cjj.bizfi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.entity.BizfiBaseUser;

@FeignClient(name = "base-service", url = "${feign.client.config.base-client.url:}")
public interface BaseUserClient {

    // 查（根据账号/工号/手机号/邮箱，任选其一）
    @GetMapping("/api/user/account/{account}")
    ApiResponse<BizfiBaseUser> getByAccount(@PathVariable("account") String account);

    // 增
    @PostMapping("/api/user")
    ApiResponse<BizfiBaseUser> addUser(@RequestBody BizfiBaseUser user);

    // 改
    @PutMapping("/api/user")
    ApiResponse<BizfiBaseUser> updateUser(@RequestBody BizfiBaseUser user);

    // 删
    @DeleteMapping("/api/user/{fid}")
    ApiResponse<Boolean> deleteUser(@PathVariable("fid") Long fid);
}
