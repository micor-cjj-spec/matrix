package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeBase;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeDoc;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeBaseMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeDocMapper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiKnowledgeScopeResolver {

    private static final Set<String> GLOBAL_ALIASES = Set.of("all", "knowledge", "bizfi");

    private final BizfiAiKnowledgeBaseMapper knowledgeBaseMapper;
    private final BizfiAiKnowledgeDocMapper knowledgeDocMapper;

    public AiKnowledgeScopeResolver(
            BizfiAiKnowledgeBaseMapper knowledgeBaseMapper,
            BizfiAiKnowledgeDocMapper knowledgeDocMapper
    ) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocMapper = knowledgeDocMapper;
    }

    /**
     * Returns null for unrestricted retrieval, an empty set when the scope resolves to no documents,
     * or concrete document IDs. Knowledge base IDs and legacy document IDs may be mixed.
     */
    public Set<String> resolveAllowedDocumentIds(List<String> kbIds) {
        if (kbIds == null || kbIds.isEmpty()) {
            return null;
        }

        LinkedHashSet<String> scopes = kbIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (scopes.isEmpty()) {
            return null;
        }
        if (scopes.stream().map(item -> item.toLowerCase(Locale.ROOT)).anyMatch(GLOBAL_ALIASES::contains)) {
            return null;
        }

        List<BizfiAiKnowledgeBase> matchingBases = knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<BizfiAiKnowledgeBase>()
                        .in(BizfiAiKnowledgeBase::getFkbid, scopes)
        );
        Set<String> knownBaseIds = matchingBases.stream()
                .map(BizfiAiKnowledgeBase::getFkbid)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> activeBaseIds = matchingBases.stream()
                .filter(base -> "ACTIVE".equals(base.getFstatus()))
                .map(BizfiAiKnowledgeBase::getFkbid)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        LinkedHashSet<String> documentIds = new LinkedHashSet<>();
        if (!activeBaseIds.isEmpty()) {
            knowledgeDocMapper.selectList(new LambdaQueryWrapper<BizfiAiKnowledgeDoc>()
                            .in(BizfiAiKnowledgeDoc::getFkbid, activeBaseIds)
                            .eq(BizfiAiKnowledgeDoc::getFstatus, "ACTIVE")
                            .orderByAsc(BizfiAiKnowledgeDoc::getFdocid))
                    .stream()
                    .map(BizfiAiKnowledgeDoc::getFdocid)
                    .filter(StringUtils::hasText)
                    .forEach(documentIds::add);
        }

        scopes.stream()
                .filter(scope -> !knownBaseIds.contains(scope))
                .map(this::normalizeDocId)
                .filter(StringUtils::hasText)
                .forEach(documentIds::add);
        return documentIds;
    }

    private String normalizeDocId(String docId) {
        if (!StringUtils.hasText(docId)) {
            return "";
        }
        String normalized = docId.trim()
                .replaceAll("[^A-Za-z0-9_-]", "_")
                .replaceAll("_+", "_")
                .toLowerCase(Locale.ROOT);
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }
}
