package single.cjj.openapi.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenVoucherDraftCreateResult {

    private Long voucherId;
    private String voucherNumber;
    private String status;
}
