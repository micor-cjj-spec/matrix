package single.cjj.bizfi.ai.service.impl;

import org.junit.jupiter.api.Test;
import single.cjj.bizfi.exception.BizException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiEvaluationQuestionDraftServiceTest {

    @Test
    void shouldDefaultUnlabelledQuestionsToInactive() {
        assertEquals("INACTIVE", AiEvaluationQuestionDraftService.resolveQuestionStatus(null, false));
    }

    @Test
    void shouldDefaultLabelledQuestionsToActive() {
        assertEquals("ACTIVE", AiEvaluationQuestionDraftService.resolveQuestionStatus(null, true));
    }

    @Test
    void shouldRejectActivationWithoutGroundTruth() {
        BizException failure = assertThrows(
                BizException.class,
                () -> AiEvaluationQuestionDraftService.resolveQuestionStatus("ACTIVE", false)
        );
        assertEquals("激活评测问题前必须绑定预期文档或预期分片", failure.getMessage());
    }

    @Test
    void shouldAllowExplicitInactiveQuestionWithGroundTruth() {
        assertEquals("INACTIVE", AiEvaluationQuestionDraftService.resolveQuestionStatus("inactive", true));
    }
}
