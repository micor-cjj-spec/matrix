package single.cjj.fi.accounting.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import single.cjj.bizfi.exception.BizException;

import java.time.LocalDate;

public record BusinessEventEnvelope(
        String eventId,
        String eventType,
        int eventVersion,
        String tenantId,
        Long orgId,
        String producerService,
        String domainCode,
        String aggregateType,
        String aggregateId,
        Long aggregateVersion,
        String sourceSystemCode,
        String sourceDocumentType,
        String sourceDocumentId,
        String sourceDocumentNo,
        LocalDate businessDate,
        String correlationId,
        String causationId,
        String traceId,
        Long operatorId,
        JsonNode payload,
        String rawJson
) {

    public static BusinessEventEnvelope parse(ObjectMapper objectMapper, String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            return new BusinessEventEnvelope(
                    requiredText(root, "eventId"),
                    requiredText(root, "eventType"),
                    root.path("eventVersion").asInt(0),
                    requiredText(root, "tenantId"),
                    nullableLong(root.get("orgId")),
                    nullableText(root.get("producerService")),
                    nullableText(root.get("domainCode")),
                    nullableText(root.get("aggregateType")),
                    nullableText(root.get("aggregateId")),
                    nullableLong(root.get("aggregateVersion")),
                    requiredText(root, "sourceSystemCode"),
                    requiredText(root, "sourceDocumentType"),
                    requiredText(root, "sourceDocumentId"),
                    nullableText(root.get("sourceDocumentNo")),
                    nullableDate(root.get("businessDate")),
                    nullableText(root.get("correlationId")),
                    nullableText(root.get("causationId")),
                    nullableText(root.get("traceId")),
                    nullableLong(root.get("operatorId")),
                    root.path("payload"),
                    rawJson
            );
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException("Business Event Envelope 解析失败: " + exception.getMessage());
        }
    }

    private static String requiredText(JsonNode root, String field) {
        String value = nullableText(root.get(field));
        if (value == null || value.isBlank()) {
            throw new BizException("Business Event 缺少字段: " + field);
        }
        return value;
    }

    private static String nullableText(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static Long nullableLong(JsonNode node) {
        if (node == null || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        return node.asLong();
    }

    private static LocalDate nullableDate(JsonNode node) {
        String value = nullableText(node);
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }
}
