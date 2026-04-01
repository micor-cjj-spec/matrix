package single.cjj.bizfi.ai.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import single.cjj.bizfi.ai.service.AiCurrentUserService;
import single.cjj.bizfi.exception.BizException;

@Service
public class AiCurrentUserServiceImpl implements AiCurrentUserService {

    @Override
    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BizException("未获取到当前登录用户");
        }
        return Long.valueOf(authentication.getPrincipal().toString());
    }
}
