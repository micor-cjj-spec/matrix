package single.cjj.botp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableFeignClients(basePackages = "single.cjj.botp.integration.fi")
@MapperScan("single.cjj.botp.persistence.mapper")
@SpringBootApplication(scanBasePackages = {"single.cjj.botp", "single.cjj.bizfi"})
public class BotpApplication {

    public static void main(String[] args) {
        SpringApplication.run(BotpApplication.class, args);
    }
}
