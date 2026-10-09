package single.cjj.erp.procurement.sourcing.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.erp.procurement.sourcing.dto.SourcingContracts.*;
import single.cjj.erp.procurement.sourcing.entity.ProcurementRfqEntity;
import single.cjj.erp.procurement.sourcing.entity.SupplierQuoteEntity;
import single.cjj.erp.procurement.sourcing.service.ProcurementSourcingService;
import java.util.List;

@RestController
@RequestMapping("/procurement/rfqs")
public class ProcurementSourcingController {
 private final ProcurementSourcingService service;
 public ProcurementSourcingController(ProcurementSourcingService service){this.service=service;}

 @PostMapping public ApiResponse<RfqDetail> create(@Valid @RequestBody RfqCreateRequest request,
   @RequestHeader(value="X-Operator-Id",required=false) Long operatorId){
  return ApiResponse.success(service.createRfq(request,operatorId));
 }
 @GetMapping("/{fid}") public ApiResponse<RfqDetail> detail(@PathVariable Long fid,@RequestParam String tenantId){
  return ApiResponse.success(service.rfqDetail(fid,tenantId));
 }
 @GetMapping public ApiResponse<IPage<ProcurementRfqEntity>> page(@RequestParam String tenantId,
   @RequestParam(required=false) Long orgId,@RequestParam(required=false) String status,
   @RequestParam(required=false) String number,@RequestParam(defaultValue="1") int page,
   @RequestParam(defaultValue="20") int size){
  return ApiResponse.success(service.pageRfqs(tenantId,orgId,status,number,page,size));
 }
 @PostMapping("/{fid}/publish") public ApiResponse<RfqDetail> publish(@PathVariable Long fid,@RequestParam String tenantId,
   @RequestHeader(value="X-Operator-Id",required=false) Long operatorId){
  return ApiResponse.success(service.publishRfq(fid,tenantId,operatorId));
 }
 @PostMapping("/{fid}/cancel") public ApiResponse<RfqDetail> cancel(@PathVariable Long fid,@RequestParam String tenantId,
   @RequestHeader(value="X-Operator-Id",required=false) Long operatorId){
  return ApiResponse.success(service.cancelDraftRfq(fid,tenantId,operatorId));
 }
 @PostMapping("/{fid}/quotes") public ApiResponse<QuoteDetail> createQuote(@PathVariable Long fid,
   @Valid @RequestBody QuoteCreateRequest request,@RequestHeader(value="X-Operator-Id",required=false) Long operatorId){
  return ApiResponse.success(service.createQuote(fid,request,operatorId));
 }
 @PostMapping("/{fid}/quotes/{quoteId}/submit") public ApiResponse<QuoteDetail> submitQuote(@PathVariable Long fid,
   @PathVariable Long quoteId,@RequestParam String tenantId,@RequestHeader(value="X-Operator-Id",required=false) Long operatorId){
  return ApiResponse.success(service.submitQuote(fid,quoteId,tenantId,operatorId));
 }
 @GetMapping("/{fid}/quotes") public ApiResponse<List<SupplierQuoteEntity>> quotes(@PathVariable Long fid,@RequestParam String tenantId){
  return ApiResponse.success(service.listQuotes(fid,tenantId));
 }
 @GetMapping("/{fid}/comparison") public ApiResponse<List<ComparisonLine>> comparison(@PathVariable Long fid,@RequestParam String tenantId){
  return ApiResponse.success(service.comparison(fid,tenantId));
 }
 @PostMapping("/{fid}/awards") public ApiResponse<AwardDetail> award(@PathVariable Long fid,
   @Valid @RequestBody AwardCreateRequest request,@RequestHeader(value="X-Operator-Id",required=false) Long operatorId){
  return ApiResponse.success(service.confirmAward(fid,request,operatorId));
 }
}
