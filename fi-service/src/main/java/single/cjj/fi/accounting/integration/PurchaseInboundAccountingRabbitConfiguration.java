package single.cjj.fi.accounting.integration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PurchaseInboundAccountingRabbitConfiguration {

    public static final String BUSINESS_EVENT_EXCHANGE = "matrix.business.events";
    public static final String PURCHASE_INBOUND_QUEUE = "matrix.fi.purchase-inbound-accounting";
    public static final String PURCHASE_INBOUND_ROUTING_KEY = "biz.procurement.purchase_inbound.confirmed";
    public static final String DEAD_EXCHANGE = "matrix.fi.business-event.dead";
    public static final String DEAD_QUEUE = "matrix.fi.purchase-inbound-accounting.dead";
    public static final String DEAD_ROUTING_KEY = "fi.purchase-inbound-accounting.dead";

    @Bean
    TopicExchange fiBusinessEventExchange() {
        return ExchangeBuilder.topicExchange(BUSINESS_EVENT_EXCHANGE).durable(true).build();
    }

    @Bean
    DirectExchange fiBusinessEventDeadExchange() {
        return ExchangeBuilder.directExchange(DEAD_EXCHANGE).durable(true).build();
    }

    @Bean
    Queue purchaseInboundAccountingQueue() {
        return QueueBuilder.durable(PURCHASE_INBOUND_QUEUE)
                .deadLetterExchange(DEAD_EXCHANGE)
                .deadLetterRoutingKey(DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue purchaseInboundAccountingDeadQueue() {
        return QueueBuilder.durable(DEAD_QUEUE).build();
    }

    @Bean
    Binding purchaseInboundAccountingBinding(
            @Qualifier("purchaseInboundAccountingQueue") Queue queue,
            TopicExchange fiBusinessEventExchange
    ) {
        return BindingBuilder.bind(queue)
                .to(fiBusinessEventExchange)
                .with(PURCHASE_INBOUND_ROUTING_KEY);
    }

    @Bean
    Binding purchaseInboundAccountingDeadBinding(
            @Qualifier("purchaseInboundAccountingDeadQueue") Queue queue,
            DirectExchange fiBusinessEventDeadExchange
    ) {
        return BindingBuilder.bind(queue)
                .to(fiBusinessEventDeadExchange)
                .with(DEAD_ROUTING_KEY);
    }
}
