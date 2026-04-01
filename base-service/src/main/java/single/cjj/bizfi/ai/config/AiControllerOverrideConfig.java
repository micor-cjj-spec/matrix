package single.cjj.bizfi.ai.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiControllerOverrideConfig implements BeanDefinitionRegistryPostProcessor {

    private static final String LEGACY_BEAN_NAME = "aiAssistantController";
    private static final String LEGACY_CLASS_NAME = "single.cjj.bizfi.controller.AiAssistantController";

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (registry.containsBeanDefinition(LEGACY_BEAN_NAME)) {
            registry.removeBeanDefinition(LEGACY_BEAN_NAME);
            return;
        }
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            if (LEGACY_CLASS_NAME.equals(beanDefinition.getBeanClassName())) {
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
