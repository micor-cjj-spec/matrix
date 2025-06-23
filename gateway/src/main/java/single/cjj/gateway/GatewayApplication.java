package single.cjj.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("\u2705 Gateway \u542f\u52a8\u6210\u529f\uff01");
    }
}
