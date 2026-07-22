package single.cjj.scheduler.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;
import single.cjj.scheduler.client.core.ExecutionRecordRepository;
import single.cjj.scheduler.client.core.JobHandlerRegistry;
import single.cjj.scheduler.client.core.SchedulerCallbackClient;
import single.cjj.scheduler.client.core.SchedulerExecutorReporter;
import single.cjj.scheduler.client.core.SchedulerInstanceIdentity;
import single.cjj.scheduler.client.core.SchedulerMessageListener;

@EnableScheduling
@AutoConfiguration
@EnableConfigurationProperties(SchedulerClientProperties.class)
@ConditionalOnProperty(prefix = "matrix.scheduler.client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder schedulerRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public TopicExchange schedulerClientExchange(SchedulerClientProperties properties) {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    @Bean(name = "schedulerClientQueue")
    public Queue schedulerClientQueue(SchedulerClientProperties properties) {
        return new Queue(properties.queueName(), true, false, false);
    }

    @Bean
    public Binding schedulerClientBinding(
            @Qualifier("schedulerClientQueue") Queue schedulerClientQueue,
            TopicExchange schedulerClientExchange,
            SchedulerClientProperties properties) {
        return BindingBuilder.bind(schedulerClientQueue)
                .to(schedulerClientExchange)
                .with("scheduler.execute." + properties.requiredExecutorCode());
    }

    @Bean
    public JobHandlerRegistry schedulerJobHandlerRegistry(ApplicationContext applicationContext) {
        return new JobHandlerRegistry(applicationContext);
    }

    @Bean
    public SchedulerInstanceIdentity schedulerInstanceIdentity(Environment environment) {
        return new SchedulerInstanceIdentity(environment);
    }

    @Bean
    public ExecutionRecordRepository schedulerExecutionRecordRepository(JdbcTemplate jdbcTemplate) {
        return new ExecutionRecordRepository(jdbcTemplate);
    }

    @Bean
    public ApplicationRunner schedulerClientSchemaInitializer(ExecutionRecordRepository repository,
                                                               SchedulerClientProperties properties) {
        return arguments -> {
            if (properties.isInitializeSchema()) {
                repository.initializeSchema();
            }
        };
    }

    @Bean
    public SchedulerCallbackClient schedulerCallbackClient(RestClient.Builder builder,
                                                           SchedulerClientProperties properties) {
        return new SchedulerCallbackClient(builder, properties);
    }

    @Bean
    public SchedulerMessageListener schedulerMessageListener(ObjectMapper objectMapper,
                                                             SchedulerClientProperties properties,
                                                             JobHandlerRegistry registry,
                                                             ExecutionRecordRepository repository,
                                                             SchedulerCallbackClient callbackClient,
                                                             SchedulerInstanceIdentity identity) {
        return new SchedulerMessageListener(objectMapper, properties, registry, repository, callbackClient, identity);
    }

    @Bean
    public SchedulerExecutorReporter schedulerExecutorReporter(RestClient.Builder builder,
                                                               SchedulerClientProperties properties,
                                                               JobHandlerRegistry registry,
                                                               SchedulerInstanceIdentity identity) {
        return new SchedulerExecutorReporter(builder, properties, registry, identity);
    }
}
