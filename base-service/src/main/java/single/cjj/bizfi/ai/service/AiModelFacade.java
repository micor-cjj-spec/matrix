package single.cjj.bizfi.ai.service;

import single.cjj.bizfi.ai.dto.AiConfigStatusResponse;
import single.cjj.bizfi.ai.dto.AiModelRequest;
import single.cjj.bizfi.ai.dto.AiModelResult;

public interface AiModelFacade {

    AiModelResult chat(AiModelRequest request);

    AiConfigStatusResponse configStatus();
}
