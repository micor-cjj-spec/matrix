package single.cjj.bizfi.ai.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.dto.AiFeedbackRequest;
import single.cjj.bizfi.ai.entity.BizfiAiFeedback;
import single.cjj.bizfi.ai.mapper.BizfiAiFeedbackMapper;
import single.cjj.bizfi.ai.service.AiConversationService;
import single.cjj.bizfi.ai.service.AiFeedbackService;
import single.cjj.bizfi.exception.BizException;

import java.time.LocalDateTime;

@Service
public class AiFeedbackServiceImpl implements AiFeedbackService {

    @Autowired
    private BizfiAiFeedbackMapper feedbackMapper;

    @Autowired
    private AiConversationService conversationService;

    @Override
    public Boolean saveFeedback(Long userId, AiFeedbackRequest request) {
        if (userId == null) {
            throw new BizException("用户ID不能为空");
        }
        if (request == null || !StringUtils.hasText(request.getConversationId())) {
            throw new BizException("会话ID不能为空");
        }
        if (!StringUtils.hasText(request.getFeedbackType())) {
            throw new BizException("反馈类型不能为空");
        }
        conversationService.getOwnedConversation(userId, request.getConversationId().trim());

        BizfiAiFeedback feedback = new BizfiAiFeedback();
        feedback.setFconversationid(request.getConversationId().trim());
        feedback.setFmessageid(request.getMessageId());
        feedback.setFuserid(userId);
        feedback.setFfeedbacktype(request.getFeedbackType().trim().toUpperCase());
        feedback.setFcomment(StringUtils.hasText(request.getComment()) ? request.getComment().trim() : null);
        feedback.setFcreatetime(LocalDateTime.now());
        feedbackMapper.insert(feedback);
        return true;
    }
}
