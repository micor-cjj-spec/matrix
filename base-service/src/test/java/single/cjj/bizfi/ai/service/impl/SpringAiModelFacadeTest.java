package single.cjj.bizfi.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiModelRequest;
import single.cjj.bizfi.ai.dto.AiModelResult;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringAiModelFacadeTest {

    private HttpServer server;
    private SpringAiModelFacade facade;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/internal/model/chat", this::handleChat);
        server.createContext("/api/internal/model/chat/stream", this::handleStream);
        server.start();

        AiProperties properties = new AiProperties();
        properties.setSpringAiBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api");
        properties.setInternalToken("test-token");
        properties.setRequestTimeoutSeconds(5);
        facade = new SpringAiModelFacade(new ObjectMapper(), properties);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldCallRemoteSynchronousEndpoint() {
        AiModelResult result = facade.chat(new AiModelRequest("question", List.of(), List.of()));

        assertEquals("sync-answer", result.getAnswer());
        assertEquals("spring-ai", result.getMode());
    }

    @Test
    void shouldForwardStreamingDeltasAndReturnDoneResult() {
        List<String> deltas = new ArrayList<>();

        AiModelResult result = facade.stream(
                new AiModelRequest("question", List.of(), List.of()),
                deltas::add
        );

        assertEquals(List.of("hello ", "world"), deltas);
        assertEquals("hello world", result.getAnswer());
        assertEquals("stream-trace", result.getTraceId());
    }

    private void handleChat(HttpExchange exchange) throws IOException {
        assertInternalToken(exchange);
        exchange.getRequestBody().readAllBytes();
        byte[] response = """
                {
                  "answer":"sync-answer",
                  "model":"test-model",
                  "mode":"spring-ai",
                  "traceId":"sync-trace",
                  "promptTokens":2,
                  "completionTokens":3,
                  "totalTokens":5,
                  "estimatedCost":0.0
                }
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private void handleStream(HttpExchange exchange) throws IOException {
        assertInternalToken(exchange);
        exchange.getRequestBody().readAllBytes();
        String response = """
                event: start
                data: {"type":"start","delta":null,"result":null,"message":null}

                event: delta
                data: {"type":"delta","delta":"hello ","result":null,"message":null}

                event: delta
                data: {"type":"delta","delta":"world","result":null,"message":null}

                event: done
                data: {"type":"done","delta":null,"result":{"answer":"hello world","model":"test-model","mode":"spring-ai","traceId":"stream-trace","promptTokens":0,"completionTokens":0,"totalTokens":0,"estimatedCost":0.0},"message":null}

                """;
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private void assertInternalToken(HttpExchange exchange) {
        assertEquals("test-token", exchange.getRequestHeaders().getFirst("X-Matrix-Internal-Token"));
        assertTrue(exchange.getRequestMethod().equalsIgnoreCase("POST"));
    }
}
