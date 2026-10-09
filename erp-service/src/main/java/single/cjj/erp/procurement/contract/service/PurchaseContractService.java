package single.cjj.erp.procurement.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.procurement.contract.dto.PurchaseContractContracts.*;
import single.cjj.erp.procurement.contract.entity.PurchaseContractEntity;
import single.cjj.erp.procurement.contract.entity.PurchaseContractEntryEntity;
import single.cjj.erp.procurement.contract.mapper.PurchaseContractEntryMapper;
import single.cjj.erp.procurement.contract.mapper.PurchaseContractMapper;
import single.cjj.erp.procurement.sourcing.entity.ProcurementRfqEntity;
import single.cjj.erp.procurement.sourcing.entity.ProcurementRfqEntryEntity;
import single.cjj.erp.procurement.sourcing.entity.SourcingAwardEntity;
import single.cjj.erp.procurement.sourcing.entity.SourcingAwardEntryEntity;
import single.cjj.erp.procurement.sourcing.entity.SupplierQuoteEntryEntity;
import single.cjj.erp.procurement.sourcing.mapper.ProcurementRfqEntryMapper;
import single.cjj.erp.procurement.sourcing.mapper.ProcurementRfqMapper;
import single.cjj.erp.procurement.sourcing.mapper.SourcingAwardEntryMapper;
import single.cjj.erp.procurement.sourcing.mapper.SourcingAwardMapper;
import single.cjj.erp.procurement.sourcing.mapper.SupplierQuoteEntryMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PurchaseContractService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_EFFECTIVE = "EFFECTIVE";
    private static final String APPROVAL_DRAFT = "DRAFT";
    private static final String APPROVAL_SUBMITTED = "SUBMITTED";
    private static final String APPROVAL_APPROVED = "APPROVED";
    private static final String APPROVAL_REJECTED = "REJECTED";
    private static final String EXECUTION_NONE = "NONE";
    private static final String AWARD_CONFIRMED = "CONFIRMED";
    private static final String EVENT_EFFECTIVE = "PURCHASE_CONTRACT_EFFECTIVE";

    private final PurchaseContractMapper contractMapper;
    private final PurchaseContractEntryMapper contractEntryMapper;
    private final SourcingAwardMapper awardMapper;
    private final SourcingAwardEntryMapper awardEntryMapper;
    private final ProcurementRfqMapper rfqMapper;
    private final ProcurementRfqEntryMapper rfqEntryMapper;
    private final SupplierQuoteEntryMapper quoteEntryMapper;
    private final BusinessEventOutboxService outboxService;

    public PurchaseContractService(
            PurchaseContractMapper contractMapper,
            PurchaseContractEntryMapper contractEntryMapper,
            SourcingAwardMapper awardMapper,
            SourcingAwardEntryMapper awardEntryMapper,
            ProcurementRfqMapper rfqMapper,
            ProcurementRfqEntryMapper rfqEntryMapper,
            SupplierQuoteEntryMapper quoteEntryMapper,
            BusinessEventOutboxService outboxService
    ) {
        this.contractMapper = contractMapper;
        this.contractEntryMapper = contractEntryMapper;
        this.awardMapper = awardMapper;
        this.awardEntryMapper = awardEntryMapper;
        this.rfqMapper = rfqMapper;
        this.rfqEntryMapper = rfqEntryMapper;
        this.quoteEntryMapper = quoteEntryMapper;
        this.outboxService = outboxService;
    }

    public Detail detail(Long fid, String tenantId) {
        PurchaseContractEntity contract = requireContract(fid, tenantId, false);
        return new Detail(contract, listEntries(fid));
    }

    public IPage<PurchaseContractEntity> page(
            String tenantId,
            Long orgId,
            String number,
            Long businessPartnerId,
            String approvalStatus,
            String status,
            int page,
            int size
    ) {
        String tenant = requireTenant(tenantId);
        return contractMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.max(size, 1)),
                new LambdaQueryWrapper<PurchaseContractEntity>()
                        .eq(PurchaseContractEntity::getFtenantId, tenant)
                        .eq(orgId != null, PurchaseContractEntity::getForgId, orgId)
                        .like(StringUtils.hasText(number), PurchaseContractEntity::getFnumber, number)
                        .eq(businessPartnerId != null, PurchaseContractEntity::getFbusinessPartnerId, businessPartnerId)
                        .eq(StringUtils.hasText(approvalStatus), PurchaseContractEntity::getFapprovalStatus, approvalStatus)
                        .eq(StringUtils.hasText(status), PurchaseContractEntity::getFstatus, status)
                        .orderByDesc(PurchaseContractEntity::getFdate)
                        .orderByDesc(PurchaseContractEntity::getFid)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail create(CreateRequest request, Long operatorId) {
        String tenant = requireTenant(request.ftenantId());
        validateDateRange(request.fstartDate(), request.fendDate());
        SourcingAwardEntity award = requireAward(request.fsourcingAwardId(), tenant);
        if (!request.forgId().equals(award.getForgId())) {
            throw new BizException("采购合同组织与定标组织不一致");
        }

        ProcurementRfqEntity rfq = requireRfq(award.getFrfqId(), tenant);
        Long contractId = IdWorker.getId();
        LocalDate contractDate = request.fdate() == null ? LocalDate.now() : request.fdate();
        String number = StringUtils.hasText(request.fnumber())
                ? request.fnumber().trim()
                : buildNumber(contractDate, contractId);
        ensureNumberUnique(tenant, number);

        CalculatedEntries calculated = buildEntries(
                contractId, tenant, request.forgId(), award, null,
                request.entries(), operatorId
        );

        PurchaseContractEntity contract = new PurchaseContractEntity();
        contract.setFid(contractId);
        contract.setFtenantId(tenant);
        contract.setForgId(request.forgId());
        contract.setFnumber(number);
        contract.setFdate(contractDate);
        contract.setFtitle(trimToNull(request.ftitle()));
        contract.setFsourcingAwardId(award.getFid());
        contract.setFbusinessPartnerId(calculated.businessPartnerId());
        contract.setFbusinessPartnerCode(calculated.businessPartnerCode());
        contract.setFbusinessPartnerName(calculated.businessPartnerName());
        contract.setFcurrencyCode(rfq.getFcurrencyCode());
        contract.setFstartDate(request.fstartDate());
        contract.setFendDate(request.fendDate());
        contract.setFpaymentTermCode(trimToNull(request.fpaymentTermCode()));
        contract.setFdeliveryTermCode(trimToNull(request.fdeliveryTermCode()));
        contract.setFtotalQuantity(calculated.totalQuantity());
        contract.setFnetAmount(calculated.netAmount());
        contract.setFtaxAmount(calculated.taxAmount());
        contract.setFgrossAmount(calculated.grossAmount());
        contract.setFstatus(STATUS_DRAFT);
        contract.setFapprovalStatus(APPROVAL_DRAFT);
        contract.setFexecutionStatus(EXECUTION_NONE);
        contract.setFremark(trimToNull(request.fremark()));
        initAudit(contract, operatorId);
        requireOne(contractMapper.insert(contract), "采购合同");
        insertEntries(calculated.entries());
        return new Detail(contract, calculated.entries());
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail update(Long fid, UpdateRequest request, Long operatorId) {
        String tenant = requireTenant(request.ftenantId());
        validateDateRange(request.fstartDate(), request.fendDate());
        PurchaseContractEntity contract = requireContract(fid, tenant, true);
        ensureEditable(contract);
        if (!request.forgId().equals(contract.getForgId())) {
            throw new BizException("采购合同组织不允许通过编辑变更");
        }

        SourcingAwardEntity award = requireAward(contract.getFsourcingAwardId(), tenant);
        CalculatedEntries calculated = buildEntries(
                fid, tenant, contract.getForgId(), award, fid,
                request.entries(), operatorId
        );
        if (!contract.getFbusinessPartnerId().equals(calculated.businessPartnerId())) {
            throw new BizException("采购合同供应商不允许通过编辑变更");
        }

        contract.setFtitle(trimToNull(request.ftitle()));
        contract.setFstartDate(request.fstartDate());
        contract.setFendDate(request.fendDate());
        contract.setFpaymentTermCode(trimToNull(request.fpaymentTermCode()));
        contract.setFdeliveryTermCode(trimToNull(request.fdeliveryTermCode()));
        contract.setFtotalQuantity(calculated.totalQuantity());
        contract.setFnetAmount(calculated.netAmount());
        contract.setFtaxAmount(calculated.taxAmount());
        contract.setFgrossAmount(calculated.grossAmount());
        contract.setFremark(trimToNull(request.fremark()));
        touch(contract, operatorId);
        requireOne(contractMapper.updateById(contract), "采购合同");

        contractEntryMapper.delete(new LambdaQueryWrapper<PurchaseContractEntryEntity>()
                .eq(PurchaseContractEntryEntity::getFpurchaseContractId, fid));
        insertEntries(calculated.entries());
        return new Detail(contract, calculated.entries());
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail submit(Long fid, String tenantId, Long operatorId) {
        PurchaseContractEntity contract = requireContract(fid, tenantId, true);
        if (!STATUS_DRAFT.equals(contract.getFstatus())
                || !(APPROVAL_DRAFT.equals(contract.getFapprovalStatus())
                || APPROVAL_REJECTED.equals(contract.getFapprovalStatus()))) {
            throw new BizException("仅草稿或已驳回采购合同允许提交");
        }
        if (listEntries(fid).isEmpty()) {
            throw new BizException("采购合同至少需要一条分录");
        }
        contract.setFapprovalStatus(APPROVAL_SUBMITTED);
        contract.setFrejectReason(null);
        touch(contract, operatorId);
        requireOne(contractMapper.updateById(contract), "采购合同");
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Detail applyApprovalResult(Long fid, String tenantId, ApprovalResultRequest result) {
        PurchaseContractEntity contract = requireContract(fid, tenantId, true);
        if (!APPROVAL_SUBMITTED.equals(contract.getFapprovalStatus())) {
            throw new BizException("只有已提交采购合同允许回写审批结果");
        }

        String approval = result.status().trim().toUpperCase();
        Long operatorId = result.operatorId();
        contract.setFworkflowInstanceId(trimToNull(result.workflowInstanceId()));

        if (APPROVAL_APPROVED.equals(approval)) {
            contract.setFapprovalStatus(APPROVAL_APPROVED);
            contract.setFstatus(STATUS_EFFECTIVE);
            contract.setFrejectReason(null);
            contract.setFapprovedBy(operatorId);
            contract.setFapprovedTime(LocalDateTime.now());
            touch(contract, operatorId);
            requireOne(contractMapper.updateById(contract), "采购合同");

            List<PurchaseContractEntryEntity> entries = listEntries(fid);
            outboxService.append(
                    contract.getFtenantId(),
                    contract.getForgId(),
                    EVENT_EFFECTIVE,
                    "PURCHASE_CONTRACT",
                    contract.getFid(),
                    contract.getFversion() == null ? 0L : contract.getFversion().longValue(),
                    "ERP_PURCHASE_CONTRACT",
                    contract.getFnumber(),
                    contract.getFdate(),
                    operatorId,
                    effectivePayload(contract, entries)
            );
            return new Detail(contract, entries);
        }

        if (APPROVAL_REJECTED.equals(approval)) {
            contract.setFapprovalStatus(APPROVAL_REJECTED);
            contract.setFstatus(STATUS_DRAFT);
            contract.setFrejectReason(trimToNull(result.reason()));
            touch(contract, operatorId);
            requireOne(contractMapper.updateById(contract), "采购合同");
            return detail(fid, tenantId);
        }

        throw new BizException("采购合同审批结果仅支持 APPROVED / REJECTED");
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long fid, String tenantId) {
        PurchaseContractEntity contract = requireContract(fid, tenantId, true);
        ensureEditable(contract);
        contractEntryMapper.delete(new LambdaQueryWrapper<PurchaseContractEntryEntity>()
                .eq(PurchaseContractEntryEntity::getFpurchaseContractId, fid));
        return contractMapper.deleteById(fid) > 0;
    }

    private CalculatedEntries buildEntries(
            Long contractId,
            String tenantId,
            Long orgId,
            SourcingAwardEntity award,
            Long excludeContractId,
            List<EntryRequest> requests,
            Long operatorId
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new BizException("采购合同至少需要一条分录");
        }

        List<EntryRequest> sorted = new ArrayList<>(requests);
        sorted.sort(Comparator.comparing(EntryRequest::fsourcingAwardEntryId));
        Set<Long> seen = new LinkedHashSet<>();
        List<PurchaseContractEntryEntity> entries = new ArrayList<>();
        Long supplierId = null;
        String supplierCode = null;
        String supplierName = null;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal netTotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        BigDecimal grossTotal = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < sorted.size(); i++) {
            EntryRequest item = sorted.get(i);
            if (!seen.add(item.fsourcingAwardEntryId())) {
                throw new BizException("同一采购合同不能重复引用同一定标分录: " + item.fsourcingAwardEntryId());
            }

            SourcingAwardEntryEntity awardEntry =
                    awardEntryMapper.selectByIdForUpdate(item.fsourcingAwardEntryId(), tenantId);
            if (awardEntry == null || !award.getFid().equals(awardEntry.getFawardId())) {
                throw new BizException("定标分录不存在或不属于当前定标单: " + item.fsourcingAwardEntryId());
            }

            if (supplierId == null) {
                supplierId = awardEntry.getFbusinessPartnerId();
                supplierCode = awardEntry.getFbusinessPartnerCode();
                supplierName = awardEntry.getFbusinessPartnerName();
            } else if (!supplierId.equals(awardEntry.getFbusinessPartnerId())) {
                throw new BizException("一个采购合同只能对应一个供应商，请按中标供应商拆分合同");
            }

            BigDecimal alreadyContracted = contractedQuantity(
                    awardEntry.getFid(), excludeContractId
            );
            BigDecimal remaining = nz(awardEntry.getFawardedQuantity()).subtract(alreadyContracted);
            if (item.fquantity().compareTo(remaining) > 0) {
                throw new BizException("合同数量超过定标剩余可签约数量: awardEntry="
                        + awardEntry.getFid() + ", remaining=" + remaining);
            }

            ProcurementRfqEntryEntity rfqEntry = rfqEntryMapper.selectById(awardEntry.getFrfqEntryId());
            if (rfqEntry == null || !tenantId.equals(rfqEntry.getFtenantId())) {
                throw new BizException("来源询价分录不存在: " + awardEntry.getFrfqEntryId());
            }

            BigDecimal net = money(item.fquantity().multiply(awardEntry.getFunitPrice()));
            BigDecimal tax = money(net.multiply(nz(awardEntry.getFtaxRate())));
            BigDecimal gross = money(net.add(tax));
            SupplierQuoteEntryEntity quoteEntry = quoteEntryMapper.selectById(awardEntry.getFquoteEntryId());

            PurchaseContractEntryEntity entry = new PurchaseContractEntryEntity();
            entry.setFid(IdWorker.getId());
            entry.setFtenantId(tenantId);
            entry.setForgId(orgId);
            entry.setFpurchaseContractId(contractId);
            entry.setFlineNo(i + 1);
            entry.setFsourcingAwardEntryId(awardEntry.getFid());
            entry.setFrfqEntryId(awardEntry.getFrfqEntryId());
            entry.setFpurchaseRequestId(rfqEntry.getFpurchaseRequestId());
            entry.setFpurchaseRequestEntryId(rfqEntry.getFpurchaseRequestEntryId());
            entry.setFmaterialId(rfqEntry.getFmaterialId());
            entry.setFmaterialCode(rfqEntry.getFmaterialCode());
            entry.setFmaterialName(rfqEntry.getFmaterialName());
            entry.setFspecification(rfqEntry.getFspecification());
            entry.setFunitId(rfqEntry.getFunitId());
            entry.setFquantity(item.fquantity());
            entry.setFunitPrice(awardEntry.getFunitPrice());
            entry.setFtaxRate(nz(awardEntry.getFtaxRate()));
            entry.setFnetAmount(net);
            entry.setFtaxAmount(tax);
            entry.setFgrossAmount(gross);
            entry.setFplannedDeliveryDate(item.fplannedDeliveryDate() != null
                    ? item.fplannedDeliveryDate()
                    : quoteEntry == null ? null : quoteEntry.getFdeliveryDate());
            entry.setFprojectId(rfqEntry.getFprojectId());
            entry.setFcostCenterId(rfqEntry.getFcostCenterId());
            entry.setForderedQuantity(BigDecimal.ZERO);
            entry.setFcreateBy(operatorId);
            entry.setFcreateTime(now);
            entry.setFmodifyBy(operatorId);
            entry.setFmodifyTime(now);
            entry.setFdeleteFlag(0);
            entry.setFversion(0);
            entries.add(entry);

            totalQuantity = totalQuantity.add(item.fquantity());
            netTotal = netTotal.add(net);
            taxTotal = taxTotal.add(tax);
            grossTotal = grossTotal.add(gross);
        }

        return new CalculatedEntries(
                List.copyOf(entries),
                supplierId,
                supplierCode,
                supplierName,
                totalQuantity,
                money(netTotal),
                money(taxTotal),
                money(grossTotal)
        );
    }

    private BigDecimal contractedQuantity(Long awardEntryId, Long excludeContractId) {
        List<PurchaseContractEntryEntity> entries = contractEntryMapper.selectList(
                new LambdaQueryWrapper<PurchaseContractEntryEntity>()
                        .eq(PurchaseContractEntryEntity::getFsourcingAwardEntryId, awardEntryId)
        );
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseContractEntryEntity entry : entries) {
            if (excludeContractId != null && excludeContractId.equals(entry.getFpurchaseContractId())) {
                continue;
            }
            total = total.add(nz(entry.getFquantity()));
        }
        return total;
    }

    private PurchaseContractEntity requireContract(Long fid, String tenantId, boolean forUpdate) {
        String tenant = requireTenant(tenantId);
        PurchaseContractEntity contract = forUpdate
                ? contractMapper.selectByIdForUpdate(fid, tenant)
                : contractMapper.selectOne(new LambdaQueryWrapper<PurchaseContractEntity>()
                        .eq(PurchaseContractEntity::getFid, fid)
                        .eq(PurchaseContractEntity::getFtenantId, tenant)
                        .last("limit 1"));
        if (contract == null) {
            throw new BizException("采购合同不存在: " + fid);
        }
        return contract;
    }

    private SourcingAwardEntity requireAward(Long awardId, String tenantId) {
        SourcingAwardEntity award = awardMapper.selectOne(
                new LambdaQueryWrapper<SourcingAwardEntity>()
                        .eq(SourcingAwardEntity::getFid, awardId)
                        .eq(SourcingAwardEntity::getFtenantId, tenantId)
                        .last("limit 1")
        );
        if (award == null) {
            throw new BizException("采购定标不存在: " + awardId);
        }
        if (!AWARD_CONFIRMED.equals(award.getFstatus())) {
            throw new BizException("只有已确认定标允许生成采购合同");
        }
        return award;
    }

    private ProcurementRfqEntity requireRfq(Long rfqId, String tenantId) {
        ProcurementRfqEntity rfq = rfqMapper.selectOne(
                new LambdaQueryWrapper<ProcurementRfqEntity>()
                        .eq(ProcurementRfqEntity::getFid, rfqId)
                        .eq(ProcurementRfqEntity::getFtenantId, tenantId)
                        .last("limit 1")
        );
        if (rfq == null) {
            throw new BizException("来源询价单不存在: " + rfqId);
        }
        return rfq;
    }

    private List<PurchaseContractEntryEntity> listEntries(Long contractId) {
        return contractEntryMapper.selectList(
                new LambdaQueryWrapper<PurchaseContractEntryEntity>()
                        .eq(PurchaseContractEntryEntity::getFpurchaseContractId, contractId)
                        .orderByAsc(PurchaseContractEntryEntity::getFlineNo)
        );
    }

    private void insertEntries(List<PurchaseContractEntryEntity> entries) {
        for (PurchaseContractEntryEntity entry : entries) {
            requireOne(contractEntryMapper.insert(entry), "采购合同分录");
        }
    }

    private Map<String, Object> effectivePayload(
            PurchaseContractEntity contract,
            List<PurchaseContractEntryEntity> entries
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("purchaseContractId", contract.getFid());
        payload.put("purchaseContractNo", contract.getFnumber());
        payload.put("sourcingAwardId", contract.getFsourcingAwardId());
        payload.put("businessPartnerId", contract.getFbusinessPartnerId());
        payload.put("businessPartnerCode", contract.getFbusinessPartnerCode());
        payload.put("businessPartnerName", contract.getFbusinessPartnerName());
        payload.put("currencyCode", contract.getFcurrencyCode());
        payload.put("totalQuantity", contract.getFtotalQuantity());
        payload.put("netAmount", contract.getFnetAmount());
        payload.put("taxAmount", contract.getFtaxAmount());
        payload.put("grossAmount", contract.getFgrossAmount());
        payload.put("startDate", contract.getFstartDate());
        payload.put("endDate", contract.getFendDate());
        payload.put("paymentTermCode", contract.getFpaymentTermCode());
        payload.put("deliveryTermCode", contract.getFdeliveryTermCode());

        List<Map<String, Object>> lines = new ArrayList<>();
        for (PurchaseContractEntryEntity entry : entries) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("contractEntryId", entry.getFid());
            line.put("sourcingAwardEntryId", entry.getFsourcingAwardEntryId());
            line.put("purchaseRequestId", entry.getFpurchaseRequestId());
            line.put("purchaseRequestEntryId", entry.getFpurchaseRequestEntryId());
            line.put("materialId", entry.getFmaterialId());
            line.put("materialCode", entry.getFmaterialCode());
            line.put("quantity", entry.getFquantity());
            line.put("unitPrice", entry.getFunitPrice());
            line.put("taxRate", entry.getFtaxRate());
            line.put("grossAmount", entry.getFgrossAmount());
            line.put("plannedDeliveryDate", entry.getFplannedDeliveryDate());
            lines.add(line);
        }
        payload.put("entries", lines);
        return payload;
    }

    private void ensureEditable(PurchaseContractEntity contract) {
        if (!STATUS_DRAFT.equals(contract.getFstatus())
                || !(APPROVAL_DRAFT.equals(contract.getFapprovalStatus())
                || APPROVAL_REJECTED.equals(contract.getFapprovalStatus()))) {
            throw new BizException("仅草稿或已驳回采购合同允许修改/删除");
        }
    }

    private void ensureNumberUnique(String tenantId, String number) {
        Long count = contractMapper.selectCount(
                new LambdaQueryWrapper<PurchaseContractEntity>()
                        .eq(PurchaseContractEntity::getFtenantId, tenantId)
                        .eq(PurchaseContractEntity::getFnumber, number)
        );
        if (count != null && count > 0) {
            throw new BizException("采购合同号已存在: " + number);
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BizException("采购合同结束日期不能早于开始日期");
        }
    }

    private String buildNumber(LocalDate date, Long id) {
        String suffix = String.valueOf(id);
        suffix = suffix.substring(Math.max(0, suffix.length() - 8));
        return "PC" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + suffix;
    }

    private String requireTenant(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            throw new BizException("tenantId 不能为空");
        }
        return tenantId.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return nz(value).setScale(2, RoundingMode.HALF_UP);
    }

    private void initAudit(PurchaseContractEntity contract, Long operatorId) {
        LocalDateTime now = LocalDateTime.now();
        contract.setFcreateBy(operatorId);
        contract.setFcreateTime(now);
        contract.setFmodifyBy(operatorId);
        contract.setFmodifyTime(now);
        contract.setFdeleteFlag(0);
        contract.setFversion(0);
    }

    private void touch(PurchaseContractEntity contract, Long operatorId) {
        contract.setFmodifyBy(operatorId);
        contract.setFmodifyTime(LocalDateTime.now());
    }

    private void requireOne(int affected, String objectName) {
        if (affected != 1) {
            throw new BizException(objectName + "已被其他请求修改，请刷新后重试");
        }
    }

    private record CalculatedEntries(
            List<PurchaseContractEntryEntity> entries,
            Long businessPartnerId,
            String businessPartnerCode,
            String businessPartnerName,
            BigDecimal totalQuantity,
            BigDecimal netAmount,
            BigDecimal taxAmount,
            BigDecimal grossAmount
    ) {}
}
