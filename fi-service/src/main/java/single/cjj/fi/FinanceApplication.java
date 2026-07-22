package single.cjj.fi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableFeignClients(basePackages = "single.cjj.fi.integration.botp")
@SpringBootApplication(scanBasePackages = "single.cjj.fi")
@MapperScan(basePackages = "single.cjj.fi.**.mapper")
public class FinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
        System.out.println("✅ Finance Service started!");
    }
}
