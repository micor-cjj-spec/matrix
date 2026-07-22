package single.cjj.botp.engine;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.FieldMapping;
import single.cjj.botp.domain.BotpContracts.MappingSourceType;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.TargetDraft;

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

        List<Map<String, Object>> targetEntries = new ArrayList<>();
        for (Map<String, Object> sourceEntry : sourceDocument.entries()) {
            Map<String, Object> targetEntry = new LinkedHashMap<>();
            applyMappings(rule.entryMappings(), sourceEntry, context, targetEntry);
            targetEntries.add(targetEntry);
        }

        return new TargetDraft(
                rule.targetSystemCode(),
                rule.targetDocumentType(),
                targetHeader,
                targetEntries
        );
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
