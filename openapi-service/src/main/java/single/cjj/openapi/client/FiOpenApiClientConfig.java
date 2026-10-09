package single.cjj.openapi.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

public class FiOpenApiClientConfig {

    public static final String INTERNAL_TOKEN_HEADER = "X-Matrix-Internal-Token";

    @Bean
    public RequestInterceptor fiOpenApiInternalTokenInterceptor(
            @Value("${matrix.openapi.internal-token:}") String internalToken) {
        return requestTemplate -> {
            if (!StringUtils.hasText(internalToken)) {
                throw new IllegalStateException("必须配置 MATRIX_INTERNAL_OPENAPI_TOKEN");
            }
            requestTemplate.header(INTERNAL_TOKEN_HEADER, internalToken);
        };
    }
}
