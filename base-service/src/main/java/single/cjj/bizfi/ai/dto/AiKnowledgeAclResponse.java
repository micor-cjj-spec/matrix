package single.cjj.bizfi.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiKnowledgeAclResponse {
    private Long id;
    private String kbId;
    private String subjectType;
    private String subjectId;
    private String permission;
    private Long createdBy;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
