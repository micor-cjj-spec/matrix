package single.cjj.erp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableFeignClients(basePackages = "single.cjj.erp.integration")
@SpringBootApplication(scanBasePackages = "single.cjj.erp")
@MapperScan("single.cjj.erp.**.mapper")
public class ErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);
        System.out.println("✅ ERP Service started!");
    }
}