package single.cjj.fi.ai.tool;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.fi.gl.service.BizfiFiPeriodProcessService;
import single.cjj.fi.gl.vo.MonthEndWorkbenchResultVO;

@RestController
@RequestMapping("/internal/ai/tools")
public class FinanceAiToolController {

    private final FinanceAiToolTokenGuard tokenGuard;
    private final BizfiFiPeriodProcessService periodProcessService;
    private final FinanceMonthEndCloseToolMapper mapper;

    public FinanceAiToolController(
            FinanceAiToolTokenGuard tokenGuard,
            BizfiFiPeriodProcessService periodProcessService,
            FinanceMonthEndCloseToolMapper mapper
    ) {
        this.tokenGuard = tokenGuard;
        this.periodProcessService = periodProcessService;
        this.mapper = mapper;
    }

    @PostMapping("/month-end-close-check")
    public FinanceMonthEndCloseToolResponse monthEndCloseCheck(
            @RequestHeader(value = FinanceAiToolTokenGuard.HEADER_NAME, required = false) String internalToken,
            @Valid @RequestBody FinanceMonthEndCloseToolRequest request
    ) {
        tokenGuard.verify(internalToken);
        MonthEndWorkbenchResultVO result = periodProcessService.monthEndWorkbench(
                request.organizationId(),
                request.period()
        );
        return mapper.map(result);
    }
}
