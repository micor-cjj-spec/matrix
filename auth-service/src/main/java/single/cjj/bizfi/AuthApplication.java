package single.cjj.bizfi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Auth Service 启动类
 */
@SpringBootApplication(scanBasePackages = "single.cjj.bizfi")
@MapperScan(basePackages = "single.cjj.bizfi.mapper") // 推荐标准写法，防止扫描不到 MyBatis-Plus Mapper
@EnableFeignClients(basePackages = "single.cjj.bizfi.client")
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
        System.out.println("✅ Auth-Service 启动成功！");
    }
}
