package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.ai.dto.AiKnowledgeBaseRequest;
import single.cjj.bizfi.ai.dto.AiKnowledgeBaseResponse;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeBase;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeBaseMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeDocMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiKnowledgeBaseServiceImplTest {

    @Mock
    private BizfiAiKnowledgeBaseMapper knowledgeBaseMapper;

    @Mock
    private BizfiAiKnowledgeDocMapper knowledgeDocMapper;

    @Mock
    private AiKnowledgeAclService aclService;

    private AiKnowledgeBaseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiKnowledgeBaseServiceImpl(knowledgeBaseMapper, knowledgeDocMapper, aclService);
        when(knowledgeDocMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
    }

    @Test
    void shouldNotAccessAclTableWhenAclIsDisabled() {
        when(knowledgeBaseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(aclService.isEnabled()).thenReturn(false);

        AiKnowledgeBaseResponse result = service.createBase(request("finance", "财务知识库"));

        assertEquals("finance", result.getKbId());
        verify(knowledgeBaseMapper).insert(any(BizfiAiKnowledgeBase.class));
        verify(aclService, never()).bootstrapOwner(any());
    }

    @Test
    void shouldBootstrapCreatorAsOwnerWhenAclIsEnabled() {
        when(knowledgeBaseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(aclService.isEnabled()).thenReturn(true);

        AiKnowledgeBaseResponse result = service.createBase(request("finance", "财务知识库"));

        assertEquals("finance", result.getKbId());
        verify(knowledgeBaseMapper).insert(any(BizfiAiKnowledgeBase.class));
        verify(aclService).bootstrapOwner("finance");
    }

    private AiKnowledgeBaseRequest request(String kbId, String name) {
        AiKnowledgeBaseRequest request = new AiKnowledgeBaseRequest();
        request.setKbId(kbId);
        request.setName(name);
        request.setDescription("企业财务制度与流程");
        request.setStatus("ACTIVE");
        return request;
    }
}
