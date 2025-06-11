package single.cjj.bizfi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.entity.BizfiBaseUser;

@FeignClient(name = "base-service", url = "http://localhost:8082")
public interface BaseUserClient {

    @GetMapping("/user/{account}")
    ApiResponse<BizfiBaseUser> getByAccount(@PathVariable("account") String account);
}
