package single.cjj.bizfi.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Captcha {
    private String key;
    private String code;
    private String base64Image;
    // getters/setters
}