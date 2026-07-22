package single.cjj.scheduler.client.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import single.cjj.scheduler.client.config.SchedulerClientProperties;

public class SchedulerCallbackClient {

    private static final Logger log = LoggerFactory.getLogger(SchedulerCallbackClient.class);

    private final RestClient restClient;
    private final SchedulerClientProperties properties;

    public SchedulerCallbackClient(RestClient.Builder builder,
                                   SchedulerClientProperties properties) {
        this.restClient = builder.baseUrl(trimTrailingSlash(properties.getSchedulerBaseUrl())).build();
        this.properties = properties;
    }

    public void running(String executionNo, String instanceId) {
        callback(executionNo, new CallbackRequest("RUNNING", instanceId, null, null, null));
    }

    public void success(String executionNo, String instanceId, String responsePayload) {
        callback(executionNo, new CallbackRequest("SUCCESS", instanceId, responsePayload, null, null));
    }

    public void failed(String executionNo, String instanceId, String errorCode, String errorMessage) {
        callback(executionNo, new CallbackRequest("FAILED", instanceId, null, errorCode, errorMessage));
    }

    private void callback(String executionNo, CallbackRequest request) {
        try {
            restClient.post()
                    .uri("/scheduler/callback/executions/{executionNo}", executionNo)
                    .header("X-Executor-Code", properties.requiredExecutorCode())
                    .header("X-Scheduler-Internal-Token", properties.requiredInternalToken())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("scheduler callback failed, executionNo={}, status={}",
                    executionNo, request.status(), e);
        }
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record CallbackRequest(String status,
                                   String executorInstance,
                                   String responsePayload,
                                   String errorCode,
                                   String errorMessage) { }
}
