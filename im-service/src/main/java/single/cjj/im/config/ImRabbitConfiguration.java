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

    @Bean
    DirectExchange imEventExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue channelTaskQueue() {
        return QueueBuilder.durable(CHANNEL_TASK_QUEUE).build();
    }

    @Bean
    Binding channelTaskBinding(DirectExchange imEventExchange, Queue channelTaskQueue) {
        return BindingBuilder.bind(channelTaskQueue).to(imEventExchange).with(CHANNEL_TASK_ROUTING_KEY);
    }
}
