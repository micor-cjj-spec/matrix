package single.cjj.scheduler.client.model;

import lombok.Data;

@Data
public class SchedulerExecutionMessage {

    private String executionNo;
    private String traceId;
    private Long jobId;
    private String jobCode;
    private String executorCode;
    private String handlerCode;
    private Integer attemptNo;
    private Integer timeoutSeconds;
    private String parameters;
}
