package single.cjj.bizfi.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiMessageResponse {
    private String role;
    private String content;
}
