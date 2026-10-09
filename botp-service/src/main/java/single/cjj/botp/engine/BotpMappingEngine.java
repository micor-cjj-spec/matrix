package single.cjj.botp.engine;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.FieldMapping;
import single.cjj.botp.domain.BotpContracts.MappingSourceType;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.TargetDraft;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class BotpMappingEngine {

    public TargetDraft transform(
            RuleDefinition rule,
            DocumentData sourceDocument,
            Map<String, Object> context
    ) {
        Map<String, Object> targetHeader = new LinkedHashMap<>();
        applyMappings(rule.headerMappings(), sourceDocument.header(), context, targetHeader);
        targetHeader.putIfAbsent("sourceExecutionId", context.get("executionId"));
        targetHeader.putIfAbsent("operatorId", context.get("operatorId"));

        List<Map<String, Object>> targetEntries = new ArrayList<>();
        int index = 0;
        for (Map<String, Object> sourceEntry : sourceDocument.entries()) {
            Map<String, Object> targetEntry = new LinkedHashMap<>();
            applyMappings(rule.entryMappings(), sourceEntry, context, targetEntry);
            applyEntryQuantityOverride(sourceEntry, targetEntry, context);
            attachRelationMetadata(sourceEntry, targetEntry, index++);
            targetEntries.add(targetEntry);
        }

        return new TargetDraft(
                rule.targetSystemCode(),
                rule.targetDocumentType(),
                targetHeader,
                targetEntries
        );
    }

    /**
     * 支持部分履约：ExecutionRequest.parameters.entryQuantities 使用源分录ID作为 key。
     * 示例：{"entryQuantities":{"12345":60}}。
     * 最终数量合法性仍由目标领域服务的行锁/预占校验兜底。
     */
    private void applyEntryQuantityOverride(
            Map<String, Object> sourceEntry,
            Map<String, Object> targetEntry,
            Map<String, Object> context
    ) {
        Object sourceEntryId = sourceEntry.get("entryId");
        Object rawOverrides = context.get("entryQuantities");
        if (sourceEntryId == null || !(rawOverrides instanceof Map<?, ?> overrides)) {
            return;
        }
        Object override = overrides.get(String.valueOf(sourceEntryId));
        if (override == null) {
            override = overrides.get(sourceEntryId);
        }
        if (override == null) {
            return;
        }
        BigDecimal quantity;
        try {
            quantity = new BigDecimal(String.valueOf(override));
        } catch (NumberFormatException exception) {
            throw new BizException("BOTP 分录下推数量格式错误: sourceEntryId=" + sourceEntryId);
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("BOTP 分录下推数量必须大于0: sourceEntryId=" + sourceEntryId);
        }
        if (targetEntry.containsKey("inspectionQuantity")) {
            targetEntry.put("inspectionQuantity", quantity);
        } else {
            targetEntry.put("quantity", quantity);
        }
    }

    private void attachRelationMetadata(
            Map<String, Object> sourceEntry,
            Map<String, Object> targetEntry,
            int index
    ) {
        Object sourceEntryId = sourceEntry.get("entryId");
        String correlationKey = sourceEntryId == null
                ? String.valueOf(index + 1)
                : String.valueOf(sourceEntryId);
        if (sourceEntryId != null) {
            targetEntry.putIfAbsent("_botpSourceEntryId", String.valueOf(sourceEntryId));
        }
        targetEntry.putIfAbsent("_botpCorrelationKey", correlationKey);

        Object quantity = firstNonNull(
                targetEntry.get("_botpRelationQuantity"),
                targetEntry.get("quantity"),
                targetEntry.get("inspectionQuantity"),
                sourceEntry.get("availableQuantity"),
                sourceEntry.get("quantity")
        );
        if (quantity != null) {
            targetEntry.put("_botpRelationQuantity", quantity);
        }
        Object amount = firstNonNull(
                targetEntry.get("_botpRelationAmount"),
                targetEntry.get("amount"),
                sourceEntry.get("amount")
        );
        if (amount != null) {
            targetEntry.putIfAbsent("_botpRelationAmount", amount);
        }
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private void applyMappings(
            List<FieldMapping> mappings,
            Map<String, Object> source,
            Map<String, Object> context,
            Map<String, Object> target
    ) {
        for (FieldMapping mapping : mappings) {
            validateMapping(mapping);
            Object value = resolveValue(mapping, source, context);
            if (mapping.required() && isEmpty(value)) {
                throw new BizException("BOTP 必填映射结果为空: targetPath=" + mapping.targetPath());
            }
            writePath(target, normalizePath(mapping.targetPath()), value);
        }
    }

    private Object resolveValue(
            FieldMapping mapping,
            Map<String, Object> source,
            Map<String, Object> context
    ) {
        MappingSourceType sourceType = mapping.sourceType();
        if (sourceType == null) {
            throw new BizException("BOTP 映射未配置 sourceType: targetPath=" + mapping.targetPath());
        }
        return switch (sourceType) {
            case SOURCE_FIELD -> readPath(source, normalizePath(mapping.sourcePath()));
            case CONSTANT -> mapping.constantValue();
            case CONTEXT -> readPath(context, normalizePath(mapping.sourcePath()));
        };
    }

    private void validateMapping(FieldMapping mapping) {
        if (mapping == null) {
            throw new BizException("BOTP 字段映射不能为空");
        }
        if (!StringUtils.hasText(mapping.targetPath())) {
            throw new BizException("BOTP 映射未配置 targetPath");
        }
        if (mapping.sourceType() != MappingSourceType.CONSTANT
                && !StringUtils.hasText(mapping.sourcePath())) {
            throw new BizException("BOTP 映射未配置 sourcePath: targetPath=" + mapping.targetPath());
        }
    }

    private Object readPath(Map<String, Object> root, String path) {
        if (root == null || !StringUtils.hasText(path)) {
            return null;
        }
        Object current = root;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return null;
            }
            current = currentMap.get(segment);
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private void writePath(Map<String, Object> root, String path, Object value) {
        String[] segments = path.split("\\.");
        Map<String, Object> current = root;
        for (int index = 0; index < segments.length - 1; index++) {
            String segment = segments[index];
            Object nested = current.get(segment);
            if (nested == null) {
                Map<String, Object> child = new LinkedHashMap<>();
                current.put(segment, child);
                current = child;
            } else if (nested instanceof Map<?, ?> nestedMap) {
                current = (Map<String, Object>) nestedMap;
            } else {
                throw new BizException("BOTP 目标路径冲突: " + path);
            }
        }
        current.put(segments[segments.length - 1], value);
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return path;
        }
        if (path.startsWith("header.")) {
            return path.substring("header.".length());
        }
        if (path.startsWith("entry.")) {
            return path.substring("entry.".length());
        }
        if (path.startsWith("parameters.")) {
            return path.substring("parameters.".length());
        }
        return path;
    }

    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence text) {
            return !StringUtils.hasText(text);
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        return false;
    }
}
