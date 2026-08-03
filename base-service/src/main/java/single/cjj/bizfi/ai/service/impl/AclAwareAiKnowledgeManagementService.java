package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeChunkResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeDocDetailResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeDocRequest;
import single.cjj.bizfi.ai.dto.AiKnowledgeDocSummaryResponse;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeChunk;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeDoc;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeChunkMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeDocMapper;
import single.cjj.bizfi.ai.security.AiKnowledgePermission;
import single.cjj.bizfi.ai.service.AiKnowledgeManagementService;
import single.cjj.bizfi.exception.BizException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Primary
@Service
public class AclAwareAiKnowledgeManagementService implements AiKnowledgeManagementService {

    private final AiKnowledgeManagementService delegate;
    private final BizfiAiKnowledgeDocMapper docMapper;
    private final BizfiAiKnowledgeChunkMapper chunkMapper;
    private final AiKnowledgeAclService aclService;

    public AclAwareAiKnowledgeManagementService(
            @Qualifier("aiKnowledgeManagementService") AiKnowledgeManagementService delegate,
            BizfiAiKnowledgeDocMapper docMapper,
            BizfiAiKnowledgeChunkMapper chunkMapper,
            AiKnowledgeAclService aclService
    ) {
        this.delegate = delegate;
        this.docMapper = docMapper;
        this.chunkMapper = chunkMapper;
        this.aclService = aclService;
    }

