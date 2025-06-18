// CheckRequest.java
package single.cjj.bizfi.dto;

import lombok.Data;

@Data
public class CheckRequest {
    private String type; // email/nickname/phone
    private String value;
}
