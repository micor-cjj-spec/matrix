package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeDoc;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeDocMapper;
import single.cjj.bizfi.ai.security.AiKnowledgePermission;
import single.cjj.bizfi.exception.BizException;

@Service
public class AiKnowledgeAccessGuard {

    private final BizfiAiKnowledgeDocMapper docMapper;
    private final AiKnowledgeAclService aclService;

    public AiKnowledgeAccessGuard(
            BizfiAiKnowledgeDocMapper docMapper,
            AiKnowledgeAclService aclService
    ) {
        this.docMapper = docMapper;
        this.aclService = aclService;
    }

    public BizfiAiKnowledgeDoc assertCanViewDocument(String docId) {
        BizfiAiKnowledgeDoc doc = requireDocument(docId);
        aclService.assertCanView(doc.getFkbid());
        return doc;
    }

    public BizfiAiKnowledgeDoc assertCanEditDocument(String docId) {
        BizfiAiKnowledgeDoc doc = requireDocument(docId);
        aclService.assertCanEdit(doc.getFkbid());
        return doc;
    }

    public void assertCanRunGlobalKnowledgeOperation() {
        if (aclService.resolveAccessibleBaseIds(AiKnowledgePermission.OWNER) != null) {
            throw new BizException("ACL启用后，全库知识运维操作仅允许系统管理员执行");
        }
    }

    private BizfiAiKnowledgeDoc requireDocument(String docId) {
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
}