    @Override
    public IPage<AiKnowledgeDocSummaryResponse> listDocs(
            int page,
            int size,
            String keyword,
            String category,
            String status,
            String kbId
    ) {
        if (StringUtils.hasText(kbId) && !"all".equalsIgnoreCase(kbId.trim())) {
            aclService.assertCanView(kbId);
            return delegate.listDocs(page, size, keyword, category, status, kbId);
        }

        Set<String> allowedKbIds = aclService.resolveAccessibleBaseIds(AiKnowledgePermission.VIEWER);
        if (allowedKbIds == null) {
            return delegate.listDocs(page, size, keyword, category, status, kbId);
        }
        int normalizedPage = Math.max(1, page);
        int normalizedSize = Math.max(1, Math.min(size, 100));
        if (allowedKbIds.isEmpty()) {
            return new Page<>(normalizedPage, normalizedSize, 0);
        }

        LambdaQueryWrapper<BizfiAiKnowledgeDoc> wrapper = new LambdaQueryWrapper<BizfiAiKnowledgeDoc>()
                .in(BizfiAiKnowledgeDoc::getFkbid, allowedKbIds);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(query -> query.like(BizfiAiKnowledgeDoc::getFtitle, kw)
                    .or().like(BizfiAiKnowledgeDoc::getFcategory, kw)
                    .or().like(BizfiAiKnowledgeDoc::getFcontent, kw));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(BizfiAiKnowledgeDoc::getFcategory, category.trim());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BizfiAiKnowledgeDoc::getFstatus, normalizeStatus(status));
        }
        wrapper.orderByDesc(BizfiAiKnowledgeDoc::getFmodifytime)
                .orderByDesc(BizfiAiKnowledgeDoc::getFid);

        Page<BizfiAiKnowledgeDoc> source = docMapper.selectPage(
                new Page<>(normalizedPage, normalizedSize),
                wrapper
        );
        Page<AiKnowledgeDocSummaryResponse> result = new Page<>(
                source.getCurrent(), source.getSize(), source.getTotal()
        );
        result.setRecords(source.getRecords().stream().map(this::toSummary).toList());
        return result;
    }

    @Override
    public AiKnowledgeDocDetailResponse getDoc(String docId) {
        BizfiAiKnowledgeDoc doc = requireDoc(docId);
        aclService.assertCanView(doc.getFkbid());
        return delegate.getDoc(docId);
    }

    @Override
    public AiKnowledgeDocDetailResponse createDoc(AiKnowledgeDocRequest request) {
        String kbId = request == null || !StringUtils.hasText(request.getKbId())
                ? AiKnowledgeBaseServiceImpl.DEFAULT_KB_ID
                : request.getKbId();
        aclService.assertCanEdit(kbId);
        return delegate.createDoc(request);
    }

    @Override
    public AiKnowledgeDocDetailResponse updateDoc(String docId, AiKnowledgeDocRequest request) {
        BizfiAiKnowledgeDoc existing = requireDoc(docId);
        aclService.assertCanEdit(existing.getFkbid());
        if (request != null && StringUtils.hasText(request.getKbId())
                && !existing.getFkbid().equalsIgnoreCase(request.getKbId().trim())) {
            aclService.assertCanEdit(request.getKbId());
        }
        return delegate.updateDoc(docId, request);
    }

    @Override
    public boolean deleteDoc(String docId) {
        aclService.assertCanEdit(requireDoc(docId).getFkbid());
        return delegate.deleteDoc(docId);
    }

    @Override
    public int rebuildChunks(String docId) {
        aclService.assertCanEdit(requireDoc(docId).getFkbid());
        return delegate.rebuildChunks(docId);
    }

    @Override
    public List<AiKnowledgeChunkResponse> listChunks(String docId) {
        aclService.assertCanView(requireDoc(docId).getFkbid());
        return delegate.listChunks(docId);
    }

    @Override
    public List<String> listCategories() {
        Set<String> allowedKbIds = aclService.resolveAccessibleBaseIds(AiKnowledgePermission.VIEWER);
        LambdaQueryWrapper<BizfiAiKnowledgeDoc> wrapper = new LambdaQueryWrapper<BizfiAiKnowledgeDoc>()
                .isNotNull(BizfiAiKnowledgeDoc::getFcategory)
                .orderByAsc(BizfiAiKnowledgeDoc::getFcategory);
        if (allowedKbIds != null) {
            if (allowedKbIds.isEmpty()) {
                return List.of();
            }
            wrapper.in(BizfiAiKnowledgeDoc::getFkbid, allowedKbIds);
        }
        return docMapper.selectList(wrapper).stream()
                .map(BizfiAiKnowledgeDoc::getFcategory)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    @Override
    public List<AiCitationResponse> retrieve(String question, List<String> kbIds, Integer topK) {
        return delegate.retrieve(question, kbIds, topK);
    }

    private BizfiAiKnowledgeDoc requireDoc(String docId) {
        if (!StringUtils.hasText(docId)) {
            throw new BizException("知识文档编号不能为空");
        }
        BizfiAiKnowledgeDoc doc = docMapper.selectOne(new LambdaQueryWrapper<BizfiAiKnowledgeDoc>()
                .eq(BizfiAiKnowledgeDoc::getFdocid, docId.trim())
                .last("limit 1"));
        if (doc == null) {
            throw new BizException("知识文档不存在");
        }
        return doc;
    }

    private AiKnowledgeDocSummaryResponse toSummary(BizfiAiKnowledgeDoc doc) {
        Long chunkCount = chunkMapper.selectCount(new LambdaQueryWrapper<BizfiAiKnowledgeChunk>()
                .eq(BizfiAiKnowledgeChunk::getFdocid, doc.getFdocid()));
        return new AiKnowledgeDocSummaryResponse(
                doc.getFid(),
                doc.getFkbid(),
                doc.getFdocid(),
                doc.getFtitle(),
                doc.getFcategory(),
                doc.getFsourcepath(),
                doc.getFversion(),
                doc.getFstatus(),
                chunkCount == null ? 0 : Math.toIntExact(chunkCount),
                preview(doc.getFcontent()),
                doc.getFcreatetime(),
                doc.getFmodifytime()
        );
    }

    private String preview(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160) + "...";
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (Set.of("ENABLED", "PUBLISHED").contains(normalized)) {
            return "ACTIVE";
        }
        if (Set.of("DISABLED", "ARCHIVED").contains(normalized)) {
            return "INACTIVE";
        }
        return normalized;
    }
}
