package single.cjj.bizfi.ai.service;

import single.cjj.bizfi.ai.dto.AiFeedbackRequest;

public interface AiFeedbackService {

    Boolean saveFeedback(Long userId, AiFeedbackRequest request);
}
