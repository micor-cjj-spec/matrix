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
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntity;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderEntryMapper;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderMapper;
import single.cjj.erp.procurement.reverse.dto.PurchaseReverseContracts.*;
import single.cjj.erp.procurement.reverse.entity.PurchaseReturnEntity;
import single.cjj.erp.procurement.reverse.entity.PurchaseReturnEntryEntity;
import single.cjj.erp.procurement.reverse.entity.SupplierClaimEntity;
import single.cjj.erp.procurement.reverse.entity.SupplierClaimEntryEntity;
import single.cjj.erp.procurement.reverse.mapper.PurchaseReturnEntryMapper;
import single.cjj.erp.procurement.reverse.mapper.PurchaseReturnMapper;
import single.cjj.erp.procurement.reverse.mapper.SupplierClaimEntryMapper;
import single.cjj.erp.procurement.reverse.mapper.SupplierClaimMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SupplierClaimService {

    private static final String STATUS_DRAFT="DRAFT";
    private static final String STATUS_CONFIRMED="CONFIRMED";
    private static final String STATUS_CANCELLED="CANCELLED";
    private static final String APPROVAL_DRAFT="DRAFT";
    private static final String APPROVAL_SUBMITTED="SUBMITTED";
    private static final String APPROVAL_AUDITED="AUDITED";
    private static final String APPROVAL_REJECTED="REJECTED";

    private final SupplierClaimMapper claimMapper;
    private final SupplierClaimEntryMapper claimEntryMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderEntryMapper orderEntryMapper;
    private final PurchaseReturnMapper returnMapper;
    private final PurchaseReturnEntryMapper returnEntryMapper;
    private final BusinessEventOutboxService outboxService;

    public SupplierClaimService(
            SupplierClaimMapper claimMapper,
            SupplierClaimEntryMapper claimEntryMapper,
            PurchaseOrderMapper orderMapper,
            PurchaseOrderEntryMapper orderEntryMapper,
            PurchaseReturnMapper returnMapper,
            PurchaseReturnEntryMapper returnEntryMapper,
            BusinessEventOutboxService outboxService
    ) {
        this.claimMapper=claimMapper; this.claimEntryMapper=claimEntryMapper;
        this.orderMapper=orderMapper; this.orderEntryMapper=orderEntryMapper;
        this.returnMapper=returnMapper; this.returnEntryMapper=returnEntryMapper;
        this.outboxService=outboxService;
    }

    public ClaimDetail detail(Long fid,String tenantId){
        SupplierClaimEntity h=requireClaim(fid,tenantId,false);
        return new ClaimDetail(h,listEntries(fid));
    }

    public IPage<SupplierClaimEntity> page(String tenantId,Long orgId,Long orderId,Long returnId,String status,int page,int size){
        return claimMapper.selectPage(new Page<>(Math.max(page,1),Math.max(size,1)),
                new LambdaQueryWrapper<SupplierClaimEntity>()
                        .eq(SupplierClaimEntity::getFtenantId,requireTenant(tenantId))
                        .eq(orgId!=null,SupplierClaimEntity::getForgId,orgId)
                        .eq(orderId!=null,SupplierClaimEntity::getFpurchaseOrderId,orderId)
                        .eq(returnId!=null,SupplierClaimEntity::getFpurchaseReturnId,returnId)
                        .eq(StringUtils.hasText(status),SupplierClaimEntity::getFstatus,status)
                        .orderByDesc(SupplierClaimEntity::getFdate).orderByDesc(SupplierClaimEntity::getFid));
    }

    @Transactional(rollbackFor=Exception.class)
    public ClaimDetail create(ClaimCreateRequest request,Long operatorId){
        String tenant=requireTenant(request.ftenantId());
        PurchaseOrderEntity order=orderMapper.selectByIdForUpdate(request.fpurchaseOrderId(),tenant);
        if(order==null) throw new BizException("采购订单不存在: "+request.fpurchaseOrderId());

        PurchaseReturnEntity sourceReturn=null;
        if(request.fpurchaseReturnId()!=null){
            sourceReturn=returnMapper.selectByIdForUpdate(request.fpurchaseReturnId(),tenant);
            if(sourceReturn==null || !"CONFIRMED".equals(sourceReturn.getFstatus()))
                throw new BizException("来源采购退货不存在或未确认");
            if(!order.getFid().equals(sourceReturn.getFpurchaseOrderId()))
                throw new BizException("来源采购退货与索赔采购订单不一致");
        }

        LocalDate date=request.fdate()==null?LocalDate.now():request.fdate();
        Long claimId=IdWorker.getId();
        String number=StringUtils.hasText(request.fnumber())?request.fnumber().trim():number("CLM",date,claimId);
        ensureNumberUnique(tenant,number);
        String claimType=normalizeClaimType(request.fclaimType());

        List<ClaimEntryRequest> requested=new ArrayList<>(request.entries());
        requested.sort(Comparator.comparing(ClaimEntryRequest::fpurchaseOrderEntryId)
                .thenComparing(v->v.fpurchaseReturnEntryId()==null?Long.MIN_VALUE:v.fpurchaseReturnEntryId()));
        Set<String> seen=new HashSet<>();
        List<SupplierClaimEntryEntity> entries=new ArrayList<>();
        BigDecimal total=BigDecimal.ZERO;
        LocalDateTime now=LocalDateTime.now();

        for(int i=0;i<requested.size();i++){
            ClaimEntryRequest item=requested.get(i);
            String key=item.fpurchaseOrderEntryId()+":"+item.fpurchaseReturnEntryId();
            if(!seen.add(key)) throw new BizException("索赔分录来源重复: "+key);

            PurchaseOrderEntryEntity orderEntry=orderEntryMapper.selectByIdForUpdate(item.fpurchaseOrderEntryId(),tenant);
            if(orderEntry==null || !order.getFid().equals(orderEntry.getFpurchaseOrderId()))
                throw new BizException("采购订单分录不存在或归属不匹配: "+item.fpurchaseOrderEntryId());

            if(item.fpurchaseReturnEntryId()!=null){
                if(sourceReturn==null) throw new BizException("引用采购退货分录时必须指定来源退货单");
                PurchaseReturnEntryEntity re=returnEntryMapper.selectByIdForUpdate(item.fpurchaseReturnEntryId(),tenant);
                if(re==null || !sourceReturn.getFid().equals(re.getFpurchaseReturnId())
                        || !orderEntry.getFid().equals(re.getFpurchaseOrderEntryId()))
                    throw new BizException("采购退货分录与索赔来源不匹配: "+item.fpurchaseReturnEntryId());
            }

            SupplierClaimEntryEntity e=new SupplierClaimEntryEntity();
            e.setFid(IdWorker.getId()); e.setFtenantId(tenant); e.setForgId(order.getForgId());
            e.setFsupplierClaimId(claimId); e.setFlineNo(i+1); e.setFpurchaseOrderId(order.getFid());
            e.setFpurchaseOrderEntryId(orderEntry.getFid()); e.setFpurchaseReturnEntryId(item.fpurchaseReturnEntryId());
            e.setFmaterialId(orderEntry.getFmaterialId()); e.setFmaterialCode(orderEntry.getFmaterialCode());
            e.setFmaterialName(orderEntry.getFmaterialName()); e.setFrequestedAmount(money(item.frequestedAmount()));
            e.setFagreedAmount(BigDecimal.ZERO.setScale(2)); e.setFdeductedAmount(BigDecimal.ZERO.setScale(2));
            e.setFreason(trim(item.freason())); initEntry(e,operatorId,now);
            entries.add(e); total=total.add(e.getFrequestedAmount());
        }

        SupplierClaimEntity h=new SupplierClaimEntity();
        h.setFid(claimId); h.setFtenantId(tenant); h.setForgId(order.getForgId()); h.setFnumber(number); h.setFdate(date);
        h.setFpurchaseOrderId(order.getFid()); h.setFpurchaseReturnId(request.fpurchaseReturnId());
        h.setFbusinessPartnerId(order.getFbusinessPartnerId()); h.setFbusinessPartnerCode(order.getFbusinessPartnerCode());
        h.setFbusinessPartnerName(order.getFbusinessPartnerName()); h.setFcurrencyCode(order.getFcurrencyCode());
        h.setFclaimType(claimType); h.setFrequestedAmount(money(total)); h.setFagreedAmount(BigDecimal.ZERO.setScale(2));
        h.setFdeductedAmount(BigDecimal.ZERO.setScale(2)); h.setFdeductionStatus("NONE"); h.setFreason(trim(request.freason()));
        h.setFstatus(STATUS_DRAFT); h.setFapprovalStatus(APPROVAL_DRAFT); initHeader(h,operatorId);
        requireOne(claimMapper.insert(h),"供应商索赔单");
        for(SupplierClaimEntryEntity e:entries) requireOne(claimEntryMapper.insert(e),"供应商索赔分录");
        return new ClaimDetail(h,List.copyOf(entries));
    }

    @Transactional(rollbackFor=Exception.class)
    public ClaimDetail submit(Long fid,String tenantId,Long operatorId){
        SupplierClaimEntity h=requireClaim(fid,tenantId,true);
        if(!STATUS_DRAFT.equals(h.getFstatus())
                || !(APPROVAL_DRAFT.equals(h.getFapprovalStatus())||APPROVAL_REJECTED.equals(h.getFapprovalStatus())))
            throw new BizException("仅草稿或已驳回供应商索赔允许提交");
        h.setFapprovalStatus(APPROVAL_SUBMITTED); touch(h,operatorId);
        requireOne(claimMapper.updateById(h),"供应商索赔单");
        return detail(fid,tenantId);
    }

    @Transactional(rollbackFor=Exception.class)
    public ClaimDetail confirm(Long fid,ClaimConfirmRequest request,Long operatorId){
        String tenant=requireTenant(request.ftenantId());
        SupplierClaimEntity h=requireClaim(fid,tenant,true);
        if(!STATUS_DRAFT.equals(h.getFstatus())||!APPROVAL_SUBMITTED.equals(h.getFapprovalStatus()))
            throw new BizException("仅已提交供应商索赔允许确认");

        List<SupplierClaimEntryEntity> stored=listEntries(fid);
        if(stored.isEmpty()) throw new BizException("供应商索赔至少需要一条分录");
        Map<Long,BigDecimal> agreed=new LinkedHashMap<>();
        for(ClaimAgreeEntryRequest item:request.entries()){
            if(agreed.put(item.fsupplierClaimEntryId(),money(item.fagreedAmount()))!=null)
                throw new BizException("索赔确认分录重复: "+item.fsupplierClaimEntryId());
        }
        if(agreed.size()!=stored.size()) throw new BizException("索赔确认必须覆盖全部索赔分录");

        BigDecimal total=BigDecimal.ZERO;
        List<SupplierClaimEntryEntity> locked=new ArrayList<>();
        for(SupplierClaimEntryEntity source:stored.stream().sorted(Comparator.comparing(SupplierClaimEntryEntity::getFid)).toList()){
            SupplierClaimEntryEntity e=claimEntryMapper.selectByIdForUpdate(source.getFid(),tenant);
            BigDecimal amount=agreed.get(e.getFid());
            if(amount==null) throw new BizException("索赔确认缺少分录: "+e.getFid());
            if(amount.compareTo(money(e.getFrequestedAmount()))>0)
                throw new BizException("协商确认金额不能超过索赔申请金额: claimEntry="+e.getFid());
            e.setFagreedAmount(amount); e.setFmodifyBy(operatorId); e.setFmodifyTime(LocalDateTime.now());
            requireOne(claimEntryMapper.updateById(e),"供应商索赔分录");
            total=total.add(amount); locked.add(e);
        }

        h.setFagreedAmount(money(total)); h.setFstatus(STATUS_CONFIRMED); h.setFapprovalStatus(APPROVAL_AUDITED);
        touch(h,operatorId); requireOne(claimMapper.updateById(h),"供应商索赔单");
        outboxService.append(h.getFtenantId(),h.getForgId(),"PURCHASE_CLAIM_CONFIRMED","SUPPLIER_CLAIM",
                h.getFid(),version(h),"ERP_SUPPLIER_CLAIM",h.getFnumber(),h.getFdate(),operatorId,eventPayload(h,locked));
        return new ClaimDetail(h,List.copyOf(locked));
    }

    @Transactional(rollbackFor=Exception.class)
    public ClaimDetail reject(Long fid,String tenantId,Long operatorId){
        SupplierClaimEntity h=requireClaim(fid,tenantId,true);
        if(!STATUS_DRAFT.equals(h.getFstatus())||!APPROVAL_SUBMITTED.equals(h.getFapprovalStatus()))
            throw new BizException("仅已提交供应商索赔允许驳回");
        h.setFapprovalStatus(APPROVAL_REJECTED); touch(h,operatorId);
        requireOne(claimMapper.updateById(h),"供应商索赔单"); return detail(fid,tenantId);
    }

    @Transactional(rollbackFor=Exception.class)
    public ClaimDetail cancel(Long fid,String tenantId,Long operatorId){
        SupplierClaimEntity h=requireClaim(fid,tenantId,true);
        if(STATUS_CONFIRMED.equals(h.getFstatus())) throw new BizException("已确认供应商索赔不能直接取消");
        if(STATUS_CANCELLED.equals(h.getFstatus())) return detail(fid,tenantId);
        h.setFstatus(STATUS_CANCELLED); touch(h,operatorId);
        requireOne(claimMapper.updateById(h),"供应商索赔单"); return detail(fid,tenantId);
    }

    private SupplierClaimEntity requireClaim(Long id,String tenant,boolean lock){
        String t=requireTenant(tenant);
        SupplierClaimEntity h=lock?claimMapper.selectByIdForUpdate(id,t):
                claimMapper.selectOne(new LambdaQueryWrapper<SupplierClaimEntity>()
                        .eq(SupplierClaimEntity::getFid,id).eq(SupplierClaimEntity::getFtenantId,t).last("limit 1"));
        if(h==null) throw new BizException("供应商索赔单不存在: "+id); return h;
    }
    private List<SupplierClaimEntryEntity> listEntries(Long id){
        return claimEntryMapper.selectList(new LambdaQueryWrapper<SupplierClaimEntryEntity>()
                .eq(SupplierClaimEntryEntity::getFsupplierClaimId,id).orderByAsc(SupplierClaimEntryEntity::getFlineNo));
    }
    private Map<String,Object> eventPayload(SupplierClaimEntity h,List<SupplierClaimEntryEntity> es){
        Map<String,Object> p=new LinkedHashMap<>();
        p.put("supplierClaimId",h.getFid());p.put("supplierClaimNo",h.getFnumber());p.put("purchaseOrderId",h.getFpurchaseOrderId());
        p.put("purchaseReturnId",h.getFpurchaseReturnId());p.put("businessPartnerId",h.getFbusinessPartnerId());
        p.put("businessPartnerCode",h.getFbusinessPartnerCode());p.put("businessPartnerName",h.getFbusinessPartnerName());
        p.put("currencyCode",h.getFcurrencyCode());p.put("claimType",h.getFclaimType());p.put("requestedAmount",h.getFrequestedAmount());
        p.put("agreedAmount",h.getFagreedAmount());p.put("entries",es.stream().map(e->{
            Map<String,Object>x=new LinkedHashMap<>();x.put("supplierClaimEntryId",e.getFid());x.put("purchaseOrderEntryId",e.getFpurchaseOrderEntryId());
            x.put("purchaseReturnEntryId",e.getFpurchaseReturnEntryId());x.put("materialId",e.getFmaterialId());
            x.put("materialCode",e.getFmaterialCode());x.put("requestedAmount",e.getFrequestedAmount());x.put("agreedAmount",e.getFagreedAmount());return x;
        }).toList()); return p;
    }
    private String normalizeClaimType(String v){String x=v.trim().toUpperCase(Locale.ROOT);if(!List.of("RETURN","QUALITY","DELAY","SHORTAGE","OTHER").contains(x))throw new BizException("索赔类型不支持: "+v);return x;}
    private void ensureNumberUnique(String t,String n){Long c=claimMapper.selectCount(new LambdaQueryWrapper<SupplierClaimEntity>().eq(SupplierClaimEntity::getFtenantId,t).eq(SupplierClaimEntity::getFnumber,n));if(c!=null&&c>0)throw new BizException("供应商索赔单号已存在: "+n);}
    private String number(String p,LocalDate d,Long id){String s=String.valueOf(id);s=s.substring(Math.max(0,s.length()-8));return p+d.format(DateTimeFormatter.BASIC_ISO_DATE)+"-"+s;}
    private String requireTenant(String t){if(!StringUtils.hasText(t))throw new BizException("tenantId 不能为空");return t.trim();}
    private String trim(String v){return StringUtils.hasText(v)?v.trim():null;}
    private BigDecimal money(BigDecimal v){return (v==null?BigDecimal.ZERO:v).setScale(2,RoundingMode.HALF_UP);}
    private long version(SupplierClaimEntity v){return v.getFversion()==null?0:v.getFversion().longValue();}
    private void initHeader(SupplierClaimEntity v,Long op){LocalDateTime n=LocalDateTime.now();v.setFcreateBy(op);v.setFcreateTime(n);v.setFmodifyBy(op);v.setFmodifyTime(n);v.setFdeleteFlag(0);v.setFversion(0);}
    private void initEntry(SupplierClaimEntryEntity v,Long op,LocalDateTime n){v.setFcreateBy(op);v.setFcreateTime(n);v.setFmodifyBy(op);v.setFmodifyTime(n);v.setFdeleteFlag(0);v.setFversion(0);}
    private void touch(SupplierClaimEntity v,Long op){v.setFmodifyBy(op);v.setFmodifyTime(LocalDateTime.now());}
    private void requireOne(int n,String w){if(n!=1)throw new BizException(w+"已被其他请求修改，请刷新后重试");}
}
