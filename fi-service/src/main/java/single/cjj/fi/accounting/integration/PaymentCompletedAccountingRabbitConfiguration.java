package single.cjj.fi.accounting.integration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentCompletedAccountingRabbitConfiguration {

    public static final String QUEUE = "matrix.fi.payment-completed-accounting";
    public static final String ROUTING_KEY = "biz.finance.payment.completed";
    public static final String DEAD_QUEUE = "matrix.fi.payment-completed-accounting.dead";
    public static final String DEAD_ROUTING_KEY = "fi.payment-completed-accounting.dead";

    @Bean
    Queue paymentCompletedAccountingQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(PurchaseInboundAccountingRabbitConfiguration.DEAD_EXCHANGE)
                .deadLetterRoutingKey(DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue paymentCompletedAccountingDeadQueue() {
        return QueueBuilder.durable(DEAD_QUEUE).build();
    }

    @Bean
    Binding paymentCompletedAccountingBinding(
            @Qualifier("paymentCompletedAccountingQueue") Queue queue,
            @Qualifier("fiBusinessEventExchange") TopicExchange exchange
    ) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    @Bean
    Binding paymentCompletedAccountingDeadBinding(
            @Qualifier("paymentCompletedAccountingDeadQueue") Queue queue,
            @Qualifier("fiBusinessEventDeadExchange") DirectExchange exchange
    ) {
        return BindingBuilder.bind(queue).to(exchange).with(DEAD_ROUTING_KEY);
    }
}
