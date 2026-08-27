package single.cjj.erp.procurement.reverse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.procurement.inbound.entity.PurchaseInboundEntity;
import single.cjj.erp.procurement.inbound.entity.PurchaseInboundEntryEntity;
import single.cjj.erp.procurement.inbound.mapper.PurchaseInboundEntryMapper;
import single.cjj.erp.procurement.inbound.mapper.PurchaseInboundMapper;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderEntryMapper;
import single.cjj.erp.procurement.reverse.dto.PurchaseReverseContracts.*;
import single.cjj.erp.procurement.reverse.entity.PurchaseReturnEntity;
import single.cjj.erp.procurement.reverse.entity.PurchaseReturnEntryEntity;
import single.cjj.erp.procurement.reverse.mapper.PurchaseReturnEntryMapper;
import single.cjj.erp.procurement.reverse.mapper.PurchaseReturnMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PurchaseReturnService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String APPROVAL_DRAFT = "DRAFT";
    private static final String APPROVAL_SUBMITTED = "SUBMITTED";
    private static final String APPROVAL_AUDITED = "AUDITED";
    private static final String APPROVAL_REJECTED = "REJECTED";

    private final PurchaseReturnMapper returnMapper;
    private final PurchaseReturnEntryMapper returnEntryMapper;
    private final PurchaseInboundMapper inboundMapper;
    private final PurchaseInboundEntryMapper inboundEntryMapper;
    private final PurchaseOrderEntryMapper orderEntryMapper;
    private final BusinessEventOutboxService outboxService;

    public PurchaseReturnService(
            PurchaseReturnMapper returnMapper,
            PurchaseReturnEntryMapper returnEntryMapper,
            PurchaseInboundMapper inboundMapper,
            PurchaseInboundEntryMapper inboundEntryMapper,
            PurchaseOrderEntryMapper orderEntryMapper,
            BusinessEventOutboxService outboxService
    ) {
        this.returnMapper = returnMapper;
        this.returnEntryMapper = returnEntryMapper;
        this.inboundMapper = inboundMapper;
        this.inboundEntryMapper = inboundEntryMapper;
        this.orderEntryMapper = orderEntryMapper;
        this.outboxService = outboxService;
    }

    public ReturnDetail detail(Long fid, String tenantId) {
        PurchaseReturnEntity header = requireReturn(fid, tenantId, false);
        return new ReturnDetail(header, listEntries(fid));
    }

    public IPage<PurchaseReturnEntity> page(
            String tenantId, Long orgId, Long purchaseOrderId,
            Long purchaseInboundId, String status, int page, int size
    ) {
        return returnMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.max(size, 1)),
                new LambdaQueryWrapper<PurchaseReturnEntity>()
                        .eq(PurchaseReturnEntity::getFtenantId, requireTenant(tenantId))
                        .eq(orgId != null, PurchaseReturnEntity::getForgId, orgId)
                        .eq(purchaseOrderId != null, PurchaseReturnEntity::getFpurchaseOrderId, purchaseOrderId)
                        .eq(purchaseInboundId != null, PurchaseReturnEntity::getFpurchaseInboundId, purchaseInboundId)
                        .eq(StringUtils.hasText(status), PurchaseReturnEntity::getFstatus, status)
                        .orderByDesc(PurchaseReturnEntity::getFdate)
                        .orderByDesc(PurchaseReturnEntity::getFid)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public ReturnDetail create(ReturnCreateRequest request, Long operatorId) {
        String tenant = requireTenant(request.ftenantId());
        PurchaseInboundEntity inbound = inboundMapper.selectByIdForUpdate(
                request.fpurchaseInboundId(), tenant);
        validateInbound(inbound);

        LocalDate date = request.fdate() == null ? LocalDate.now() : request.fdate();
        Long returnId = IdWorker.getId();
        String number = StringUtils.hasText(request.fnumber())
                ? request.fnumber().trim()
                : number("PRT", date, returnId);
        ensureNumberUnique(tenant, number);

        String reasonType = normalizeReasonType(request.freasonType());
        List<ReturnEntryRequest> requests = new ArrayList<>(request.entries());
        requests.sort(Comparator.comparing(ReturnEntryRequest::fpurchaseInboundEntryId));

        List<PurchaseReturnEntryEntity> entries = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        Long purchaseOrderId = null;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < requests.size(); i++) {
            ReturnEntryRequest item = requests.get(i);
            if (!seen.add(item.fpurchaseInboundEntryId())) {
                throw new BizException("同一退货单不能重复引用同一入库分录");
            }
            PurchaseInboundEntryEntity source =
                    inboundEntryMapper.selectByIdForUpdate(item.fpurchaseInboundEntryId(), tenant);
            if (source == null || !inbound.getFid().equals(source.getFpurchaseInboundId())) {
                throw new BizException("采购入库分录不存在或归属不匹配: " + item.fpurchaseInboundEntryId());
            }
            if (purchaseOrderId == null) {
                purchaseOrderId = source.getFpurchaseOrderId();
            } else if (!purchaseOrderId.equals(source.getFpurchaseOrderId())) {
                throw new BizException("v1 一张采购退货单只能对应一个采购订单");
            }
            BigDecimal available = nz(source.getFquantity()).subtract(nz(source.getFreturnedQuantity()));
            if (item.fquantity().compareTo(available) > 0) {
                throw new BizException("退货数量超过入库剩余可退数量: inboundEntry="
                        + source.getFid() + ", available=" + plain(available));
            }

            BigDecimal amount = money(item.fquantity().multiply(nz(source.getFunitPrice())));
            PurchaseReturnEntryEntity entry = new PurchaseReturnEntryEntity();
            entry.setFid(IdWorker.getId());
            entry.setFtenantId(tenant);
            entry.setForgId(inbound.getForgId());
            entry.setFpurchaseReturnId(returnId);
            entry.setFlineNo(i + 1);
            entry.setFpurchaseInboundId(inbound.getFid());
            entry.setFpurchaseInboundEntryId(source.getFid());
            entry.setFpurchaseOrderId(source.getFpurchaseOrderId());
            entry.setFpurchaseOrderEntryId(source.getFpurchaseOrderEntryId());
            entry.setFmaterialId(source.getFmaterialId());
            entry.setFmaterialCode(source.getFmaterialCode());
            entry.setFmaterialName(source.getFmaterialName());
            entry.setFspecification(source.getFspecification());
            entry.setFunitId(source.getFunitId());
            entry.setFquantity(item.fquantity());
            entry.setFunitPrice(source.getFunitPrice());
            entry.setFamount(amount);
            entry.setFbatchNo(source.getFbatchNo());
            entry.setFwarehouseId(source.getFwarehouseId());
            entry.setFprojectId(source.getFprojectId());
            entry.setFcostCenterId(source.getFcostCenterId());
            initEntry(entry, operatorId, now);
            entries.add(entry);
            totalQuantity = totalQuantity.add(item.fquantity());
            totalAmount = totalAmount.add(amount);
        }

        PurchaseReturnEntity header = new PurchaseReturnEntity();
        header.setFid(returnId);
        header.setFtenantId(tenant);
        header.setForgId(inbound.getForgId());
        header.setFnumber(number);
        header.setFdate(date);
        header.setFpurchaseInboundId(inbound.getFid());
        header.setFpurchaseOrderId(purchaseOrderId);
        header.setFbusinessPartnerId(inbound.getFbusinessPartnerId());
        header.setFbusinessPartnerCode(inbound.getFbusinessPartnerCode());
        header.setFbusinessPartnerName(inbound.getFbusinessPartnerName());
        header.setFcurrencyCode(inbound.getFcurrencyCode());
        header.setFwarehouseId(inbound.getFwarehouseId());
        header.setFtotalQuantity(totalQuantity);
        header.setFtotalAmount(money(totalAmount));
        header.setFreasonType(reasonType);
        header.setFreason(trim(request.freason()));
        header.setFstatus(STATUS_DRAFT);
        header.setFapprovalStatus(APPROVAL_DRAFT);
        initHeader(header, operatorId);

        requireOne(returnMapper.insert(header), "采购退货单");
        for (PurchaseReturnEntryEntity entry : entries) {
            requireOne(returnEntryMapper.insert(entry), "采购退货分录");
        }
        return new ReturnDetail(header, List.copyOf(entries));
    }

    @Transactional(rollbackFor = Exception.class)
    public ReturnDetail submit(Long fid, String tenantId, Long operatorId) {
        PurchaseReturnEntity header = requireReturn(fid, tenantId, true);
        if (!STATUS_DRAFT.equals(header.getFstatus())
                || !(APPROVAL_DRAFT.equals(header.getFapprovalStatus())
                || APPROVAL_REJECTED.equals(header.getFapprovalStatus()))) {
            throw new BizException("仅草稿或已驳回采购退货单允许提交");
        }
        if (listEntries(fid).isEmpty()) {
            throw new BizException("采购退货单至少需要一条分录");
        }
        header.setFapprovalStatus(APPROVAL_SUBMITTED);
        touch(header, operatorId);
        requireOne(returnMapper.updateById(header), "采购退货单");
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReturnDetail confirm(Long fid, String tenantId, Long operatorId) {
        PurchaseReturnEntity header = requireReturn(fid, tenantId, true);
        if (!STATUS_DRAFT.equals(header.getFstatus())
                || !APPROVAL_SUBMITTED.equals(header.getFapprovalStatus())) {
            throw new BizException("仅已提交采购退货单允许确认");
        }

        List<PurchaseReturnEntryEntity> entries = listEntries(fid);
        if (entries.isEmpty()) {
            throw new BizException("采购退货单至少需要一条分录");
        }

        for (PurchaseReturnEntryEntity returnEntry : entries.stream()
                .sorted(Comparator.comparing(PurchaseReturnEntryEntity::getFpurchaseInboundEntryId))
                .toList()) {
            PurchaseInboundEntryEntity inboundEntry =
                    inboundEntryMapper.selectByIdForUpdate(returnEntry.getFpurchaseInboundEntryId(), header.getFtenantId());
            if (inboundEntry == null) {
                throw new BizException("采购入库分录不存在: " + returnEntry.getFpurchaseInboundEntryId());
            }
            BigDecimal nextInboundReturned = nz(inboundEntry.getFreturnedQuantity()).add(returnEntry.getFquantity());
            if (nextInboundReturned.compareTo(nz(inboundEntry.getFquantity())) > 0) {
                throw new BizException("累计退货数量超过采购入库数量: inboundEntry=" + inboundEntry.getFid());
            }
            inboundEntry.setFreturnedQuantity(nextInboundReturned);
            inboundEntry.setFmodifyBy(operatorId);
            inboundEntry.setFmodifyTime(LocalDateTime.now());
            requireOne(inboundEntryMapper.updateById(inboundEntry), "采购入库退货数量");

            PurchaseOrderEntryEntity orderEntry =
                    orderEntryMapper.selectByIdForUpdate(returnEntry.getFpurchaseOrderEntryId(), header.getFtenantId());
            if (orderEntry == null) {
                throw new BizException("采购订单分录不存在: " + returnEntry.getFpurchaseOrderEntryId());
            }
            BigDecimal nextOrderReturned = nz(orderEntry.getFreturnedQuantity()).add(returnEntry.getFquantity());
            if (nextOrderReturned.compareTo(nz(orderEntry.getFinboundQuantity())) > 0) {
                throw new BizException("累计退货数量超过采购订单累计入库数量: orderEntry=" + orderEntry.getFid());
            }
            orderEntry.setFreturnedQuantity(nextOrderReturned);
            orderEntry.setFmodifyBy(operatorId);
            orderEntry.setFmodifyTime(LocalDateTime.now());
            requireOne(orderEntryMapper.updateById(orderEntry), "采购订单退货数量");
        }

        header.setFstatus(STATUS_CONFIRMED);
        header.setFapprovalStatus(APPROVAL_AUDITED);
        touch(header, operatorId);
        requireOne(returnMapper.updateById(header), "采购退货单");

        outboxService.append(
                header.getFtenantId(), header.getForgId(),
                "PURCHASE_RETURN_CONFIRMED", "PURCHASE_RETURN", header.getFid(),
                version(header), "ERP_PURCHASE_RETURN", header.getFnumber(),
                header.getFdate(), operatorId, eventPayload(header, entries)
        );
        return new ReturnDetail(header, entries);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReturnDetail reject(Long fid, String tenantId, Long operatorId) {
        PurchaseReturnEntity header = requireReturn(fid, tenantId, true);
        if (!STATUS_DRAFT.equals(header.getFstatus())
                || !APPROVAL_SUBMITTED.equals(header.getFapprovalStatus())) {
            throw new BizException("仅已提交采购退货单允许驳回");
        }
        header.setFapprovalStatus(APPROVAL_REJECTED);
        touch(header, operatorId);
        requireOne(returnMapper.updateById(header), "采购退货单");
        return detail(fid, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReturnDetail cancel(Long fid, String tenantId, Long operatorId) {
        PurchaseReturnEntity header = requireReturn(fid, tenantId, true);
        if (STATUS_CONFIRMED.equals(header.getFstatus())) {
            throw new BizException("已确认采购退货不能直接取消，后续需走退货冲销");
        }
        if (STATUS_CANCELLED.equals(header.getFstatus())) {
            return detail(fid, tenantId);
        }
        header.setFstatus(STATUS_CANCELLED);
        touch(header, operatorId);
        requireOne(returnMapper.updateById(header), "采购退货单");
        return detail(fid, tenantId);
    }

    private PurchaseReturnEntity requireReturn(Long fid, String tenantId, boolean lock) {
        String tenant = requireTenant(tenantId);
        PurchaseReturnEntity value = lock
                ? returnMapper.selectByIdForUpdate(fid, tenant)
                : returnMapper.selectOne(new LambdaQueryWrapper<PurchaseReturnEntity>()
                .eq(PurchaseReturnEntity::getFid, fid)
                .eq(PurchaseReturnEntity::getFtenantId, tenant)
                .last("limit 1"));
        if (value == null) throw new BizException("采购退货单不存在: " + fid);
        return value;
    }

    private void validateInbound(PurchaseInboundEntity inbound) {
        if (inbound == null) throw new BizException("采购入库单不存在");
        if (!STATUS_CONFIRMED.equals(inbound.getFstatus())
                || !APPROVAL_AUDITED.equals(inbound.getFapprovalStatus())) {
            throw new BizException("只有已审核确认采购入库单允许退货");
        }
    }

    private List<PurchaseReturnEntryEntity> listEntries(Long headerId) {
        return returnEntryMapper.selectList(new LambdaQueryWrapper<PurchaseReturnEntryEntity>()
                .eq(PurchaseReturnEntryEntity::getFpurchaseReturnId, headerId)
                .orderByAsc(PurchaseReturnEntryEntity::getFlineNo));
    }

    private Map<String,Object> eventPayload(PurchaseReturnEntity header,List<PurchaseReturnEntryEntity> entries){
        Map<String,Object> p=new LinkedHashMap<>();
        p.put("purchaseReturnId",header.getFid()); p.put("purchaseReturnNo",header.getFnumber());
        p.put("purchaseInboundId",header.getFpurchaseInboundId()); p.put("purchaseOrderId",header.getFpurchaseOrderId());
        p.put("businessPartnerId",header.getFbusinessPartnerId()); p.put("businessPartnerCode",header.getFbusinessPartnerCode());
        p.put("businessPartnerName",header.getFbusinessPartnerName()); p.put("currencyCode",header.getFcurrencyCode());
        p.put("totalQuantity",header.getFtotalQuantity()); p.put("totalAmount",header.getFtotalAmount());
        p.put("reasonType",header.getFreasonType());
        p.put("entries",entries.stream().map(e->{
            Map<String,Object> x=new LinkedHashMap<>();
            x.put("purchaseReturnEntryId",e.getFid()); x.put("purchaseInboundEntryId",e.getFpurchaseInboundEntryId());
            x.put("purchaseOrderEntryId",e.getFpurchaseOrderEntryId()); x.put("materialId",e.getFmaterialId());
            x.put("materialCode",e.getFmaterialCode()); x.put("quantity",e.getFquantity()); x.put("unitPrice",e.getFunitPrice());
            x.put("amount",e.getFamount()); x.put("warehouseId",e.getFwarehouseId()); x.put("projectId",e.getFprojectId());
            x.put("costCenterId",e.getFcostCenterId()); return x;
        }).toList());
        return p;
    }

    private String normalizeReasonType(String value) {
        String v=value.trim().toUpperCase(Locale.ROOT);
        if(!List.of("QUALITY","DAMAGE","WRONG_GOODS","OVER_DELIVERY","OTHER").contains(v))
            throw new BizException("采购退货原因类型不支持: "+value);
        return v;
    }
    private void ensureNumberUnique(String tenant,String number){
        Long c=returnMapper.selectCount(new LambdaQueryWrapper<PurchaseReturnEntity>()
                .eq(PurchaseReturnEntity::getFtenantId,tenant).eq(PurchaseReturnEntity::getFnumber,number));
        if(c!=null&&c>0) throw new BizException("采购退货单号已存在: "+number);
    }
    private String number(String prefix,LocalDate date,Long id){
        String s=String.valueOf(id); s=s.substring(Math.max(0,s.length()-8));
        return prefix+date.format(DateTimeFormatter.BASIC_ISO_DATE)+"-"+s;
    }
    private String requireTenant(String tenant){if(!StringUtils.hasText(tenant))throw new BizException("tenantId 不能为空");return tenant.trim();}
    private String trim(String v){return StringUtils.hasText(v)?v.trim():null;}
    private BigDecimal nz(BigDecimal v){return v==null?BigDecimal.ZERO:v;}
    private BigDecimal money(BigDecimal v){return nz(v).setScale(2,RoundingMode.HALF_UP);}
    private String plain(BigDecimal v){return nz(v).stripTrailingZeros().toPlainString();}
    private long version(PurchaseReturnEntity v){return v.getFversion()==null?0L:v.getFversion().longValue();}
    private void initHeader(PurchaseReturnEntity v,Long op){LocalDateTime n=LocalDateTime.now();v.setFcreateBy(op);v.setFcreateTime(n);v.setFmodifyBy(op);v.setFmodifyTime(n);v.setFdeleteFlag(0);v.setFversion(0);}
    private void initEntry(PurchaseReturnEntryEntity v,Long op,LocalDateTime n){v.setFcreateBy(op);v.setFcreateTime(n);v.setFmodifyBy(op);v.setFmodifyTime(n);v.setFdeleteFlag(0);v.setFversion(0);}
    private void touch(PurchaseReturnEntity v,Long op){v.setFmodifyBy(op);v.setFmodifyTime(LocalDateTime.now());}
    private void requireOne(int n,String what){if(n!=1)throw new BizException(what+"已被其他请求修改，请刷新后重试");}
}
