package single.cjj.bizfi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Base Service 启动类
 */
@SpringBootApplication(scanBasePackages = "single.cjj.bizfi")
@MapperScan(basePackages = "single.cjj.bizfi.mapper") // 推荐标准写法，防止扫描不到 MyBatis-Plus Mapper
public class BaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(BaseApplication.class, args);
        System.out.println("✅ Base-Service 启动成功！");
    }
}
