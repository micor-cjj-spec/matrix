package single.cjj.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import single.cjj.openapi.entity.OpenApiWriteRequestLine;
import single.cjj.openapi.entity.OpenApiWriteStatusLog;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherWriteDetailResponse {

    private VoucherWriteStatusResponse request;
    private List<OpenApiWriteRequestLine> lines;
    private List<OpenApiWriteStatusLog> logs;
}
