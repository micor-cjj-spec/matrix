package single.cjj.erp.procurement.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderCreateRequest;
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderDetail;
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderEntryRequest;
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderUpdateRequest;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntity;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderEntryMapper;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseOrderService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_EFFECTIVE = "EFFECTIVE";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String APPROVAL_DRAFT = "DRAFT";
    private static final String APPROVAL_SUBMITTED = "SUBMITTED";
    private static final String APPROVAL_AUDITED = "AUDITED";
    private static final String APPROVAL_REJECTED = "REJECTED";
    private static final String NONE = "NONE";
    private static final String CLOSE_OPEN = "OPEN";
    private static final String CLOSE_CLOSED = "CLOSED";

    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderEntryMapper entryMapper;
    private final PurchaseOrderContractConversionService contractConversionService;

    public PurchaseOrderService(
            PurchaseOrderMapper orderMapper,
            PurchaseOrderEntryMapper entryMapper,
            PurchaseOrderContractConversionService contractConversionService
    ) {
        this.orderMapper = orderMapper;
        this.entryMapper = entryMapper;
        this.contractConversionService = contractConversionService;
    }

    public PurchaseOrderDetail detail(Long fid, String tenantId) {
        PurchaseOrderEntity order = requireOrder(fid, tenantId);
        return new PurchaseOrderDetail(order, listEntries(fid));
    }

    public IPage<PurchaseOrderEntity> page(String tenantId, Long orgId, int page, int size, String number,
                                           Long businessPartnerId, String approvalStatus, String status) {
        if (!StringUtils.hasText(tenantId)) {
            throw new BizException("tenantId 不能为空");
        }
        LambdaQueryWrapper<PurchaseOrderEntity> wrapper = new LambdaQueryWrapper<PurchaseOrderEntity>()
                .eq(PurchaseOrderEntity::getFtenantId, tenantId.trim())
                .eq(orgId != null, PurchaseOrderEntity::getForgId, orgId)
                .like(StringUtils.hasText(number), PurchaseOrderEntity::getFnumber, number)
                .eq(businessPartnerId != null, PurchaseOrderEntity::getFbusinessPartnerId, businessPartnerId)
                .eq(StringUtils.hasText(approvalStatus), PurchaseOrderEntity::getFapprovalStatus, approvalStatus)
                .eq(StringUtils.hasText(status), PurchaseOrderEntity::getFstatus, status)
                .orderByDesc(PurchaseOrderEntity::getFdate)
                .orderByDesc(PurchaseOrderEntity::getFcreateTime);
        return orderMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)), wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDetail create(PurchaseOrderCreateRequest request, Long operatorId) {
        String tenantId = requireTenant(request.ftenantId());
        if (request.fcontractId() != null) {
            throw new BizException("合同来源采购订单必须使用 from-contract 专用接口创建");
        }
        LocalDate orderDate = request.fdate() == null ? LocalDate.now() : request.fdate();
        Long orderId = IdWorker.getId();
        String number = StringUtils.hasText(request.fnumber()) ? request.fnumber().trim() : buildNumber(orderDate, orderId);
        ensureNumberUnique(tenantId, number);
        CalculatedEntries calculated = calculateEntries(orderId, tenantId, request.forgId(),
                request.fplannedDeliveryDate(), request.entries(), operatorId);

        PurchaseOrderEntity order = new PurchaseOrderEntity();
        order.setFid(orderId);
        order.setFtenantId(tenantId);
        order.setForgId(request.forgId());
        order.setFnumber(number);
        order.setFdate(orderDate);
        order.setFbusinessPartnerId(request.fbusinessPartnerId());
        order.setFbusinessPartnerCode(request.fbusinessPartnerCode().trim());
        order.setFbusinessPartnerName(request.fbusinessPartnerName().trim());
        order.setFcontractId(request.fcontractId());
        order.setFcurrencyId(request.fcurrencyId());
        order.setFcurrencyCode(request.fcurrencyCode().trim());
        order.setFpaymentTermCode(trimToNull(request.fpaymentTermCode()));
        order.setFplannedDeliveryDate(request.fplannedDeliveryDate());
        applyTotals(order, calculated);
        order.setFstatus(STATUS_DRAFT);
        order.setFapprovalStatus(APPROVAL_DRAFT);
        order.setFreceiptStatus(NONE);
        order.setFinvoiceStatus(NONE);
        order.setFsettlementStatus(NONE);
        order.setFcloseStatus(CLOSE_OPEN);
        order.setFcreateBy(operatorId);
        order.setFcreateTime(LocalDateTime.now());
        order.setFmodifyBy(operatorId);
        order.setFmodifyTime(order.getFcreateTime());
        order.setFdeleteFlag(0);
        order.setFversion(0);
        orderMapper.insert(order);
        insertEntries(calculated.entries());
        return detail(orderId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDetail update(Long fid, PurchaseOrderUpdateRequest request, Long operatorId) {
        String tenantId = requireTenant(request.ftenantId());
        PurchaseOrderEntity order = requireOrder(fid, tenantId);
        ensureEditable(order);
        if (request.fcontractId() != null || contractConversionService.isContractSourced(listEntries(fid))) {
            throw new BizException("合同来源采购订单不允许通过通用编辑接口修改，请删除草稿后重新从合同生成");
        }
        CalculatedEntries calculated = calculateEntries(fid, tenantId, request.forgId(),
                request.fplannedDeliveryDate(), request.entries(), operatorId);
        order.setForgId(request.forgId());
        order.setFbusinessPartnerId(request.fbusinessPartnerId());
        order.setFbusinessPartnerCode(request.fbusinessPartnerCode().trim());
        order.setFbusinessPartnerName(request.fbusinessPartnerName().trim());
        order.setFcontractId(request.fcontractId());
        order.setFcurrencyId(request.fcurrencyId());
        order.setFcurrencyCode(request.fcurrencyCode().trim());
        order.setFpaymentTermCode(trimToNull(request.fpaymentTermCode()));
        order.setFplannedDeliveryDate(request.fplannedDeliveryDate());
        applyTotals(order, calculated);
        touch(order, operatorId);
        requireUpdated(orderMapper.updateById(order));
        entryMapper.delete(new LambdaQueryWrapper<PurchaseOrderEntryEntity>()
                .eq(PurchaseOrderEntryEntity::getFpurchaseOrderId, fid));
        insertEntries(calculated.entries());
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDetail submit(Long fid, String tenantId, Long operatorId) {
        PurchaseOrderEntity order = requireOrder(fid, tenantId);
        if (!STATUS_DRAFT.equals(order.getFstatus())
                || !(APPROVAL_DRAFT.equals(order.getFapprovalStatus()) || APPROVAL_REJECTED.equals(order.getFapprovalStatus()))) {
            throw new BizException("仅草稿或已驳回采购订单允许提交");
        }
        if (listEntries(fid).isEmpty()) {
            throw new BizException("采购订单至少需要一条分录");
        }
        order.setFapprovalStatus(APPROVAL_SUBMITTED);
        touch(order, operatorId);
        requireUpdated(orderMapper.updateById(order));
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDetail audit(Long fid, String tenantId, Long operatorId) {
        PurchaseOrderEntity order = requireOrder(fid, tenantId);
        if (!STATUS_DRAFT.equals(order.getFstatus()) || !APPROVAL_SUBMITTED.equals(order.getFapprovalStatus())) {
            throw new BizException("仅已提交采购订单允许审核");
        }
        order.setFapprovalStatus(APPROVAL_AUDITED);
        order.setFstatus(STATUS_EFFECTIVE);
        touch(order, operatorId);
        requireUpdated(orderMapper.updateById(order));
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDetail reject(Long fid, String tenantId, Long operatorId) {
        PurchaseOrderEntity order = requireOrder(fid, tenantId);
        if (!STATUS_DRAFT.equals(order.getFstatus()) || !APPROVAL_SUBMITTED.equals(order.getFapprovalStatus())) {
            throw new BizException("仅已提交采购订单允许驳回");
        }
        order.setFapprovalStatus(APPROVAL_REJECTED);
        touch(order, operatorId);
        requireUpdated(orderMapper.updateById(order));
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDetail cancel(Long fid, String tenantId, Long operatorId) {
        PurchaseOrderEntity order = requireOrder(fid, tenantId);
        if (!STATUS_EFFECTIVE.equals(order.getFstatus()) || !APPROVAL_AUDITED.equals(order.getFapprovalStatus())) {
            throw new BizException("仅已生效采购订单允许取消");
        }
        if (!NONE.equals(order.getFreceiptStatus())) {
            throw new BizException("已发生收货的采购订单不能直接取消，请走后续退货/关闭流程");
        }
        List<PurchaseOrderEntryEntity> sourceEntries = listEntries(fid);
        if (contractConversionService.isContractSourced(sourceEntries)) {
            contractConversionService.releaseOrderAllocation(order, sourceEntries, operatorId);
        }
        order.setFstatus(STATUS_CANCELLED);
        order.setFcloseStatus(CLOSE_CLOSED);
        touch(order, operatorId);
        requireUpdated(orderMapper.updateById(order));
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long fid, String tenantId) {
        PurchaseOrderEntity order = requireOrder(fid, tenantId);
        ensureEditable(order);
        List<PurchaseOrderEntryEntity> sourceEntries = listEntries(fid);
        if (contractConversionService.isContractSourced(sourceEntries)) {
            contractConversionService.releaseOrderAllocation(order, sourceEntries, order.getFmodifyBy());
        }
        entryMapper.delete(new LambdaQueryWrapper<PurchaseOrderEntryEntity>()
                .eq(PurchaseOrderEntryEntity::getFpurchaseOrderId, fid));
        return orderMapper.deleteById(fid) > 0;
    }

    private PurchaseOrderEntity requireOrder(Long fid, String tenantId) {
        PurchaseOrderEntity order = orderMapper.selectOne(new LambdaQueryWrapper<PurchaseOrderEntity>()
                .eq(PurchaseOrderEntity::getFid, fid)
                .eq(PurchaseOrderEntity::getFtenantId, requireTenant(tenantId))
                .last("limit 1"));
        if (order == null) {
            throw new BizException("采购订单不存在: " + fid);
        }
        return order;
    }

    private List<PurchaseOrderEntryEntity> listEntries(Long orderId) {
        return entryMapper.selectList(new LambdaQueryWrapper<PurchaseOrderEntryEntity>()
                .eq(PurchaseOrderEntryEntity::getFpurchaseOrderId, orderId)
                .orderByAsc(PurchaseOrderEntryEntity::getFlineNo));
    }

    private void ensureEditable(PurchaseOrderEntity order) {
        if (!STATUS_DRAFT.equals(order.getFstatus())
                || !(APPROVAL_DRAFT.equals(order.getFapprovalStatus()) || APPROVAL_REJECTED.equals(order.getFapprovalStatus()))) {
            throw new BizException("仅草稿或已驳回采购订单允许修改/删除");
        }
    }

    private void ensureNumberUnique(String tenantId, String number) {
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrderEntity>()
                .eq(PurchaseOrderEntity::getFtenantId, tenantId)
                .eq(PurchaseOrderEntity::getFnumber, number));
        if (count != null && count > 0) {
            throw new BizException("采购订单号已存在: " + number);
        }
    }

    private CalculatedEntries calculateEntries(Long orderId, String tenantId, Long orgId,
                                                LocalDate headerPlannedDeliveryDate,
                                                List<PurchaseOrderEntryRequest> requests, Long operatorId) {
        if (requests == null || requests.isEmpty()) {
            throw new BizException("采购订单至少需要一条分录");
        }
        List<PurchaseOrderEntryEntity> entries = new ArrayList<>(requests.size());
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal netAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal grossAmount = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < requests.size(); i++) {
            PurchaseOrderEntryRequest request = requests.get(i);
            BigDecimal quantity = request.fquantity();
            BigDecimal unitPrice = request.funitPrice();
            BigDecimal taxRate = request.ftaxRate() == null ? BigDecimal.ZERO : request.ftaxRate();
            BigDecimal lineNet = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTax = lineNet.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineGross = lineNet.add(lineTax);

            PurchaseOrderEntryEntity entry = new PurchaseOrderEntryEntity();
            entry.setFid(IdWorker.getId());
            entry.setFtenantId(tenantId);
            entry.setForgId(orgId);
            entry.setFpurchaseOrderId(orderId);
            entry.setFlineNo(i + 1);
            entry.setFmaterialId(request.fmaterialId());
            entry.setFmaterialCode(request.fmaterialCode().trim());
            entry.setFmaterialName(request.fmaterialName().trim());
            entry.setFspecification(trimToNull(request.fspecification()));
            entry.setFunitId(request.funitId());
            entry.setFquantity(quantity);
            entry.setFunitPrice(unitPrice);
            entry.setFnetAmount(lineNet);
            entry.setFtaxRate(taxRate);
            entry.setFtaxAmount(lineTax);
            entry.setFgrossAmount(lineGross);
            entry.setFplannedDeliveryDate(request.fplannedDeliveryDate() == null ? headerPlannedDeliveryDate : request.fplannedDeliveryDate());
            entry.setFprojectId(request.fprojectId());
            entry.setFcostCenterId(request.fcostCenterId());
            entry.setFreceiptReservedQuantity(BigDecimal.ZERO);
            entry.setFreceivedQuantity(BigDecimal.ZERO);
            entry.setFacceptedQuantity(BigDecimal.ZERO);
            entry.setFinboundQuantity(BigDecimal.ZERO);
            entry.setFinvoicedQuantity(BigDecimal.ZERO);
            entry.setFsettledAmount(BigDecimal.ZERO);
            entry.setFcreateBy(operatorId);
            entry.setFcreateTime(now);
            entry.setFmodifyBy(operatorId);
            entry.setFmodifyTime(now);
            entry.setFdeleteFlag(0);
            entry.setFversion(0);
            entries.add(entry);
            totalQuantity = totalQuantity.add(quantity);
            netAmount = netAmount.add(lineNet);
            taxAmount = taxAmount.add(lineTax);
            grossAmount = grossAmount.add(lineGross);
        }
        return new CalculatedEntries(entries, totalQuantity, netAmount, taxAmount, grossAmount);
    }

    private void insertEntries(List<PurchaseOrderEntryEntity> entries) {
        for (PurchaseOrderEntryEntity entry : entries) {
            entryMapper.insert(entry);
        }
    }

    private void applyTotals(PurchaseOrderEntity order, CalculatedEntries calculated) {
        order.setFtotalQuantity(calculated.totalQuantity());
        order.setFnetAmount(calculated.netAmount());
        order.setFtaxAmount(calculated.taxAmount());
        order.setFgrossAmount(calculated.grossAmount());
    }

    private String buildNumber(LocalDate date, Long id) {
        String suffix = String.valueOf(id);
        suffix = suffix.substring(Math.max(0, suffix.length() - 8));
        return "PO" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + suffix;
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

    private void touch(PurchaseOrderEntity order, Long operatorId) {
        order.setFmodifyBy(operatorId);
        order.setFmodifyTime(LocalDateTime.now());
    }

    private void requireUpdated(int updated) {
        if (updated != 1) {
            throw new BizException("采购订单已被其他请求修改，请刷新后重试");
        }
    }

    private record CalculatedEntries(List<PurchaseOrderEntryEntity> entries, BigDecimal totalQuantity,
                                     BigDecimal netAmount, BigDecimal taxAmount, BigDecimal grossAmount) {
    }
}