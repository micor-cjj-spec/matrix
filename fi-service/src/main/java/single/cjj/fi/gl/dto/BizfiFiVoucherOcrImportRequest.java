package single.cjj.fi.gl.dto;

import lombok.Data;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.entity.BizfiFiVoucherLine;

import java.util.List;

@Data
public class BizfiFiVoucherOcrImportRequest {
    private BizfiFiVoucher voucher;
    private List<BizfiFiVoucherLine> lines;
}
