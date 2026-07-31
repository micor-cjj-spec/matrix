package single.cjj.bizfi.ai.service.impl;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiProperties;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Resolves candidate ai-service base URIs from service discovery and static configuration.
 */
@Component
public class AiServiceEndpointResolver {

    static final String CONTEXT_PATH_METADATA = "matrix.context-path";

    private final AiProperties properties;
    private final ObjectProvider<DiscoveryClient> discoveryClientProvider;
    private final AtomicInteger cursor = new AtomicInteger();

    public AiServiceEndpointResolver(
            AiProperties properties,
            ObjectProvider<DiscoveryClient> discoveryClientProvider
    ) {
        this.properties = properties;
        this.discoveryClientProvider = discoveryClientProvider;
    }

    public List<URI> resolveCandidates() {
        Map<String, URI> candidates = new LinkedHashMap<>();
        if (Boolean.TRUE.equals(properties.getSpringAiDiscoveryEnabled())) {
            addDiscoveredCandidates(candidates);
        }
        if (Boolean.TRUE.equals(properties.getSpringAiStaticFallbackEnabled()) || candidates.isEmpty()) {
            addStaticCandidate(candidates);
        }
        if (candidates.isEmpty()) {
            throw new IllegalStateException("没有可用的 ai-service 地址");
        }
        return rotate(new ArrayList<>(candidates.values()));
    }

    private void addDiscoveredCandidates(Map<String, URI> candidates) {
        DiscoveryClient discoveryClient = discoveryClientProvider.getIfAvailable();
        if (discoveryClient == null || !StringUtils.hasText(properties.getSpringAiServiceId())) {
            return;
        }
        List<ServiceInstance> instances = discoveryClient.getInstances(properties.getSpringAiServiceId());
        if (instances == null) {
            return;
        }
        for (ServiceInstance instance : instances) {
            if (instance == null || instance.getUri() == null) {
                continue;
            }
            String contextPath = instance.getMetadata() == null
                    ? null
                    : instance.getMetadata().get(CONTEXT_PATH_METADATA);
            URI uri = normalize(instance.getUri().toString(), contextPath);
            candidates.putIfAbsent(uri.toString(), uri);
        }
    }

    private void addStaticCandidate(Map<String, URI> candidates) {
        if (!StringUtils.hasText(properties.getSpringAiBaseUrl())) {
            return;
        }
        URI uri = normalize(properties.getSpringAiBaseUrl(), null);
        candidates.putIfAbsent(uri.toString(), uri);
    }

    private URI normalize(String baseUrl, String contextPath) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        String resolvedContextPath = StringUtils.hasText(contextPath) ? contextPath.trim() : "/api";
        if (!resolvedContextPath.startsWith("/")) {
            resolvedContextPath = "/" + resolvedContextPath;
        }
        while (resolvedContextPath.endsWith("/") && resolvedContextPath.length() > 1) {
            resolvedContextPath = resolvedContextPath.substring(0, resolvedContextPath.length() - 1);
        }
        if (!normalized.endsWith(resolvedContextPath)) {
            normalized += resolvedContextPath;
        }
        return URI.create(normalized);
    }

    private List<URI> rotate(List<URI> candidates) {
        if (candidates.size() <= 1) {
            return candidates;
        }
        int start = Math.floorMod(cursor.getAndIncrement(), candidates.size());
        List<URI> rotated = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            rotated.add(candidates.get((start + i) % candidates.size()));
        }
        return rotated;
    }
}
