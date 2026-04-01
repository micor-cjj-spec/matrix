package single.cjj.bizfi.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiCitationResponse {
    private String docId;
    private String docName;
    private String chunkId;
    private String snippet;
}
