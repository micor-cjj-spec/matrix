package single.cjj.bizfi.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiKnowledgeBaseResponse {
    private Long fid;
    private String kbId;
    private String name;
    private String description;
    private String status;
    private Integer documentCount;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
