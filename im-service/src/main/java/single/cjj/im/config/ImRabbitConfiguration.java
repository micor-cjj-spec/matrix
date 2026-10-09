package single.cjj.im.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImRabbitConfiguration {

    public static final String EXCHANGE = "matrix.im.event";
    public static final String CHANNEL_TASK_QUEUE = "matrix.im.channel-task.queue";
    public static final String CHANNEL_TASK_ROUTING_KEY = "im.channel-task.dispatch";

    public static final String DEAD_LETTER_EXCHANGE = "matrix.im.dead-letter";
    public static final String CHANNEL_TASK_DLQ = "matrix.im.channel-task.dlq";
    public static final String CHANNEL_TASK_DEAD_ROUTING_KEY = "im.channel-task.dead";

    @Bean
    DirectExchange imEventExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange imDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue channelTaskQueue() {
        return QueueBuilder.durable(CHANNEL_TASK_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(CHANNEL_TASK_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue channelTaskDeadLetterQueue() {
        return QueueBuilder.durable(CHANNEL_TASK_DLQ).build();
    }

    @Bean
    Binding channelTaskBinding(DirectExchange imEventExchange, Queue channelTaskQueue) {
        return BindingBuilder.bind(channelTaskQueue).to(imEventExchange).with(CHANNEL_TASK_ROUTING_KEY);
    }

    @Bean
    Binding channelTaskDeadLetterBinding(DirectExchange imDeadLetterExchange,
                                         Queue channelTaskDeadLetterQueue) {
        return BindingBuilder.bind(channelTaskDeadLetterQueue)
                .to(imDeadLetterExchange)
                .with(CHANNEL_TASK_DEAD_ROUTING_KEY);
    }
}
