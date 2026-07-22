package single.cjj.openapi.contract;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OpenVoucherQuery {

    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String voucherNumber;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
}
