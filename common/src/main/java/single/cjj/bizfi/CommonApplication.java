package single.cjj.bizfi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Auth Service 启动类
 */
@SpringBootApplication(scanBasePackages = "single.cjj.bizfi")
public class CommonApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommonApplication.class, args);
        System.out.println("✅ Auth-Service 启动成功！");
    }
}
