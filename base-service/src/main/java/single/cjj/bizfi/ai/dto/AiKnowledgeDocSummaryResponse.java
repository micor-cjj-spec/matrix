package single.cjj.bizfi.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiKnowledgeDocSummaryResponse {
    private Long fid;
    private String docId;
    private String title;
    private String category;
    private String sourcePath;
    private String version;
    private String status;
    private Integer chunkCount;
    private String contentPreview;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
