package single.cjj.fi.accounting.integration;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PurchaseDeductionAccountingRabbitConfiguration {

    public static final String QUEUE = "matrix.fi.purchase-deduction-accounting";
    public static final String ROUTING_KEY = "biz.procurement.purchase_deduction.confirmed";
    public static final String DEAD_QUEUE = "matrix.fi.purchase-deduction-accounting.dead";
    public static final String DEAD_ROUTING_KEY = "fi.purchase-deduction-accounting.dead";

    @Bean
    Queue purchaseDeductionAccountingQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(PurchaseInboundAccountingRabbitConfiguration.DEAD_EXCHANGE)
                .deadLetterRoutingKey(DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue purchaseDeductionAccountingDeadQueue() {
        return QueueBuilder.durable(DEAD_QUEUE).build();
    }

    @Bean
    Binding purchaseDeductionAccountingBinding(
            @Qualifier("purchaseDeductionAccountingQueue") Queue queue,
            @Qualifier("fiBusinessEventExchange") TopicExchange exchange
    ) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    @Bean
    Binding purchaseDeductionAccountingDeadBinding(
            @Qualifier("purchaseDeductionAccountingDeadQueue") Queue queue,
            @Qualifier("fiBusinessEventDeadExchange") DirectExchange exchange
    ) {
        return BindingBuilder.bind(queue).to(exchange).with(DEAD_ROUTING_KEY);
    }
}
