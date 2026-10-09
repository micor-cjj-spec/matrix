package single.cjj.scheduler.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerRabbitConfig {

    @Bean
    public TopicExchange schedulerExchange(
            @Value("${matrix.scheduler.exchange:matrix.scheduler.execute}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }
}
