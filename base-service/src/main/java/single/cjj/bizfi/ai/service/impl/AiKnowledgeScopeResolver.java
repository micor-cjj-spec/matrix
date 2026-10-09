package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeBase;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeDoc;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeBaseMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeDocMapper;
import single.cjj.bizfi.ai.security.AiKnowledgePermission;

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
    private final AiKnowledgeAclService aclService;

    public AiKnowledgeScopeResolver(
            BizfiAiKnowledgeBaseMapper knowledgeBaseMapper,
            BizfiAiKnowledgeDocMapper knowledgeDocMapper,
            AiKnowledgeAclService aclService
    ) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocMapper = knowledgeDocMapper;
        this.aclService = aclService;
    }

    /**
     * Returns null for unrestricted retrieval, an empty set when the scope resolves to no accessible documents,
     * or concrete document IDs. Knowledge base IDs and legacy document IDs may be mixed.
     */
    public Set<String> resolveAllowedDocumentIds(List<String> kbIds) {
        Set<String> accessibleBaseIds = aclService.resolveAccessibleBaseIds(AiKnowledgePermission.VIEWER);
        LinkedHashSet<String> scopes = normalizeScopes(kbIds);
        boolean globalScope = scopes.isEmpty() || scopes.stream().anyMatch(GLOBAL_ALIASES::contains);

        if (globalScope) {
            if (accessibleBaseIds == null) {
                return null;
            }
            return loadActiveDocumentIds(accessibleBaseIds);
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
                .filter(kbId -> accessibleBaseIds == null || accessibleBaseIds.contains(kbId))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        LinkedHashSet<String> documentIds = new LinkedHashSet<>(loadActiveDocumentIds(activeBaseIds));
        Set<String> legacyDocumentIds = scopes.stream()
                .filter(scope -> !knownBaseIds.contains(scope))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!legacyDocumentIds.isEmpty()) {
            knowledgeDocMapper.selectList(new LambdaQueryWrapper<BizfiAiKnowledgeDoc>()
                            .in(BizfiAiKnowledgeDoc::getFdocid, legacyDocumentIds)
                            .eq(BizfiAiKnowledgeDoc::getFstatus, "ACTIVE"))
                    .stream()
                    .filter(doc -> accessibleBaseIds == null || accessibleBaseIds.contains(doc.getFkbid()))
                    .map(BizfiAiKnowledgeDoc::getFdocid)
                    .filter(StringUtils::hasText)
                    .forEach(documentIds::add);
        }
        return documentIds;
    }

    private LinkedHashSet<String> normalizeScopes(List<String> kbIds) {
        if (kbIds == null || kbIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return kbIds.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeIdentifier)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> loadActiveDocumentIds(Set<String> baseIds) {
        if (baseIds == null || baseIds.isEmpty()) {
            return Set.of();
        }
        return knowledgeDocMapper.selectList(new LambdaQueryWrapper<BizfiAiKnowledgeDoc>()
                        .in(BizfiAiKnowledgeDoc::getFkbid, baseIds)
                        .eq(BizfiAiKnowledgeDoc::getFstatus, "ACTIVE")
                        .orderByAsc(BizfiAiKnowledgeDoc::getFdocid))
                .stream()
                .map(BizfiAiKnowledgeDoc::getFdocid)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeIdentifier(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim()
                .replaceAll("[^A-Za-z0-9_-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "")
                .toLowerCase(Locale.ROOT);
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }
}
