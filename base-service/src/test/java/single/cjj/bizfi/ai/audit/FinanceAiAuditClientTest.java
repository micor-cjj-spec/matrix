package single.cjj.bizfi.ai.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import single.cjj.bizfi.ai.config.AiProperties;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceAiAuditClientTest {

    private HttpServer server;
    private FinanceAiAuditClient client;
    private AiAuditOperatorContext operator;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/internal/ai/audit/tool-executions", this::handleRequest);
        server.start();

        AiProperties properties = new AiProperties();
        properties.setFinanceAuditBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api");
        properties.setFinanceAuditInternalToken("audit-secret");
        properties.setRequestTimeoutSeconds(5);
        client = new FinanceAiAuditClient(new ObjectMapper().findAndRegisterModules(), properties);
        operator = new AiAuditOperatorContext(
                7L,
                Set.of("AI_TOOL_AUDIT_VIEW", "AI_TOOL_AUDIT_RECONCILE")
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldSendNamedOperatorHeadersForDetail() {
        AiToolExecutionAuditResponse response = client.execution(operator, "tool_request_1");

        assertEquals("tool_request_1", response.requestId());
        assertEquals("TIMED_OUT", response.status());
    }

    @Test
    void shouldForwardBoundedSearchFilters() {
        AiToolExecutionAuditPageResponse response = client.executions(
                operator,
                7L,
                10L,
                "2026-07",
                "TIMED_OUT",
                "c_tool",
                "trace_tool",
                null,
                null,
                1,
                20
        );

        assertEquals(1L, response.total());
        assertEquals(1, response.items().size());
    }

    @Test
    void shouldUseReconcileEndpoint() {
        AiToolExecutionReconciliationResponse response = client.reconcileStale(operator);

        assertEquals(1, response.timedOutCount());
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        assertEquals("audit-secret", exchange.getRequestHeaders().getFirst("X-Matrix-AI-Audit-Token"));
        assertEquals("7", exchange.getRequestHeaders().getFirst("X-Matrix-Audit-Operator-Id"));
        assertTrue(exchange.getRequestHeaders().getFirst("X-Matrix-Audit-Operator-Roles")
                .contains("AI_TOOL_AUDIT_VIEW"));
        assertTrue(exchange.getRequestHeaders().getFirst("X-Matrix-Audit-Request-Id")
                .startsWith("audit_"));

        String path = exchange.getRequestURI().getPath();
        String response;
        if (path.endsWith("/reconcile-stale")) {
            assertEquals("POST", exchange.getRequestMethod());
            response = """
                    {"cutoff":"2026-07-31T09:45:00","scannedCount":2,"timedOutCount":1}
                    """;
        } else if (path.endsWith("/tool_request_1")) {
            response = detailJson();
        } else {
            String query = exchange.getRequestURI().getQuery();
            assertTrue(query.contains("organizationId=10"));
            assertTrue(query.contains("status=TIMED_OUT"));
            response = """
                    {"page":1,"size":20,"total":1,"totalPages":1,"items":[%s]}
                    """.formatted(detailJson());
        }
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String detailJson() {
        return """
                {
                  "requestId":"tool_request_1",
                  "conversationId":"c_tool",
                  "modelName":"gpt-tool-model",
                  "modelTraceId":"trace_tool",
                  "toolName":"month-end-close-check",
                  "userId":7,
                  "organizationId":10,
                  "period":"2026-07",
                  "status":"TIMED_OUT",
                  "durationMillis":3600000,
                  "errorCode":"EXECUTION_TIMEOUT",
                  "startedAt":"2026-07-31T09:00:00",
                  "endedAt":"2026-07-31T10:00:00"
                }
                """;
    }
}
