package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeChunkResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeDocDetailResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeDocRequest;
import single.cjj.bizfi.ai.dto.AiKnowledgeDocSummaryResponse;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeBase;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeChunk;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeDoc;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeChunkMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeDocMapper;
import single.cjj.bizfi.ai.service.AiKnowledgeManagementService;
import single.cjj.bizfi.ai.service.AiKnowledgeService;
import single.cjj.bizfi.exception.BizException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("aiKnowledgeManagementService")
public class AiKnowledgeManagementServiceImpl implements AiKnowledgeService, AiKnowledgeManagementService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 20;
    private static final int MAX_CHUNK_LENGTH = 1200;
    private static final int CHUNK_OVERLAP = 120;

    private final BizfiAiKnowledgeDocMapper knowledgeDocMapper;
    private final BizfiAiKnowledgeChunkMapper knowledgeChunkMapper;
    private final AiKnowledgeBaseServiceImpl knowledgeBaseService;
    private final AiKnowledgeScopeResolver scopeResolver;

    public AiKnowledgeManagementServiceImpl(
            BizfiAiKnowledgeDocMapper knowledgeDocMapper,
            BizfiAiKnowledgeChunkMapper knowledgeChunkMapper,
            AiKnowledgeBaseServiceImpl knowledgeBaseService,
            AiKnowledgeScopeResolver scopeResolver
    ) {
        this.knowledgeDocMapper = knowledgeDocMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.knowledgeBaseService = knowledgeBaseService;
        this.scopeResolver = scopeResolver;
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
        LambdaQueryWrapper<BizfiAiKnowledgeDoc> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(q -> q.like(BizfiAiKnowledgeDoc::getFtitle, kw)
                    .or().like(BizfiAiKnowledgeDoc::getFcategory, kw)
                    .or().like(BizfiAiKnowledgeDoc::getFcontent, kw));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(BizfiAiKnowledgeDoc::getFcategory, category.trim());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BizfiAiKnowledgeDoc::getFstatus, normalizeStatus(status));
        }
        if (StringUtils.hasText(kbId) && !"all".equalsIgnoreCase(kbId.trim())) {
            wrapper.eq(BizfiAiKnowledgeDoc::getFkbid, normalizeKbId(kbId));
        }
        wrapper.orderByDesc(BizfiAiKnowledgeDoc::getFmodifytime)
                .orderByDesc(BizfiAiKnowledgeDoc::getFid);

        Page<BizfiAiKnowledgeDoc> docPage = knowledgeDocMapper.selectPage(
                new Page<>(Math.max(1, page), Math.max(1, Math.min(size, 100))),
                wrapper
        );
        List<AiKnowledgeDocSummaryResponse> records = docPage.getRecords().stream()
                .map(this::toSummary)
                .toList();

        Page<AiKnowledgeDocSummaryResponse> result = new Page<>(
                docPage.getCurrent(),
                docPage.getSize(),
                docPage.getTotal()
        );
        result.setRecords(records);
        return result;
    }

    @Override
    public AiKnowledgeDocDetailResponse getDoc(String docId) {
        return toDetail(requireDoc(docId), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeDocDetailResponse createDoc(AiKnowledgeDocRequest request) {
        if (request == null) {
            throw new BizException("知识文档不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BizException("知识标题不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new BizException("知识正文不能为空");
        }

        String docId = normalizeDocId(request.getDocId());
        if (!StringUtils.hasText(docId)) {
            docId = generateDocId();
        }
        if (findDoc(docId) != null) {
            throw new BizException("知识文档编号已存在");
        }

        BizfiAiKnowledgeBase knowledgeBase = knowledgeBaseService.requireActiveBase(request.getKbId());
        LocalDateTime now = LocalDateTime.now();
        BizfiAiKnowledgeDoc doc = new BizfiAiKnowledgeDoc();
        doc.setFkbid(knowledgeBase.getFkbid());
        doc.setFdocid(docId);
        doc.setFtitle(request.getTitle().trim());
        doc.setFcategory(normalizeCategory(request.getCategory()));
        doc.setFsourcepath(normalizeSourcePath(request.getSourcePath(), docId));
        doc.setFcontent(request.getContent().trim());
        doc.setFversion(normalizeVersion(request.getVersion()));
        doc.setFstatus(normalizeStatus(request.getStatus()));
        doc.setFcreatetime(now);
        doc.setFmodifytime(now);
        knowledgeDocMapper.insert(doc);
        replaceChunks(doc);
        return getDoc(docId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeDocDetailResponse updateDoc(String docId, AiKnowledgeDocRequest request) {
        BizfiAiKnowledgeDoc doc = requireDoc(docId);
        if (request == null) {
            throw new BizException("知识文档不能为空");
        }
        if (request.getKbId() != null) {
            BizfiAiKnowledgeBase knowledgeBase = knowledgeBaseService.requireActiveBase(request.getKbId());
            doc.setFkbid(knowledgeBase.getFkbid());
        }
        if (StringUtils.hasText(request.getTitle())) {
            doc.setFtitle(request.getTitle().trim());
        }
        if (request.getCategory() != null) {
            doc.setFcategory(normalizeCategory(request.getCategory()));
        }
        if (request.getSourcePath() != null) {
            doc.setFsourcepath(normalizeSourcePath(request.getSourcePath(), doc.getFdocid()));
        }
        if (StringUtils.hasText(request.getContent())) {
            doc.setFcontent(request.getContent().trim());
        }
        if (request.getVersion() != null) {
            doc.setFversion(normalizeVersion(request.getVersion()));
        }
        if (request.getStatus() != null) {
            doc.setFstatus(normalizeStatus(request.getStatus()));
        }
        doc.setFmodifytime(LocalDateTime.now());
        knowledgeDocMapper.updateById(doc);
        replaceChunks(doc);
        return getDoc(doc.getFdocid());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDoc(String docId) {
        BizfiAiKnowledgeDoc doc = requireDoc(docId);
        knowledgeChunkMapper.delete(new LambdaQueryWrapper<BizfiAiKnowledgeChunk>()
                .eq(BizfiAiKnowledgeChunk::getFdocid, doc.getFdocid()));
        return knowledgeDocMapper.deleteById(doc.getFid()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rebuildChunks(String docId) {
        return replaceChunks(requireDoc(docId));
    }

    @Override
    public List<AiKnowledgeChunkResponse> listChunks(String docId) {
        BizfiAiKnowledgeDoc doc = requireDoc(docId);
        return knowledgeChunkMapper.selectList(new LambdaQueryWrapper<BizfiAiKnowledgeChunk>()
                        .eq(BizfiAiKnowledgeChunk::getFdocid, doc.getFdocid())
                        .orderByAsc(BizfiAiKnowledgeChunk::getFseq))
                .stream()
                .map(this::toChunkResponse)
                .toList();
    }

    @Override
    public List<String> listCategories() {
        return knowledgeDocMapper.selectList(new LambdaQueryWrapper<BizfiAiKnowledgeDoc>()
                        .isNotNull(BizfiAiKnowledgeDoc::getFcategory)
                        .orderByAsc(BizfiAiKnowledgeDoc::getFcategory))
                .stream()
                .map(BizfiAiKnowledgeDoc::getFcategory)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    @Override
    public List<AiCitationResponse> retrieve(String question, List<String> kbIds) {
        return retrieve(question, kbIds, DEFAULT_TOP_K);
    }

    @Override
    public List<AiCitationResponse> retrieve(String question, List<String> kbIds, Integer topK) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }
        List<String> keywords = extractKeywords(question);
        if (keywords.isEmpty()) {
            return List.of();
        }

        Set<String> allowedDocIds = scopeResolver.resolveAllowedDocumentIds(kbIds);
        if (allowedDocIds != null && allowedDocIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<BizfiAiKnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        if (allowedDocIds != null) {
            wrapper.in(BizfiAiKnowledgeChunk::getFdocid, allowedDocIds);
        }
        wrapper.and(q -> appendKeywordConditions(q, keywords))
                .orderByAsc(BizfiAiKnowledgeChunk::getFdocid)
                .orderByAsc(BizfiAiKnowledgeChunk::getFseq);

        List<BizfiAiKnowledgeChunk> chunks = knowledgeChunkMapper.selectList(wrapper);
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        Set<String> docIds = chunks.stream()
                .map(BizfiAiKnowledgeChunk::getFdocid)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, BizfiAiKnowledgeDoc> docMap = loadActiveDocMap(docIds);
        if (docMap.isEmpty()) {
            return List.of();
        }

        int limit = normalizeTopK(topK);
        return chunks.stream()
                .filter(chunk -> docMap.containsKey(chunk.getFdocid()))
                .map(chunk -> new ScoredKnowledgeChunk(
                        chunk,
                        docMap.get(chunk.getFdocid()),
                        score(chunk, docMap.get(chunk.getFdocid()), keywords)
                ))
                .filter(item -> item.score() > 0)
                .sorted(Comparator.comparingInt(ScoredKnowledgeChunk::score).reversed()
                        .thenComparing(item -> item.chunk().getFdocid())
                        .thenComparing(item -> item.chunk().getFseq(), Comparator.nullsLast(Integer::compareTo)))
                .limit(limit)
                .map(item -> new AiCitationResponse(
                        item.doc().getFdocid(),
                        item.doc().getFtitle(),
                        item.chunk().getFchunkid(),
                        buildSnippet(item.chunk().getFcontent(), keywords)
                ))
                .toList();
    }

    private LambdaQueryWrapper<BizfiAiKnowledgeChunk> appendKeywordConditions(
            LambdaQueryWrapper<BizfiAiKnowledgeChunk> wrapper,
            List<String> keywords
    ) {
        boolean first = true;
        for (String keyword : keywords) {
            if (first) {
                wrapper.like(BizfiAiKnowledgeChunk::getFkeywords, keyword)
                        .or()
                        .like(BizfiAiKnowledgeChunk::getFcontent, keyword);
                first = false;
            } else {
                wrapper.or(nested -> nested.like(BizfiAiKnowledgeChunk::getFkeywords, keyword)
                        .or()
                        .like(BizfiAiKnowledgeChunk::getFcontent, keyword));
            }
        }
        return wrapper;
    }

    private Map<String, BizfiAiKnowledgeDoc> loadActiveDocMap(Set<String> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return Map.of();
        }
        return knowledgeDocMapper.selectList(new LambdaQueryWrapper<BizfiAiKnowledgeDoc>()
                        .in(BizfiAiKnowledgeDoc::getFdocid, docIds)
                        .eq(BizfiAiKnowledgeDoc::getFstatus, "ACTIVE"))
                .stream()
                .collect(Collectors.toMap(
                        BizfiAiKnowledgeDoc::getFdocid,
                        Function.identity(),
                        (left, right) -> left
                ));
    }

    private int replaceChunks(BizfiAiKnowledgeDoc doc) {
        knowledgeChunkMapper.delete(new LambdaQueryWrapper<BizfiAiKnowledgeChunk>()
                .eq(BizfiAiKnowledgeChunk::getFdocid, doc.getFdocid()));

        List<String> chunks = splitContent(doc.getFcontent());
        LocalDateTime now = LocalDateTime.now();
        int seq = 1;
        for (String chunkText : chunks) {
            if (!StringUtils.hasText(chunkText)) {
                continue;
            }
            BizfiAiKnowledgeChunk chunk = new BizfiAiKnowledgeChunk();
            chunk.setFdocid(doc.getFdocid());
            chunk.setFchunkid(doc.getFdocid() + "_" + seq);
            chunk.setFseq(seq);
            chunk.setFcontent(chunkText.trim());
            chunk.setFkeywords(buildKeywords(doc, chunkText));
            chunk.setFcreatetime(now);
            knowledgeChunkMapper.insert(chunk);
            seq++;
        }
        return Math.max(0, seq - 1);
    }

    private List<String> splitContent(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n').trim();
        List<String> sections = splitByMarkdownHeading(normalized);
        List<String> chunks = new ArrayList<>();
        for (String section : sections) {
            chunks.addAll(splitLongSection(section));
        }
        return chunks;
    }

    private List<String> splitByMarkdownHeading(String content) {
        List<String> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : content.split("\n")) {
            if (line.startsWith("#") && current.length() > 0) {
                sections.add(current.toString().trim());
                current.setLength(0);
            }
            current.append(line).append('\n');
        }
        if (current.length() > 0) {
            sections.add(current.toString().trim());
        }
        return sections.isEmpty() ? List.of(content) : sections;
    }

    private List<String> splitLongSection(String section) {
        if (!StringUtils.hasText(section)) {
            return List.of();
        }
        String text = section.trim();
        if (text.length() <= MAX_CHUNK_LENGTH) {
            return List.of(text);
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int hardEnd = Math.min(text.length(), start + MAX_CHUNK_LENGTH);
            int end = hardEnd;
            int paragraphBreak = text.lastIndexOf("\n\n", hardEnd);
            if (paragraphBreak > start + 400) {
                end = paragraphBreak;
            }
            String chunk = text.substring(start, end).trim();
            if (StringUtils.hasText(chunk)) {
                chunks.add(chunk);
            }
            if (end >= text.length()) {
                break;
            }
            start = Math.max(start + 1, end - CHUNK_OVERLAP);
        }
        return chunks;
    }

    private List<String> extractKeywords(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String normalized = text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\r\\n\\t,，。.!！?？;；:：()（）\\[\\]{}<>《》\"'“”‘’、/\\\\|]+", " ")
                .trim();
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }

        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        for (String token : normalized.split("\\s+")) {
            if (!StringUtils.hasText(token)) {
                continue;
            }
            appendTokenKeywords(token.trim(), keywords);
            if (keywords.size() >= 24) {
                break;
            }
        }
        return keywords.stream().limit(24).toList();
    }

    private void appendTokenKeywords(String token, LinkedHashSet<String> keywords) {
        if (token.length() >= 2 && token.length() <= 32) {
            keywords.add(token);
        }
        if (!containsCjk(token)) {
            return;
        }
        for (String term : domainTerms()) {
            if (token.contains(term)) {
                keywords.add(term);
            }
        }
        int maxWindow = Math.min(4, token.length());
        for (int window = 2; window <= maxWindow; window++) {
            for (int i = 0; i + window <= token.length(); i++) {
                String part = token.substring(i, i + window);
                if (containsCjk(part)) {
                    keywords.add(part);
                }
                if (keywords.size() >= 24) {
                    return;
                }
            }
        }
    }

    private List<String> domainTerms() {
        return List.of(
                "总账", "凭证", "科目", "报表", "资产负债表", "利润表", "现金流量表",
                "应收", "应付", "往来", "核销", "月结", "结账", "过账", "辅助核算",
                "组织", "币种", "汇率", "客户", "供应商", "共享运营", "初始化"
        );
    }

    private boolean containsCjk(String text) {
        for (int i = 0; i < text.length(); i++) {
            Character.UnicodeScript script = Character.UnicodeScript.of(text.charAt(i));
            if (script == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private String buildKeywords(BizfiAiKnowledgeDoc doc, String text) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        appendTokenKeywords(safeLower(doc.getFtitle()), keywords);
        appendTokenKeywords(safeLower(doc.getFcategory()), keywords);
        for (String keyword : extractKeywords(text)) {
            keywords.add(keyword);
            if (keywords.size() >= 40) {
                break;
            }
        }
        return String.join(",", keywords.stream().limit(40).toList());
    }

    private int score(BizfiAiKnowledgeChunk chunk, BizfiAiKnowledgeDoc doc, List<String> keywords) {
        int score = 0;
        String content = safeLower(chunk.getFcontent());
        String keywordText = safeLower(chunk.getFkeywords());
        String title = safeLower(doc.getFtitle());
        String category = safeLower(doc.getFcategory());
        Set<String> matched = new HashSet<>();

        for (String keyword : keywords) {
            if (!matched.add(keyword)) {
                continue;
            }
            if (keywordText.contains(keyword)) {
                score += 6;
            }
            if (title.contains(keyword)) {
                score += 4;
            }
            if (category.contains(keyword)) {
                score += 2;
            }
            if (content.contains(keyword)) {
                score += containsCjk(keyword) && keyword.length() <= 2 ? 2 : 3;
            }
        }
        return score;
    }

    private String buildSnippet(String content, List<String> keywords) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String text = content.trim().replaceAll("\\s+", " ");
        String lower = text.toLowerCase(Locale.ROOT);
        int hit = -1;
        for (String keyword : keywords) {
            hit = lower.indexOf(keyword.toLowerCase(Locale.ROOT));
            if (hit >= 0) {
                break;
            }
        }
        if (hit < 0) {
            return limit(text, 220);
        }
        int start = Math.max(0, hit - 70);
        int end = Math.min(text.length(), hit + 150);
        String prefix = start > 0 ? "..." : "";
        String suffix = end < text.length() ? "..." : "";
        return prefix + text.substring(start, end).trim() + suffix;
    }

    private BizfiAiKnowledgeDoc findDoc(String docId) {
        if (!StringUtils.hasText(docId)) {
            return null;
        }
        return knowledgeDocMapper.selectOne(new LambdaQueryWrapper<BizfiAiKnowledgeDoc>()
                .eq(BizfiAiKnowledgeDoc::getFdocid, docId.trim())
                .last("limit 1"));
    }

    private BizfiAiKnowledgeDoc requireDoc(String docId) {
        BizfiAiKnowledgeDoc doc = findDoc(normalizeDocId(docId));
        if (doc == null) {
            throw new BizException("知识文档不存在");
        }
        return doc;
    }

    private AiKnowledgeDocSummaryResponse toSummary(BizfiAiKnowledgeDoc doc) {
        return new AiKnowledgeDocSummaryResponse(
                doc.getFid(),
                doc.getFkbid(),
                doc.getFdocid(),
                doc.getFtitle(),
                doc.getFcategory(),
                doc.getFsourcepath(),
                doc.getFversion(),
                doc.getFstatus(),
                countChunks(doc.getFdocid()),
                buildPreview(doc.getFcontent()),
                doc.getFcreatetime(),
                doc.getFmodifytime()
        );
    }

    private AiKnowledgeDocDetailResponse toDetail(BizfiAiKnowledgeDoc doc, boolean withChunks) {
        List<AiKnowledgeChunkResponse> chunks = withChunks ? listChunks(doc.getFdocid()) : List.of();
        return new AiKnowledgeDocDetailResponse(
                doc.getFid(),
                doc.getFkbid(),
                doc.getFdocid(),
                doc.getFtitle(),
                doc.getFcategory(),
                doc.getFsourcepath(),
                doc.getFcontent(),
                doc.getFversion(),
                doc.getFstatus(),
                chunks.size(),
                doc.getFcreatetime(),
                doc.getFmodifytime(),
                chunks
        );
    }

    private AiKnowledgeChunkResponse toChunkResponse(BizfiAiKnowledgeChunk chunk) {
        return new AiKnowledgeChunkResponse(
                chunk.getFid(),
                chunk.getFdocid(),
                chunk.getFchunkid(),
                chunk.getFseq(),
                chunk.getFcontent(),
                chunk.getFkeywords(),
                chunk.getFcreatetime()
        );
    }

    private int countChunks(String docId) {
        Long count = knowledgeChunkMapper.selectCount(new LambdaQueryWrapper<BizfiAiKnowledgeChunk>()
                .eq(BizfiAiKnowledgeChunk::getFdocid, docId));
        return count == null ? 0 : count.intValue();
    }

    private String buildPreview(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String preview = content.replaceAll("(?m)^#+\\s*", "")
                .replaceAll("\\s+", " ")
                .trim();
        return limit(preview, 160);
    }

    private String limit(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength) + "...";
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

    private String normalizeKbId(String kbId) {
        if (!StringUtils.hasText(kbId)) {
            return AiKnowledgeBaseServiceImpl.DEFAULT_KB_ID;
        }
        String normalized = kbId.trim()
                .replaceAll("[^A-Za-z0-9_-]", "_")
                .replaceAll("_+", "_")
                .toLowerCase(Locale.ROOT);
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }

    private String generateDocId() {
        return "kb_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private String normalizeCategory(String category) {
        return StringUtils.hasText(category) ? category.trim() : "通用知识";
    }

    private String normalizeSourcePath(String sourcePath, String docId) {
        return StringUtils.hasText(sourcePath) ? sourcePath.trim() : "manual://" + docId;
    }

    private String normalizeVersion(String version) {
        return StringUtils.hasText(version) ? version.trim() : "v1";
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (Objects.equals(normalized, "ENABLED") || Objects.equals(normalized, "PUBLISHED")) {
            return "ACTIVE";
        }
        if (Objects.equals(normalized, "DISABLED") || Objects.equals(normalized, "ARCHIVED")) {
            return "INACTIVE";
        }
        return normalized;
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    private String safeLower(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    private record ScoredKnowledgeChunk(BizfiAiKnowledgeChunk chunk, BizfiAiKnowledgeDoc doc, int score) {
    }
}
