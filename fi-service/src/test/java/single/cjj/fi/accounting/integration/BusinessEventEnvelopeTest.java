package single.cjj.fi.accounting.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BusinessEventEnvelopeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldParsePurchaseInboundEnvelope() {
        String json = """
                {
                  "eventId":"BE-001",
                  "eventType":"PURCHASE_INBOUND_CONFIRMED",
                  "eventVersion":1,
                  "tenantId":"T001",
                  "orgId":1001,
                  "producerService":"erp-service",
                  "domainCode":"PROCUREMENT",
                  "aggregateType":"PURCHASE_INBOUND",
                  "aggregateId":"9001",
                  "aggregateVersion":3,
                  "sourceSystemCode":"MATRIX",
                  "sourceDocumentType":"ERP_PURCHASE_INBOUND",
                  "sourceDocumentId":"9001",
                  "sourceDocumentNo":"PI202608260001",
                  "businessDate":"2026-08-26",
                  "operatorId":88,
                  "payload":{"totalAmount":100.00,"currencyCode":"CNY"}
                }
                """;

        BusinessEventEnvelope event = BusinessEventEnvelope.parse(objectMapper, json);

        assertEquals("BE-001", event.eventId());
        assertEquals("PURCHASE_INBOUND_CONFIRMED", event.eventType());
        assertEquals(1, event.eventVersion());
        assertEquals("T001", event.tenantId());
        assertEquals(1001L, event.orgId());
        assertEquals("ERP_PURCHASE_INBOUND", event.sourceDocumentType());
        assertEquals("PI202608260001", event.sourceDocumentNo());
        assertEquals(LocalDate.of(2026, 8, 26), event.businessDate());
        assertEquals(0, event.payload().path("totalAmount").decimalValue().compareTo(new BigDecimal("100.00")));
        assertNotNull(event.rawJson());
    }
}
