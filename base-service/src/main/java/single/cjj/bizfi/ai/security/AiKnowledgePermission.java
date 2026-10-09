package single.cjj.bizfi.ai.security;

import single.cjj.bizfi.exception.BizException;

import java.util.Locale;

public enum AiKnowledgePermission {
    VIEWER(10),
    EDITOR(20),
    ADMIN(30),
    OWNER(40);

    private final int rank;

    AiKnowledgePermission(int rank) {
        this.rank = rank;
    }

    public boolean allows(AiKnowledgePermission required) {
        return required != null && rank >= required.rank;
    }

    public static AiKnowledgePermission parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException("知识库权限不能为空");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            throw new BizException("不支持的知识库权限: " + value);
        }
    }
}
