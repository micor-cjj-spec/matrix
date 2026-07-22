package single.cjj.scheduler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import single.cjj.scheduler.entity.MatrixSchedulerExecution;
import single.cjj.scheduler.entity.MatrixSchedulerOutbox;
import single.cjj.scheduler.mapper.MatrixSchedulerExecutionMapper;
import single.cjj.scheduler.mapper.MatrixSchedulerOutboxMapper;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxPublisher {

    private final MatrixSchedulerOutboxMapper outboxMapper;
    private final MatrixSchedulerExecutionMapper executionMapper;
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final int batchSize;
    private final int maxRetry;

    public OutboxPublisher(MatrixSchedulerOutboxMapper outboxMapper,
                           MatrixSchedulerExecutionMapper executionMapper,
                           RabbitTemplate rabbitTemplate,
                           @Value("${matrix.scheduler.exchange:matrix.scheduler.execute}") String exchange,
                           @Value("${matrix.scheduler.outbox.batch-size:100}") int batchSize,
                           @Value("${matrix.scheduler.outbox.max-retry:10}") int maxRetry) {
        this.outboxMapper = outboxMapper;
        this.executionMapper = executionMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.batchSize = Math.max(1, Math.min(batchSize, 500));
        this.maxRetry = Math.max(1, maxRetry);
    }

    @Scheduled(fixedDelayString = "${matrix.scheduler.outbox.publish-delay-ms:3000}")
    public void publishPending() {
        LocalDateTime now = LocalDateTime.now();
        List<MatrixSchedulerOutbox> events = outboxMapper.selectList(
                new LambdaQueryWrapper<MatrixSchedulerOutbox>()
                        .in(MatrixSchedulerOutbox::getFstatus, "PENDING", "FAILED")
                        .and(w -> w.isNull(MatrixSchedulerOutbox::getFnextRetryTime)
                                .or()
                                .le(MatrixSchedulerOutbox::getFnextRetryTime, now))
                        .orderByAsc(MatrixSchedulerOutbox::getFcreateTime)
                        .last("LIMIT " + batchSize));

        for (MatrixSchedulerOutbox event : events) {
            publishOne(event);
        }
    }

    private void publishOne(MatrixSchedulerOutbox event) {
        try {
            rabbitTemplate.convertAndSend(exchange, event.getFroutingKey(), event.getFpayload(), message -> {
                message.getMessageProperties().setMessageId(event.getFeventId());
                message.getMessageProperties().setContentType("application/json");
                return message;
            });
            event.setFstatus("SENT");
            event.setFnextRetryTime(null);
            event.setFlastError(null);
            event.setFupdateTime(LocalDateTime.now());
            outboxMapper.updateById(event);
            markExecutionQueued(event.getFaggregateId());
        } catch (Exception e) {
            int retries = event.getFretryCount() == null ? 1 : event.getFretryCount() + 1;
            event.setFretryCount(retries);
            event.setFstatus(retries >= maxRetry ? "DEAD" : "FAILED");
            event.setFnextRetryTime(retries >= maxRetry
                    ? null
                    : LocalDateTime.now().plusSeconds(backoffSeconds(retries)));
            event.setFlastError(trim(e.getMessage()));
            event.setFupdateTime(LocalDateTime.now());
            outboxMapper.updateById(event);
        }
    }

    private void markExecutionQueued(String executionNo) {
        MatrixSchedulerExecution execution = executionMapper.selectOne(
                new LambdaQueryWrapper<MatrixSchedulerExecution>()
                        .eq(MatrixSchedulerExecution::getFexecutionNo, executionNo)
                        .last("LIMIT 1"));
        if (execution != null && "CREATED".equals(execution.getFstatus())) {
            execution.setFstatus("QUEUED");
            execution.setFupdateTime(LocalDateTime.now());
            executionMapper.updateById(execution);
        }
    }

    private long backoffSeconds(int retry) {
        return Math.min(3600L, 30L * (1L << Math.min(retry - 1, 6)));
    }

    private String trim(String message) {
        if (message == null || message.length() <= 2000) {
            return message;
        }
        return message.substring(0, 2000);
    }
}
