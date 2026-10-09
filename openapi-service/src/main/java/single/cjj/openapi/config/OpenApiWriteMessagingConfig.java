package single.cjj.openapi.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiWriteMessagingConfig {

    private final String exchangeName;
    private final String queueName;
    private final String routingKey;

    public OpenApiWriteMessagingConfig(
            @Value("${matrix.openapi.write.exchange:matrix.openapi.write.exchange}") String exchangeName,
            @Value("${matrix.openapi.write.queue:matrix.openapi.voucher.write.queue}") String queueName,
            @Value("${matrix.openapi.write.routing-key:matrix.openapi.voucher.write}") String routingKey) {
        this.exchangeName = exchangeName;
        this.queueName = queueName;
        this.routingKey = routingKey;
    }

    @Bean
    public DirectExchange openApiWriteExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue openApiVoucherWriteQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding openApiVoucherWriteBinding(Queue openApiVoucherWriteQueue,
                                               DirectExchange openApiWriteExchange) {
        return BindingBuilder.bind(openApiVoucherWriteQueue)
                .to(openApiWriteExchange)
                .with(routingKey);
    }
}
