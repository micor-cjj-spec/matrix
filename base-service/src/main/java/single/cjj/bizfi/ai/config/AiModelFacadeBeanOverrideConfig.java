package single.cjj.bizfi.ai.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModelFacadeBeanOverrideConfig implements BeanDefinitionRegistryPostProcessor {

    private static final String LEGACY_BEAN_NAME = "primaryAiModelFacade";

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (registry.containsBeanDefinition(LEGACY_BEAN_NAME)) {
            registry.removeBeanDefinition(LEGACY_BEAN_NAME);
            return;
        }
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            if (LEGACY_BEAN_NAME.equals(beanDefinition.getBeanClassName())) {
                registry.removeBeanDefinition(beanName);
                return;
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // no-op
    }
}
