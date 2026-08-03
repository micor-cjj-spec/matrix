package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.dto.AiKnowledgeBaseRequest;
import single.cjj.bizfi.ai.dto.AiKnowledgeBaseResponse;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeBase;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeDoc;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeBaseMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeDocMapper;
import single.cjj.bizfi.ai.service.AiKnowledgeBaseService;
import single.cjj.bizfi.exception.BizException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class AiKnowledgeBaseServiceImpl implements AiKnowledgeBaseService {

    public static final String DEFAULT_KB_ID = "default";
    private static final Set<String> RESERVED_GLOBAL_IDS = Set.of("all", "knowledge", "bizfi");

    private final BizfiAiKnowledgeBaseMapper knowledgeBaseMapper;
    private final BizfiAiKnowledgeDocMapper knowledgeDocMapper;

    public AiKnowledgeBaseServiceImpl(
            BizfiAiKnowledgeBaseMapper knowledgeBaseMapper,
            BizfiAiKnowledgeDocMapper knowledgeDocMapper
    ) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocMapper = knowledgeDocMapper;
    }

    @Override
    public List<AiKnowledgeBaseResponse> listBases(String status) {
        LambdaQueryWrapper<BizfiAiKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(BizfiAiKnowledgeBase::getFstatus, normalizeStatus(status));
        }
        wrapper.orderByDesc(BizfiAiKnowledgeBase::getFmodifytime)
                .orderByAsc(BizfiAiKnowledgeBase::getFid);
        return knowledgeBaseMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeBaseResponse createBase(AiKnowledgeBaseRequest request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BizException("知识库名称不能为空");
        }
        String kbId = normalizeKbId(request.getKbId());
        if (!StringUtils.hasText(kbId)) {
            kbId = generateKbId(request.getName());
        }
        validateNewKbId(kbId);
        if (findBase(kbId) != null) {
            throw new BizException("知识库编号已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        BizfiAiKnowledgeBase base = new BizfiAiKnowledgeBase();
        base.setFkbid(kbId);
        base.setFname(request.getName().trim());
        base.setFdescription(normalizeDescription(request.getDescription()));
        base.setFstatus(normalizeStatus(request.getStatus()));
        base.setFcreatetime(now);
        base.setFmodifytime(now);
        knowledgeBaseMapper.insert(base);
        return toResponse(base);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeBaseResponse updateBase(String kbId, AiKnowledgeBaseRequest request) {
        BizfiAiKnowledgeBase base = requireBase(kbId);
        if (request == null) {
            throw new BizException("知识库内容不能为空");
        }
        if (StringUtils.hasText(request.getName())) {
            base.setFname(request.getName().trim());
        }
        if (request.getDescription() != null) {
            base.setFdescription(normalizeDescription(request.getDescription()));
        }
        if (request.getStatus() != null) {
            String nextStatus = normalizeStatus(request.getStatus());
            if (DEFAULT_KB_ID.equals(base.getFkbid()) && !"ACTIVE".equals(nextStatus)) {
                throw new BizException("默认知识库必须保持启用");
            }
            base.setFstatus(nextStatus);
        }
        base.setFmodifytime(LocalDateTime.now());
        knowledgeBaseMapper.updateById(base);
        return toResponse(base);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBase(String kbId) {
        BizfiAiKnowledgeBase base = requireBase(kbId);
        if (DEFAULT_KB_ID.equals(base.getFkbid())) {
            throw new BizException("默认知识库不能删除");
        }
        long documentCount = countDocuments(base.getFkbid());
        if (documentCount > 0) {
            throw new BizException("知识库中仍有文档，请先迁移或删除文档");
        }
        return knowledgeBaseMapper.deleteById(base.getFid()) > 0;
    }

    public BizfiAiKnowledgeBase requireActiveBase(String kbId) {
        BizfiAiKnowledgeBase base = requireBase(StringUtils.hasText(kbId) ? kbId : DEFAULT_KB_ID);
        if (!"ACTIVE".equals(base.getFstatus())) {
            throw new BizException("知识库未启用");
        }
        return base;
    }

    private AiKnowledgeBaseResponse toResponse(BizfiAiKnowledgeBase base) {
        return new AiKnowledgeBaseResponse(
                base.getFid(),
                base.getFkbid(),
                base.getFname(),
                base.getFdescription(),
                base.getFstatus(),
                Math.toIntExact(countDocuments(base.getFkbid())),
                base.getFcreatetime(),
                base.getFmodifytime()
        );
    }

    private long countDocuments(String kbId) {
        Long count = knowledgeDocMapper.selectCount(new LambdaQueryWrapper<BizfiAiKnowledgeDoc>()
                .eq(BizfiAiKnowledgeDoc::getFkbid, kbId));
        return count == null ? 0L : count;
    }

    private BizfiAiKnowledgeBase findBase(String kbId) {
        if (!StringUtils.hasText(kbId)) {
            return null;
        }
        return knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<BizfiAiKnowledgeBase>()
                .eq(BizfiAiKnowledgeBase::getFkbid, normalizeKbId(kbId))
                .last("limit 1"));
    }

    private BizfiAiKnowledgeBase requireBase(String kbId) {
        BizfiAiKnowledgeBase base = findBase(kbId);
        if (base == null) {
            throw new BizException("知识库不存在");
        }
        return base;
    }

    private void validateNewKbId(String kbId) {
        if (!StringUtils.hasText(kbId)) {
            throw new BizException("知识库编号不能为空");
        }
        if (RESERVED_GLOBAL_IDS.contains(kbId)) {
            throw new BizException("该知识库编号为系统保留范围标识");
        }
    }

    private String normalizeKbId(String kbId) {
        if (!StringUtils.hasText(kbId)) {
            return "";
        }
        String normalized = kbId.trim()
                .replaceAll("[^A-Za-z0-9_-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "")
                .toLowerCase(Locale.ROOT);
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }

    private String generateKbId(String name) {
        String normalized = normalizeKbId(name);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        return "kb_" + Long.toUnsignedString(System.nanoTime(), 36);
    }

    private String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        String normalized = description.trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
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
}
