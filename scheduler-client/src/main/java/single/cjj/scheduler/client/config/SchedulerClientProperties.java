package single.cjj.scheduler.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "matrix.scheduler.client")
public class SchedulerClientProperties {

    private boolean enabled = true;
    private String executorCode;
    private String executorName;
    private String schedulerBaseUrl = "http://127.0.0.1:8080/api";
    private String exchange = "matrix.scheduler.execute";
    private String queuePrefix = "matrix.scheduler.executor";
    private int concurrency = 1;
    private int maxConcurrency = 10;
    private long heartbeatIntervalMs = 30000;
    private boolean initializeSchema = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getExecutorCode() {
        return executorCode;
    }

    public void setExecutorCode(String executorCode) {
        this.executorCode = executorCode;
    }

    public String getExecutorName() {
        return executorName;
    }

    public void setExecutorName(String executorName) {
        this.executorName = executorName;
    }

    public String getSchedulerBaseUrl() {
        return schedulerBaseUrl;
    }

    public void setSchedulerBaseUrl(String schedulerBaseUrl) {
        this.schedulerBaseUrl = schedulerBaseUrl;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getQueuePrefix() {
        return queuePrefix;
    }

    public void setQueuePrefix(String queuePrefix) {
        this.queuePrefix = queuePrefix;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    public long getHeartbeatIntervalMs() {
        return heartbeatIntervalMs;
    }

    public void setHeartbeatIntervalMs(long heartbeatIntervalMs) {
        this.heartbeatIntervalMs = heartbeatIntervalMs;
    }

    public boolean isInitializeSchema() {
        return initializeSchema;
    }

    public void setInitializeSchema(boolean initializeSchema) {
        this.initializeSchema = initializeSchema;
    }

    public String requiredExecutorCode() {
        if (executorCode == null || executorCode.isBlank()) {
            throw new IllegalStateException("matrix.scheduler.client.executor-code 不能为空");
        }
        return executorCode.trim();
    }

    public String resolvedExecutorName() {
        return executorName == null || executorName.isBlank() ? requiredExecutorCode() : executorName.trim();
    }

    public String queueName() {
        return queuePrefix + "." + requiredExecutorCode();
    }
}
