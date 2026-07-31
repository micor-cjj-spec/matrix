package single.cjj.bizfi.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "bizfi.ai")
public class AiProperties {

    /**
     * 模型适配器：prompt-http、legacy-http、spring-ai。
     */
    private String modelAdapter = "prompt-http";

    private Boolean enabled = true;
    private Boolean knowledgeEnabled = true;
    private Boolean fallbackEnabled = true;
    private Integer maxHistoryMessages = 20;
    private Integer maxKnowledgeChunks = 5;
    private Integer requestTimeoutSeconds = 60;

    /**
     * 独立 ai-service 的静态内部 API 根地址，包含应用 context path。
     */
    private String springAiBaseUrl = "http://127.0.0.1:10020/api";

    /**
     * Nacos/DiscoveryClient 中注册的服务 ID。
     */
    private String springAiServiceId = "ai-service";

    /**
     * 是否优先通过 DiscoveryClient 获取 ai-service 实例。
     */
    private Boolean springAiDiscoveryEnabled = false;

    /**
     * 服务发现没有可用实例时，是否允许使用静态地址。
     */
    private Boolean springAiStaticFallbackEnabled = true;

    /**
     * 一次请求最多尝试的端点数，包括首次调用。
     */
    private Integer springAiMaxAttempts = 2;

    /**
     * 连续失败达到该阈值后打开本地熔断器。
     */
    private Integer springAiCircuitFailureThreshold = 3;

    /**
     * 熔断器打开后的等待秒数。
     */
    private Integer springAiCircuitOpenSeconds = 30;

    /**
     * base-service 调用 ai-service 时使用的内部令牌。
     */
    private String internalToken;
}
