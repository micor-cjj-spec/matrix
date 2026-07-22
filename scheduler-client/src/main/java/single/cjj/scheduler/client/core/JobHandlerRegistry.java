package single.cjj.scheduler.client.core;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import single.cjj.scheduler.client.annotation.MatrixJobHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JobHandlerRegistry implements SmartInitializingSingleton {

    private final ApplicationContext applicationContext;
    private final Map<String, RegisteredHandler> handlers = new LinkedHashMap<>();

    public JobHandlerRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, MatrixJob> beans = applicationContext.getBeansOfType(MatrixJob.class);
        for (MatrixJob handler : beans.values()) {
            Class<?> targetClass = AopUtils.getTargetClass(handler);
            MatrixJobHandler annotation = AnnotationUtils.findAnnotation(targetClass, MatrixJobHandler.class);
            if (annotation == null) {
                continue;
            }
            String code = annotation.value().trim();
            if (code.isEmpty()) {
                throw new IllegalStateException("MatrixJobHandler code 不能为空: " + targetClass.getName());
            }
            RegisteredHandler previous = handlers.putIfAbsent(code,
                    new RegisteredHandler(code,
                            annotation.name().isBlank() ? targetClass.getSimpleName() : annotation.name(),
                            handler));
            if (previous != null) {
                throw new IllegalStateException("重复的调度处理器编码: " + code);
            }
        }
    }

    public MatrixJob required(String handlerCode) {
        RegisteredHandler registered = handlers.get(handlerCode);
        if (registered == null) {
            throw new IllegalArgumentException("未注册调度处理器: " + handlerCode);
        }
        return registered.handler();
    }

    public List<HandlerDescriptor> descriptors() {
        List<HandlerDescriptor> result = new ArrayList<>();
        handlers.values().forEach(item -> result.add(new HandlerDescriptor(item.code(), item.name())));
        return Collections.unmodifiableList(result);
    }

    private record RegisteredHandler(String code, String name, MatrixJob handler) { }

    public record HandlerDescriptor(String handlerCode, String handlerName) { }
}
