package single.cjj.openapi.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenVoucherDraftCreateCommand {

    private String sourceRequestId;
    private String tenantId;
    private String organizationId;
    private String bookId;
    private LocalDate voucherDate;
    private String summary;
    private String createdBy;
    private List<OpenVoucherDraftLineCommand> lines;
}
