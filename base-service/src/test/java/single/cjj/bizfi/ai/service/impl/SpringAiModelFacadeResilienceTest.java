package single.cjj.bizfi.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiModelRequest;
import single.cjj.bizfi.ai.dto.AiModelResult;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiModelFacadeResilienceTest {

    private final List<HttpServer> servers = new ArrayList<>();

    @AfterEach
    void tearDown() {
        servers.forEach(server -> server.stop(0));
    }

    @Test
    void shouldFailOverToNextEndpointForRetryableStatus() throws IOException {
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        URI first = startServer(503, "unavailable", firstCalls);
        URI second = startServer(200, successBody("second-answer"), secondCalls);

        SpringAiModelFacade facade = facade(List.of(first, second), 2);
        AiModelResult result = facade.chat(new AiModelRequest("question", List.of(), List.of()));

        assertEquals("second-answer", result.getAnswer());
        assertEquals(1, firstCalls.get());
        assertEquals(1, secondCalls.get());
    }

    @Test
    void shouldStopForNonRetryableClientError() throws IOException {
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        URI first = startServer(401, "unauthorized", firstCalls);
        URI second = startServer(200, successBody("must-not-run"), secondCalls);

        SpringAiModelFacade facade = facade(List.of(first, second), 2);

        assertThrows(
                IllegalStateException.class,
                () -> facade.chat(new AiModelRequest("question", List.of(), List.of()))
        );
        assertEquals(1, firstCalls.get());
        assertEquals(0, secondCalls.get());
    }

    private SpringAiModelFacade facade(List<URI> endpoints, int maxAttempts) {
        AiProperties properties = new AiProperties();
        properties.setInternalToken("test-token");
        properties.setRequestTimeoutSeconds(5);
        properties.setSpringAiMaxAttempts(maxAttempts);
        properties.setSpringAiCircuitFailureThreshold(3);

        AiServiceEndpointResolver resolver = mock(AiServiceEndpointResolver.class);
        when(resolver.resolveCandidates()).thenReturn(endpoints);
        return new SpringAiModelFacade(
                new ObjectMapper(),
                properties,
                resolver,
                new SpringAiCircuitBreaker(properties)
        );
    }

    private URI startServer(int status, String responseBody, AtomicInteger calls) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/internal/model/chat", exchange -> handle(exchange, status, responseBody, calls));
        server.start();
        servers.add(server);
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api");
    }

    private void handle(
            HttpExchange exchange,
            int status,
            String responseBody,
            AtomicInteger calls
    ) throws IOException {
        calls.incrementAndGet();
        exchange.getRequestBody().readAllBytes();
        byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private String successBody(String answer) {
        return """
                {
                  "answer":"%s",
                  "model":"test-model",
                  "mode":"spring-ai",
                  "traceId":"trace",
                  "promptTokens":1,
                  "completionTokens":1,
                  "totalTokens":2,
                  "estimatedCost":0.0
                }
                """.formatted(answer);
    }
}
