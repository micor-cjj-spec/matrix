package single.cjj.bizfi.ai.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import single.cjj.bizfi.ai.config.AiProperties;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiServiceEndpointResolverTest {

    @Test
    void shouldUseDiscoveredInstancesBeforeStaticFallback() {
        AiProperties properties = new AiProperties();
        properties.setSpringAiDiscoveryEnabled(true);
        properties.setSpringAiStaticFallbackEnabled(true);
        properties.setSpringAiBaseUrl("http://static:10020/api");

        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        ServiceInstance first = instance("http://ai-1:10020", Map.of("matrix.context-path", "/api"));
        ServiceInstance second = instance("http://ai-2:10020", Map.of());
        when(discoveryClient.getInstances("ai-service")).thenReturn(List.of(first, second));

        @SuppressWarnings("unchecked")
        ObjectProvider<DiscoveryClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(discoveryClient);

        AiServiceEndpointResolver resolver = new AiServiceEndpointResolver(properties, provider);

        assertEquals(
                List.of(
                        URI.create("http://ai-1:10020/api"),
                        URI.create("http://ai-2:10020/api"),
                        URI.create("http://static:10020/api")
                ),
                resolver.resolveCandidates()
        );
        assertEquals(
                List.of(
                        URI.create("http://ai-2:10020/api"),
                        URI.create("http://static:10020/api"),
                        URI.create("http://ai-1:10020/api")
                ),
                resolver.resolveCandidates()
        );
    }

    @Test
    void shouldUseStaticAddressWhenDiscoveryIsDisabled() {
        AiProperties properties = new AiProperties();
        properties.setSpringAiDiscoveryEnabled(false);
        properties.setSpringAiBaseUrl("http://127.0.0.1:10020/api/");

        @SuppressWarnings("unchecked")
        ObjectProvider<DiscoveryClient> provider = mock(ObjectProvider.class);
        AiServiceEndpointResolver resolver = new AiServiceEndpointResolver(properties, provider);

        assertEquals(
                List.of(URI.create("http://127.0.0.1:10020/api")),
                resolver.resolveCandidates()
        );
    }

    private ServiceInstance instance(String uri, Map<String, String> metadata) {
        ServiceInstance instance = mock(ServiceInstance.class);
        when(instance.getUri()).thenReturn(URI.create(uri));
        when(instance.getMetadata()).thenReturn(metadata);
        return instance;
    }
}
