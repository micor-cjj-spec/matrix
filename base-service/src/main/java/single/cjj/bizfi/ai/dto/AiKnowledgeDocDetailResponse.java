package single.cjj.bizfi.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiKnowledgeDocDetailResponse {
    private Long fid;
    private String docId;
    private String title;
    private String category;
    private String sourcePath;
    private String content;
    private String version;
    private String status;
    private Integer chunkCount;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
    private List<AiKnowledgeChunkResponse> chunks;
}
