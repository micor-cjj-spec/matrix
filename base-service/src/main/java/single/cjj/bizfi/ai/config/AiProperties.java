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
     * 是否启用基于 Embedding 的语义召回。首次部署需先执行 bizfi_ai_rag_v2.sql。
     */
    private Boolean semanticRetrievalEnabled = false;

    /**
     * Embedding 或向量解析失败时是否退回现有关键词检索。
     */
    private Boolean semanticFailOpen = true;

    /**
     * 单次语义检索最多在多少个已索引分片中计算相似度。
     * Phase 1 使用 MySQL JSON 向量和应用内余弦计算，后续迁移 PGVector。
     */
    private Integer semanticCandidateLimit = 500;

    /**
     * 语义结果最低余弦相似度。
     */
    private Double semanticMinScore = 0.50D;

    /**
     * 混合检索中关键词召回的权重。
     */
    private Double hybridKeywordWeight = 1.0D;

    /**
     * 混合检索中语义召回的权重。
     */
    private Double hybridSemanticWeight = 1.0D;

    /**
     * Reciprocal Rank Fusion 的平滑常数。
     */
    private Integer hybridRrfK = 60;

    /**
     * 知识分片生成 Embedding 时的批大小，最大不应超过 ai-service 的 32 条限制。
     */
    private Integer embeddingBatchSize = 16;

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

    /**
     * 高风险能力默认关闭，启用后仍需通过工具白名单和组织范围校验。
     */
    private Boolean toolCallingEnabled = false;

    /**
     * 仅供受控开发环境使用。生产环境应保持 false。
     */
    private Boolean toolAllowAllOrganizations = false;

    /**
     * 迁移期用户组织授权，格式为 userId:organizationId，多个值使用逗号分隔。
     * 生产环境应改用统一权限服务或 JWT 组织权限声明。
     */
    private String toolAllowedUserOrganizationPairs = "";

    /**
     * fi-service 内部审计 API 根地址，包含应用 context path。
     */
    private String financeAuditBaseUrl = "http://127.0.0.1:10003/api";

    /**
     * 查询工具执行审计时使用的独立内部令牌，不得与工具执行令牌复用。
     */
    private String financeAuditInternalToken;

    /**
     * 迁移期审计查看人用户 ID，多个值使用逗号分隔。
     * 平台 JWT 支持 Authority 后应迁移到 AI_TOOL_AUDIT_VIEW。
     */
    private String auditViewerUserIds = "";

    /**
     * 迁移期审计对账操作人用户 ID，多个值使用逗号分隔。
     * 平台 JWT 支持 Authority 后应迁移到 AI_TOOL_AUDIT_RECONCILE。
     */
    private String auditReconcilerUserIds = "";
}
