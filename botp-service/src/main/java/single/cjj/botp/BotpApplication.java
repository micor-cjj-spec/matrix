package single.cjj.botp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"single.cjj.botp", "single.cjj.bizfi"})
public class BotpApplication {

    public static void main(String[] args) {
        SpringApplication.run(BotpApplication.class, args);
    }
}
