package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeDoc;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeDocMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@ConditionalOnBean(PgVectorKnowledgeRepository.class)
public class PgVectorSemanticRetriever {

    private final PgVectorKnowledgeRepository repository;
    private final BizfiAiKnowledgeDocMapper docMapper;
    private final AiProperties properties;
    private final AiKnowledgeScopeResolver scopeResolver;

    public PgVectorSemanticRetriever(
            PgVectorKnowledgeRepository repository,
            BizfiAiKnowledgeDocMapper docMapper,
            AiProperties properties,
            AiKnowledgeScopeResolver scopeResolver
    ) {
        this.repository = repository;
        this.docMapper = docMapper;
        this.properties = properties;
        this.scopeResolver = scopeResolver;
    }

    public List<AiCitationResponse> retrieve(
            List<Double> queryVector,
            String queryModel,
            List<String> kbIds,
            int topK
    ) {
        Set<String> allowedDocIds = scopeResolver.resolveAllowedDocumentIds(kbIds);
        if (allowedDocIds != null && allowedDocIds.isEmpty()) {
            return List.of();
        }
        List<PgVectorKnowledgeRepository.VectorSearchResult> rawResults = repository.similaritySearch(
                queryVector,
                queryModel,
                allowedDocIds,
                Math.max(topK, topK * 2)
        );
        if (rawResults.isEmpty()) {
            return List.of();
        }

        Set<String> documentIds = rawResults.stream()
                .map(PgVectorKnowledgeRepository.VectorSearchResult::documentId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, BizfiAiKnowledgeDoc> activeDocs = loadActiveDocs(documentIds);
        double minScore = resolveMinSemanticScore();

        return rawResults.stream()
                .filter(item -> item.similarity() >= minScore)
                .filter(item -> activeDocs.containsKey(item.documentId()))
                .limit(topK)
                .map(item -> {
                    BizfiAiKnowledgeDoc doc = activeDocs.get(item.documentId());
                    return new AiCitationResponse(
                            item.documentId(),
                            doc.getFtitle(),
                            item.chunkId(),
                            buildSnippet(item.content())
                    );
                })
                .toList();
    }

    public boolean isReady() {
        return repository.isReady();
    }

    private Map<String, BizfiAiKnowledgeDoc> loadActiveDocs(Set<String> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return Map.of();
        }
        return docMapper.selectList(new LambdaQueryWrapper<BizfiAiKnowledgeDoc>()
                        .in(BizfiAiKnowledgeDoc::getFdocid, docIds)
                        .eq(BizfiAiKnowledgeDoc::getFstatus, "ACTIVE"))
                .stream()
                .collect(Collectors.toMap(
                        BizfiAiKnowledgeDoc::getFdocid,
                        Function.identity(),
                        (left, right) -> left
                ));
    }

    private String buildSnippet(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 280 ? normalized : normalized.substring(0, 280) + "...";
    }

    private double resolveMinSemanticScore() {
        Double configured = properties.getSemanticMinScore();
        if (configured == null || !Double.isFinite(configured)) {
            return 0.50D;
        }
        return Math.max(-1D, Math.min(configured, 1D));
    }
}
