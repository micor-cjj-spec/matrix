package single.cjj.fi.fund.bank;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.fund.bank.BankTransactionContracts.CreateRequest;
import single.cjj.fi.fund.bank.BankTransactionContracts.Detail;
import single.cjj.fi.fund.bank.BankTransactionContracts.MatchRequest;
import single.cjj.fi.fund.bank.BankTransactionContracts.MatchResult;

import java.util.List;

@RestController
@RequestMapping("/fund/bank-transactions")
public class BankTransactionController {

    private final BankTransactionService service;

    public BankTransactionController(BankTransactionService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<Detail> create(
            @Valid @RequestBody CreateRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.create(request, operatorId));
    }

    @GetMapping("/{fid}")
    public ApiResponse<Detail> detail(
            @PathVariable Long fid,
            @RequestParam String tenantId
    ) {
        return ApiResponse.success(service.detail(fid, tenantId));
    }

    @GetMapping
    public ApiResponse<List<Detail>> list(
            @RequestParam String tenantId,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String matchStatus,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ApiResponse.success(service.list(tenantId, orgId, matchStatus, limit));
    }

    @PostMapping("/{fid}/match-payment-order")
    public ApiResponse<MatchResult> matchPaymentOrder(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @Valid @RequestBody MatchRequest request
    ) {
        return ApiResponse.success(service.matchPaymentOrder(fid, tenantId, request));
    }
}
