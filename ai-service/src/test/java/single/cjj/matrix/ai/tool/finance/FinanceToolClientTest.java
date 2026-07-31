package single.cjj.matrix.ai.tool.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import single.cjj.matrix.ai.api.ModelContracts;
import single.cjj.matrix.ai.config.MatrixAiProperties;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceToolClientTest {

    private HttpServer server;
    private FinanceToolClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/internal/ai/tools/month-end-close-check", this::handleRequest);
        server.start();

        MatrixAiProperties properties = new MatrixAiProperties();
        properties.setInternalToken("model-token");
        properties.setFinanceToolBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api");
        properties.setFinanceToolInternalToken("finance-token");
        properties.setFinanceToolTimeoutSeconds(5);
        client = new FinanceToolClient(new ObjectMapper().findAndRegisterModules(), properties);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldCallReadOnlyMonthEndEndpoint() {
        FinanceMonthEndCloseResult result = client.monthEndCloseCheck(new ModelContracts.ToolContext(
                "month-end-close-check",
                7L,
                10L,
                "2026-07",
                "tool_request"
        ));

        assertEquals(10L, result.organizationId());
        assertEquals("BLOCKED", result.closeStatus());
        assertEquals(2, result.blockingCount());
        assertTrue(result.readOnly());
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        assertEquals("finance-token", exchange.getRequestHeaders().getFirst(FinanceToolClient.TOKEN_HEADER));
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(requestBody.contains("\"organizationId\":10"));
        assertTrue(requestBody.contains("\"requestedByUserId\":7"));

        String response = """
                {
                  "organizationId":10,
                  "period":"2026-07",
                  "periodStatus":"OPEN",
                  "closeStatus":"BLOCKED",
                  "readinessScore":62,
                  "canClose":false,
                  "totalCheckCount":5,
                  "passedCount":2,
                  "warningCount":1,
                  "blockingCount":2,
                  "pendingCount":0,
                  "periodVoucherCount":12,
                  "postedVoucherCount":8,
                  "pendingVoucherCount":4,
                  "exceptionVoucherCount":0,
                  "checkedAt":"2026-07-31T09:00:00",
                  "checkItems":[],
                  "warnings":["存在未过账凭证"],
                  "readOnly":true
                }
                """;
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
