package single.cjj.botp.rule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.RuleStatus;
import single.cjj.botp.persistence.entity.BotpRuleEntity;
import single.cjj.botp.persistence.entity.BotpRuleVersionEntity;
import single.cjj.botp.persistence.mapper.BotpRuleMapper;
import single.cjj.botp.persistence.mapper.BotpRuleVersionMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "botp.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class MybatisBotpRuleRepository implements BotpRuleRepository {

    private final BotpRuleMapper ruleMapper;
    private final BotpRuleVersionMapper versionMapper;
    private final ObjectMapper objectMapper;
    private final String tenantId;

    public MybatisBotpRuleRepository(
            BotpRuleMapper ruleMapper,
            BotpRuleVersionMapper versionMapper,
            ObjectMapper objectMapper,
            @Value("${botp.default-tenant:default}") String tenantId
    ) {
        this.ruleMapper = ruleMapper;
        this.versionMapper = versionMapper;
        this.objectMapper = objectMapper;
        this.tenantId = tenantId;
    }

    @Override
    public List<RuleDefinition> findAll() {
        return ruleMapper.selectList(new LambdaQueryWrapper<BotpRuleEntity>()
                        .eq(BotpRuleEntity::getFtenantId, tenantId)
                        .orderByDesc(BotpRuleEntity::getFmodifyTime)
                        .orderByAsc(BotpRuleEntity::getFcode))
                .stream()
                .map(BotpRuleEntity::getFcode)
                .map(this::findByCode)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public Optional<RuleDefinition> findByCode(String ruleCode) {
        Optional<BotpRuleVersionEntity> draft = findVersion(ruleCode, RuleStatus.DRAFT, false);
        return draft.map(this::deserialize).or(() -> findPublishedByCode(ruleCode));
    }

    @Override
    public Optional<RuleDefinition> findPublishedByCode(String ruleCode) {
        return findVersion(ruleCode, RuleStatus.PUBLISHED, true).map(this::deserialize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RuleDefinition saveDraft(RuleSaveRequest request) {
        LocalDateTime now = LocalDateTime.now();
        BotpRuleEntity rule = findRuleEntity(request.ruleCode()).orElseGet(() -> {
            BotpRuleEntity created = new BotpRuleEntity();
            created.setFtenantId(tenantId);
            created.setFcode(request.ruleCode());
            created.setFcurrentVersion(0);
            created.setFcreateTime(now);
            created.setFdeleteFlag(0);
            created.setFversion(0);
            ruleMapper.insert(created);
            return created;
        });

        int draftVersion = Optional.ofNullable(rule.getFcurrentVersion()).orElse(0) + 1;
        RuleDefinition draft = new RuleDefinition(
                request.ruleCode(),
                request.ruleName(),
                draftVersion,
                RuleStatus.DRAFT,
                request.sourceSystemCode(),
                request.sourceDocumentType(),
                request.targetSystemCode(),
                request.targetDocumentType(),
                request.headerMappings(),
                request.entryMappings(),
                request.writebackMappings()
        );
        String json = serialize(draft);

        BotpRuleVersionEntity version = findVersion(request.ruleCode(), RuleStatus.DRAFT, false)
                .orElseGet(BotpRuleVersionEntity::new);
        boolean insert = version.getFid() == null;
        version.setFtenantId(tenantId);
        version.setFruleId(rule.getFid());
        version.setFruleCode(request.ruleCode());
        version.setFversionNo(draftVersion);
        version.setFstatus(RuleStatus.DRAFT.name());
        version.setFdefinitionJson(json);
        version.setFpersistHash(hash(json));
        version.setFmodifyTime(now);
        if (insert) {
            version.setFcreateTime(now);
            version.setFdeleteFlag(0);
            version.setFversion(0);
            versionMapper.insert(version);
        } else {
            versionMapper.updateById(version);
        }

        rule.setFname(request.ruleName());
        rule.setFsourceSystemCode(request.sourceSystemCode());
        rule.setFsourceDocumentType(request.sourceDocumentType());
        rule.setFtargetSystemCode(request.targetSystemCode());
        rule.setFtargetDocumentType(request.targetDocumentType());
        rule.setFstatus(RuleStatus.DRAFT.name());
        rule.setFmodifyTime(now);
        ruleMapper.updateById(rule);
        return draft;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RuleDefinition publish(String ruleCode) {
        BotpRuleEntity rule = findRuleEntity(ruleCode)
                .orElseThrow(() -> new BizException("BOTP 规则不存在: " + ruleCode));
        BotpRuleVersionEntity draftEntity = findVersion(ruleCode, RuleStatus.DRAFT, false)
                .orElseThrow(() -> new BizException("BOTP 待发布草稿不存在: " + ruleCode));
        RuleDefinition draft = deserialize(draftEntity);
        RuleDefinition published = new RuleDefinition(
                draft.ruleCode(),
                draft.ruleName(),
                draft.version(),
                RuleStatus.PUBLISHED,
                draft.sourceSystemCode(),
                draft.sourceDocumentType(),
                draft.targetSystemCode(),
                draft.targetDocumentType(),
                draft.headerMappings(),
                draft.entryMappings(),
                draft.writebackMappings()
        );
        String json = serialize(published);
        LocalDateTime now = LocalDateTime.now();

        draftEntity.setFstatus(RuleStatus.PUBLISHED.name());
        draftEntity.setFdefinitionJson(json);
        draftEntity.setFpersistHash(hash(json));
        draftEntity.setFpublishedTime(now);
        draftEntity.setFmodifyTime(now);
        versionMapper.updateById(draftEntity);

        rule.setFcurrentVersion(published.version());
        rule.setFstatus(RuleStatus.PUBLISHED.name());
        rule.setFmodifyTime(now);
        ruleMapper.updateById(rule);
        return published;
    }

    @Override
    public List<RuleDefinition> findVersions(String ruleCode) {
        return versionMapper.selectList(new LambdaQueryWrapper<BotpRuleVersionEntity>()
                        .eq(BotpRuleVersionEntity::getFtenantId, tenantId)
                        .eq(BotpRuleVersionEntity::getFruleCode, ruleCode)
                        .eq(BotpRuleVersionEntity::getFstatus, RuleStatus.PUBLISHED.name())
                        .orderByAsc(BotpRuleVersionEntity::getFversionNo))
                .stream()
                .map(this::deserialize)
                .toList();
    }

    private Optional<BotpRuleEntity> findRuleEntity(String ruleCode) {
        return Optional.ofNullable(ruleMapper.selectOne(new LambdaQueryWrapper<BotpRuleEntity>()
                .eq(BotpRuleEntity::getFtenantId, tenantId)
                .eq(BotpRuleEntity::getFcode, ruleCode)
                .last("limit 1")));
    }

    private Optional<BotpRuleVersionEntity> findVersion(
            String ruleCode,
            RuleStatus status,
            boolean latestFirst
    ) {
        LambdaQueryWrapper<BotpRuleVersionEntity> query = new LambdaQueryWrapper<BotpRuleVersionEntity>()
                .eq(BotpRuleVersionEntity::getFtenantId, tenantId)
                .eq(BotpRuleVersionEntity::getFruleCode, ruleCode)
                .eq(BotpRuleVersionEntity::getFstatus, status.name());
        if (latestFirst) {
            query.orderByDesc(BotpRuleVersionEntity::getFversionNo);
        }
        query.last("limit 1");
        return Optional.ofNullable(versionMapper.selectOne(query));
    }

    private RuleDefinition deserialize(BotpRuleVersionEntity entity) {
        try {
            return objectMapper.readValue(entity.getFdefinitionJson(), RuleDefinition.class);
        } catch (JsonProcessingException exception) {
            throw new BizException("BOTP 规则快照解析失败: " + entity.getFruleCode());
        }
    }

    private String serialize(RuleDefinition definition) {
        try {
            return objectMapper.writeValueAsString(definition);
        } catch (JsonProcessingException exception) {
            throw new BizException("BOTP 规则快照序列化失败: " + definition.ruleCode());
        }
    }

    private String hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
