package single.cjj.erp.event.config;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessEventRabbitConfiguration {

    public static final String BUSINESS_EVENT_EXCHANGE = "matrix.business.events";

    @Bean
    TopicExchange businessEventExchange() {
        return ExchangeBuilder.topicExchange(BUSINESS_EVENT_EXCHANGE)
                .durable(true)
                .build();
    }
}
