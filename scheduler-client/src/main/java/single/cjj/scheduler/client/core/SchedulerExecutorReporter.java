package single.cjj.scheduler.client.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;
import single.cjj.scheduler.client.config.SchedulerClientProperties;

import java.util.List;

public class SchedulerExecutorReporter {

    private static final Logger log = LoggerFactory.getLogger(SchedulerExecutorReporter.class);

    private final RestClient restClient;
    private final SchedulerClientProperties properties;
    private final JobHandlerRegistry handlerRegistry;
    private final SchedulerInstanceIdentity identity;

    public SchedulerExecutorReporter(RestClient.Builder builder,
                                     SchedulerClientProperties properties,
                                     JobHandlerRegistry handlerRegistry,
                                     SchedulerInstanceIdentity identity) {
        this.restClient = builder.baseUrl(trimTrailingSlash(properties.getSchedulerBaseUrl())).build();
        this.properties = properties;
        this.handlerRegistry = handlerRegistry;
        this.identity = identity;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerWhenReady() {
        register();
    }

    @Scheduled(initialDelayString = "${matrix.scheduler.client.heartbeat-interval-ms:30000}",
            fixedDelayString = "${matrix.scheduler.client.heartbeat-interval-ms:30000}")
    public void heartbeat() {
        try {
            restClient.post()
                    .uri("/scheduler/executors/heartbeat")
                    .body(new HeartbeatRequest(
                            properties.requiredExecutorCode(),
                            identity.getInstanceId(),
                            properties.getMaxConcurrency()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("scheduler executor heartbeat failed: {}", e.getMessage());
        }
    }

    private void register() {
        try {
            List<HandlerRequest> handlers = handlerRegistry.descriptors().stream()
                    .map(item -> new HandlerRequest(item.handlerCode(), item.handlerName()))
                    .toList();
            restClient.post()
                    .uri("/scheduler/executors/register")
                    .body(new RegisterRequest(
                            properties.requiredExecutorCode(),
                            properties.resolvedExecutorName(),
                            identity.getInstanceId(),
                            properties.getMaxConcurrency(),
                            handlers))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("scheduler executor registration failed: {}", e.getMessage());
        }
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record RegisterRequest(String executorCode,
                                   String executorName,
                                   String instanceId,
                                   Integer maxConcurrency,
                                   List<HandlerRequest> handlers) { }

    private record HandlerRequest(String handlerCode, String handlerName) { }

    private record HeartbeatRequest(String executorCode,
                                    String instanceId,
                                    Integer runningCount) { }
}
