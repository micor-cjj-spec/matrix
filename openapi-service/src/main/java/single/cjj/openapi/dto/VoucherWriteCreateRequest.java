package single.cjj.openapi.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class VoucherWriteCreateRequest {

    private String externalBizNo;
    private String idempotencyKey;
    private String organizationId;
    private String bookId;
    private LocalDate voucherDate;
    private String summary;
    private List<VoucherWriteLineRequest> lines;
}
