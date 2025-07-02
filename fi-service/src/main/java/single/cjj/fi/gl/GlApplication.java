package single.cjj.fi.gl;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GL 模块启动类
 */
@SpringBootApplication(scanBasePackages = "single.cjj.fi.gl")
@MapperScan(basePackages = "single.cjj.fi.gl.mapper")
public class GlApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlApplication.class, args);
        System.out.println("✅ GL Service started!");
    }
}
