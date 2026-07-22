package single.cjj.scheduler.client.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import single.cjj.scheduler.client.config.SchedulerClientProperties;
import single.cjj.scheduler.client.model.SchedulerExecutionMessage;

public class SchedulerMessageListener {

    private static final Logger log = LoggerFactory.getLogger(SchedulerMessageListener.class);

    private final ObjectMapper objectMapper;
    private final SchedulerClientProperties properties;
    private final JobHandlerRegistry handlerRegistry;
    private final ExecutionRecordRepository recordRepository;
    private final SchedulerCallbackClient callbackClient;
    private final SchedulerInstanceIdentity instanceIdentity;

    public SchedulerMessageListener(ObjectMapper objectMapper,
                                    SchedulerClientProperties properties,
                                    JobHandlerRegistry handlerRegistry,
                                    ExecutionRecordRepository recordRepository,
                                    SchedulerCallbackClient callbackClient,
                                    SchedulerInstanceIdentity instanceIdentity) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.handlerRegistry = handlerRegistry;
        this.recordRepository = recordRepository;
        this.callbackClient = callbackClient;
        this.instanceIdentity = instanceIdentity;
    }

    @RabbitListener(queues = "#{schedulerClientQueue.name}",
            concurrency = "${matrix.scheduler.client.concurrency:1}")
    public void consume(Message rabbitMessage) {
        SchedulerExecutionMessage message = parse(rabbitMessage.getBody());
        if (!properties.requiredExecutorCode().equals(message.getExecutorCode())) {
            throw new IllegalArgumentException("消息执行器不匹配: " + message.getExecutorCode());
        }

        ExecutionRecordRepository.StartResult start = recordRepository.tryStart(
                message.getExecutionNo(), message.getHandlerCode());
        if (!start.started()) {
            log.info("skip duplicated scheduler message, executionNo={}, existingStatus={}",
                    message.getExecutionNo(), start.existingStatus());
            return;
        }

        String instanceId = instanceIdentity.getInstanceId();
        callbackClient.running(message.getExecutionNo(), instanceId);
        callbackClient.progress(message.getExecutionNo(), instanceId, 0, "STARTING", "任务开始执行");
        try {
            MatrixJob handler = handlerRegistry.required(message.getHandlerCode());
            JobContext context = new JobContext(
                    message.getExecutionNo(),
                    message.getTraceId(),
                    message.getJobCode(),
                    message.getHandlerCode(),
                    message.getAttemptNo() == null ? 1 : message.getAttemptNo(),
                    message.getParameters(),
                    objectMapper,
                    (progress, stage, progressMessage) -> callbackClient.progress(
                            message.getExecutionNo(), instanceId, progress, stage, progressMessage));
            JobResult result = handler.execute(context);
            if (result == null) {
                result = JobResult.success();
            }
            String resultJson = objectMapper.writeValueAsString(result);
            if (result.success()) {
                callbackClient.progress(message.getExecutionNo(), instanceId, 100, "FINISHED", "任务执行完成");
                recordRepository.markSuccess(message.getExecutionNo(), resultJson);
                callbackClient.success(message.getExecutionNo(), instanceId, resultJson);
            } else {
                recordRepository.markFailed(message.getExecutionNo(), result.message());
                callbackClient.failed(message.getExecutionNo(), instanceId, result.code(), result.message());
            }
        } catch (Exception e) {
            log.error("scheduler handler failed, executionNo={}, handlerCode={}",
                    message.getExecutionNo(), message.getHandlerCode(), e);
            recordRepository.markFailed(message.getExecutionNo(), e.getMessage());
            callbackClient.failed(message.getExecutionNo(), instanceId,
                    "HANDLER_EXCEPTION", trim(e.getMessage()));
        }
    }

    private SchedulerExecutionMessage parse(byte[] payload) {
        try {
            return objectMapper.readValue(payload, SchedulerExecutionMessage.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("无法解析调度消息", e);
        }
    }

    private String trim(String value) {
        if (value == null || value.length() <= 2000) {
            return value;
        }
        return value.substring(0, 2000);
    }
}
