package single.cjj.fi.accounting.integration;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import single.cjj.fi.accounting.service.PaymentCompletedAccountingService;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(
        prefix = "fi.accounting.payment-completed",
        name = "enabled",
        havingValue = "true"
)
public class PaymentCompletedAccountingConsumer {

    private final PaymentCompletedAccountingService accountingService;
    private final PaymentCompletedInboxFailureRecorder failureRecorder;

    public PaymentCompletedAccountingConsumer(
            PaymentCompletedAccountingService accountingService,
            PaymentCompletedInboxFailureRecorder failureRecorder
    ) {
        this.accountingService = accountingService;
        this.failureRecorder = failureRecorder;
    }

    @RabbitListener(
            queues = PaymentCompletedAccountingRabbitConfiguration.QUEUE,
            ackMode = "MANUAL"
    )
    public void consume(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String rawJson = new String(message.getBody(), StandardCharsets.UTF_8);
        String messageId = message.getMessageProperties().getMessageId();
        try {
            accountingService.process(rawJson);
            channel.basicAck(deliveryTag, false);
        } catch (Exception exception) {
            try {
                failureRecorder.record(rawJson, messageId, exception);
            } finally {
                channel.basicReject(deliveryTag, false);
            }
        }
    }
}
