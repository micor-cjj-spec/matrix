package single.cjj.bizfi.ai.security;

import single.cjj.bizfi.exception.BizException;

import java.util.Locale;

public enum AiKnowledgeAclSubjectType {
    USER,
    ORGANIZATION,
    AUTHORITY;

    public static AiKnowledgeAclSubjectType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException("ACL主体类型不能为空");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            throw new BizException("不支持的ACL主体类型: " + value);
        }
    }
}
