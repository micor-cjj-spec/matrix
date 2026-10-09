package single.cjj.fi.accounting.integration;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import single.cjj.fi.accounting.service.PurchaseInboundAccountingService;

import java.nio.charset.StandardCharsets;

@Component
public class PurchaseInboundAccountingConsumer {

    private final PurchaseInboundAccountingService accountingService;
    private final PurchaseInboundInboxFailureRecorder failureRecorder;

    public PurchaseInboundAccountingConsumer(
            PurchaseInboundAccountingService accountingService,
            PurchaseInboundInboxFailureRecorder failureRecorder
    ) {
        this.accountingService = accountingService;
        this.failureRecorder = failureRecorder;
    }

    @RabbitListener(
            queues = PurchaseInboundAccountingRabbitConfiguration.PURCHASE_INBOUND_QUEUE,
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
                // no broker requeue loop; the queue DLX moves the message to the FI dead-letter queue.
                channel.basicReject(deliveryTag, false);
            }
        }
    }
}
