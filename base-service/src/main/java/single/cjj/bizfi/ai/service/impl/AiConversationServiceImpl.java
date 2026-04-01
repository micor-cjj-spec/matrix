package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.entity.BizfiAiConversation;
import single.cjj.bizfi.ai.entity.BizfiAiMessage;
import single.cjj.bizfi.ai.mapper.BizfiAiConversationMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiMessageMapper;
import single.cjj.bizfi.ai.service.AiConversationService;
import single.cjj.bizfi.exception.BizException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiConversationServiceImpl
        extends ServiceImpl<BizfiAiConversationMapper, BizfiAiConversation>
        implements AiConversationService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String SOURCE_WEB = "WEB";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    @Autowired
    private BizfiAiConversationMapper conversationMapper;

    @Autowired
    private BizfiAiMessageMapper messageMapper;

    @Override
    public BizfiAiConversation createConversation(Long userId, String title, String scene) {
        if (userId == null) {
            throw new BizException("用户ID不能为空");
        }
        BizfiAiConversation conversation = new BizfiAiConversation();
        LocalDateTime now = LocalDateTime.now();
        conversation.setFconversationid(buildConversationId());
        conversation.setFuserid(userId);
        conversation.setFtitle(StringUtils.hasText(title) ? title.trim() : "新会话");
        conversation.setFscene(StringUtils.hasText(scene) ? scene.trim() : "knowledge_qa");
        conversation.setFstatus(STATUS_ACTIVE);
        conversation.setFsource(SOURCE_WEB);
        conversation.setFcreatetime(now);
        conversation.setFmodifytime(now);
        conversationMapper.insert(conversation);
        return conversation;
    }

    @Override
    public BizfiAiConversation getByConversationId(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return null;
        }
        return conversationMapper.selectOne(new LambdaQueryWrapper<BizfiAiConversation>()
                .eq(BizfiAiConversation::getFconversationid, conversationId.trim())
                .last("limit 1"));
    }

    @Override
    public BizfiAiConversation getOwnedConversation(Long userId, String conversationId) {
        BizfiAiConversation conversation = mustGetConversation(conversationId);
        if (!conversation.getFuserid().equals(userId)) {
            throw new BizException("无权访问该会话");
        }
        return conversation;
    }

    @Override
    public List<BizfiAiMessage> listMessages(Long userId, String conversationId) {
        getOwnedConversation(userId, conversationId);
        return messageMapper.selectList(new LambdaQueryWrapper<BizfiAiMessage>()
                .eq(BizfiAiMessage::getFconversationid, conversationId)
                .orderByAsc(BizfiAiMessage::getFid));
    }

    @Override
    public BizfiAiMessage saveUserMessage(String conversationId, String content) {
        if (!StringUtils.hasText(content)) {
            throw new BizException("用户消息不能为空");
        }
        mustGetConversation(conversationId);
        BizfiAiMessage message = buildMessage(conversationId, ROLE_USER, content.trim());
        messageMapper.insert(message);
        touchConversation(conversationId);
        return message;
    }

    @Override
    public BizfiAiMessage saveAssistantMessage(String conversationId, String content, String model, String mode,
                                              String traceId, Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        if (!StringUtils.hasText(content)) {
            throw new BizException("AI回复不能为空");
        }
        mustGetConversation(conversationId);
        BizfiAiMessage message = buildMessage(conversationId, ROLE_ASSISTANT, content.trim());
        message.setFmodel(trimToNull(model));
        message.setFmode(trimToNull(mode));
        message.setFtraceid(trimToNull(traceId));
        message.setFprompttokens(promptTokens == null ? 0 : promptTokens);
        message.setFcompletiontokens(completionTokens == null ? 0 : completionTokens);
        message.setFtotaltokens(totalTokens == null ? 0 : totalTokens);
        messageMapper.insert(message);
        touchConversation(conversationId);
        return message;
    }

    private BizfiAiConversation mustGetConversation(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            throw new BizException("会话ID不能为空");
        }
        BizfiAiConversation conversation = getByConversationId(conversationId.trim());
        if (conversation == null) {
            throw new BizException("会话不存在");
        }
        return conversation;
    }

    private BizfiAiMessage buildMessage(String conversationId, String role, String content) {
        BizfiAiMessage message = new BizfiAiMessage();
        message.setFconversationid(conversationId);
        message.setFrole(role);
        message.setFcontent(content);
        message.setFprompttokens(0);
        message.setFcompletiontokens(0);
        message.setFtotaltokens(0);
        message.setFcreatetime(LocalDateTime.now());
        return message;
    }

    private void touchConversation(String conversationId) {
        BizfiAiConversation conversation = mustGetConversation(conversationId);
        conversation.setFmodifytime(LocalDateTime.now());
        conversationMapper.updateById(conversation);
    }

    private String buildConversationId() {
        return "c_" + System.currentTimeMillis();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
