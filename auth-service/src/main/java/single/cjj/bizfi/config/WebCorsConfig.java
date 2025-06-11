package single.cjj.bizfi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**") // 配置需要跨域的路径
//                .allowedOrigins("http://localhost:5173") // 允许前端的 Origin
//                .allowedMethods("*") // 允许哪些方法（GET、POST等）
//                .allowedHeaders("*") // 允许的请求头
//                .allowCredentials(true) // 是否允许携带 Cookie
//                .maxAge(3600); // 缓存时间
//    }
//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        // 前端打包的静态资源路径
//        registry.addResourceHandler("/**")
//                .addResourceLocations("classpath:/static/")
//                .resourceChain(true);
//    }

}
