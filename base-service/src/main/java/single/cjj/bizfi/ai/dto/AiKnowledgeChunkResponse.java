package single.cjj.bizfi.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiKnowledgeChunkResponse {
    private Long fid;
    private String docId;
    private String chunkId;
    private Integer seq;
    private String content;
    private String keywords;
    private LocalDateTime createTime;
}
