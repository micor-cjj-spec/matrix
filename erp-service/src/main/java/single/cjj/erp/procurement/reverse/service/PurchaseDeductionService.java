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
import single.cjj.erp.procurement.reverse.dto.PurchaseReverseContracts.*;
import single.cjj.erp.procurement.reverse.entity.PurchaseDeductionEntity;
import single.cjj.erp.procurement.reverse.entity.PurchaseDeductionEntryEntity;
import single.cjj.erp.procurement.reverse.entity.SupplierClaimEntity;
import single.cjj.erp.procurement.reverse.entity.SupplierClaimEntryEntity;
import single.cjj.erp.procurement.reverse.mapper.PurchaseDeductionEntryMapper;
import single.cjj.erp.procurement.reverse.mapper.PurchaseDeductionMapper;
import single.cjj.erp.procurement.reverse.mapper.SupplierClaimEntryMapper;
import single.cjj.erp.procurement.reverse.mapper.SupplierClaimMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PurchaseDeductionService {

    private static final String STATUS_DRAFT="DRAFT";
    private static final String STATUS_CONFIRMED="CONFIRMED";
    private static final String STATUS_CANCELLED="CANCELLED";
    private static final String APPROVAL_DRAFT="DRAFT";
    private static final String APPROVAL_SUBMITTED="SUBMITTED";
    private static final String APPROVAL_AUDITED="AUDITED";
    private static final String APPROVAL_REJECTED="REJECTED";

    private final PurchaseDeductionMapper deductionMapper;
    private final PurchaseDeductionEntryMapper deductionEntryMapper;
    private final SupplierClaimMapper claimMapper;
    private final SupplierClaimEntryMapper claimEntryMapper;
    private final BusinessEventOutboxService outboxService;

    public PurchaseDeductionService(
            PurchaseDeductionMapper deductionMapper,
            PurchaseDeductionEntryMapper deductionEntryMapper,
            SupplierClaimMapper claimMapper,
            SupplierClaimEntryMapper claimEntryMapper,
            BusinessEventOutboxService outboxService
    ){
        this.deductionMapper=deductionMapper; this.deductionEntryMapper=deductionEntryMapper;
        this.claimMapper=claimMapper; this.claimEntryMapper=claimEntryMapper; this.outboxService=outboxService;
    }

    public DeductionDetail detail(Long fid,String tenantId){
        PurchaseDeductionEntity h=requireDeduction(fid,tenantId,false);
        return new DeductionDetail(h,listEntries(fid));
    }

    public IPage<PurchaseDeductionEntity> page(String tenantId,Long orgId,Long claimId,String status,int page,int size){
        return deductionMapper.selectPage(new Page<>(Math.max(page,1),Math.max(size,1)),
                new LambdaQueryWrapper<PurchaseDeductionEntity>()
                        .eq(PurchaseDeductionEntity::getFtenantId,requireTenant(tenantId))
                        .eq(orgId!=null,PurchaseDeductionEntity::getForgId,orgId)
                        .eq(claimId!=null,PurchaseDeductionEntity::getFsupplierClaimId,claimId)
                        .eq(StringUtils.hasText(status),PurchaseDeductionEntity::getFstatus,status)
                        .orderByDesc(PurchaseDeductionEntity::getFdate).orderByDesc(PurchaseDeductionEntity::getFid));
    }

    @Transactional(rollbackFor=Exception.class)
    public DeductionDetail create(DeductionCreateRequest request,Long operatorId){
        String tenant=requireTenant(request.ftenantId());
        SupplierClaimEntity claim=claimMapper.selectByIdForUpdate(request.fsupplierClaimId(),tenant);
        if(claim==null || !STATUS_CONFIRMED.equals(claim.getFstatus())
                || !APPROVAL_AUDITED.equals(claim.getFapprovalStatus()))
            throw new BizException("只有已确认供应商索赔允许创建扣款");

        LocalDate date=request.fdate()==null?LocalDate.now():request.fdate();
        Long deductionId=IdWorker.getId();
        String number=StringUtils.hasText(request.fnumber())?request.fnumber().trim():number(date,deductionId);
        ensureNumberUnique(tenant,number);

        List<DeductionEntryRequest> requested=new ArrayList<>(request.entries());
        requested.sort(Comparator.comparing(DeductionEntryRequest::fsupplierClaimEntryId));
        Set<Long> seen=new HashSet<>();
        List<PurchaseDeductionEntryEntity> entries=new ArrayList<>();
        BigDecimal total=BigDecimal.ZERO;
        LocalDateTime now=LocalDateTime.now();

        for(int i=0;i<requested.size();i++){
            DeductionEntryRequest item=requested.get(i);
            if(!seen.add(item.fsupplierClaimEntryId()))
                throw new BizException("同一扣款单不能重复引用索赔分录: "+item.fsupplierClaimEntryId());
            SupplierClaimEntryEntity claimEntry=claimEntryMapper.selectByIdForUpdate(item.fsupplierClaimEntryId(),tenant);
            if(claimEntry==null || !claim.getFid().equals(claimEntry.getFsupplierClaimId()))
                throw new BizException("索赔分录不存在或归属不匹配: "+item.fsupplierClaimEntryId());
            BigDecimal available=money(claimEntry.getFagreedAmount()).subtract(money(claimEntry.getFdeductedAmount()));
            BigDecimal amount=money(item.famount());
            if(amount.compareTo(available)>0)
                throw new BizException("扣款金额超过索赔剩余可扣金额: claimEntry="+claimEntry.getFid()+", available="+available);

            PurchaseDeductionEntryEntity e=new PurchaseDeductionEntryEntity();
            e.setFid(IdWorker.getId());e.setFtenantId(tenant);e.setForgId(claim.getForgId());
            e.setFpurchaseDeductionId(deductionId);e.setFlineNo(i+1);e.setFsupplierClaimEntryId(claimEntry.getFid());
            e.setFpurchaseOrderId(claimEntry.getFpurchaseOrderId());e.setFpurchaseOrderEntryId(claimEntry.getFpurchaseOrderEntryId());
            e.setFmaterialId(claimEntry.getFmaterialId());e.setFmaterialCode(claimEntry.getFmaterialCode());e.setFmaterialName(claimEntry.getFmaterialName());
            e.setFamount(amount);initEntry(e,operatorId,now);entries.add(e);total=total.add(amount);
        }

        PurchaseDeductionEntity h=new PurchaseDeductionEntity();
        h.setFid(deductionId);h.setFtenantId(tenant);h.setForgId(claim.getForgId());h.setFnumber(number);h.setFdate(date);
        h.setFsupplierClaimId(claim.getFid());h.setFpurchaseOrderId(claim.getFpurchaseOrderId());
        h.setFbusinessPartnerId(claim.getFbusinessPartnerId());h.setFbusinessPartnerCode(claim.getFbusinessPartnerCode());
        h.setFbusinessPartnerName(claim.getFbusinessPartnerName());h.setFcurrencyCode(claim.getFcurrencyCode());h.setFamount(money(total));
        h.setFreason(trim(request.freason()));h.setFstatus(STATUS_DRAFT);h.setFapprovalStatus(APPROVAL_DRAFT);initHeader(h,operatorId);
        requireOne(deductionMapper.insert(h),"采购扣款单");
        for(PurchaseDeductionEntryEntity e:entries)requireOne(deductionEntryMapper.insert(e),"采购扣款分录");
        return new DeductionDetail(h,List.copyOf(entries));
    }

    @Transactional(rollbackFor=Exception.class)
    public DeductionDetail submit(Long fid,String tenantId,Long operatorId){
        PurchaseDeductionEntity h=requireDeduction(fid,tenantId,true);
        if(!STATUS_DRAFT.equals(h.getFstatus())
                || !(APPROVAL_DRAFT.equals(h.getFapprovalStatus())||APPROVAL_REJECTED.equals(h.getFapprovalStatus())))
            throw new BizException("仅草稿或已驳回采购扣款单允许提交");
        h.setFapprovalStatus(APPROVAL_SUBMITTED);touch(h,operatorId);requireOne(deductionMapper.updateById(h),"采购扣款单");
        return detail(fid,tenantId);
    }

    @Transactional(rollbackFor=Exception.class)
    public DeductionDetail confirm(Long fid,String tenantId,Long operatorId){
        PurchaseDeductionEntity h=requireDeduction(fid,tenantId,true);
        if(!STATUS_DRAFT.equals(h.getFstatus())||!APPROVAL_SUBMITTED.equals(h.getFapprovalStatus()))
            throw new BizException("仅已提交采购扣款单允许确认");

        SupplierClaimEntity claim=claimMapper.selectByIdForUpdate(h.getFsupplierClaimId(),h.getFtenantId());
        if(claim==null || !STATUS_CONFIRMED.equals(claim.getFstatus()))
            throw new BizException("来源供应商索赔不存在或已失效");

        List<PurchaseDeductionEntryEntity> entries=listEntries(fid);
        BigDecimal deductionTotal=BigDecimal.ZERO;
        for(PurchaseDeductionEntryEntity source:entries.stream().sorted(Comparator.comparing(PurchaseDeductionEntryEntity::getFsupplierClaimEntryId)).toList()){
            SupplierClaimEntryEntity claimEntry=claimEntryMapper.selectByIdForUpdate(source.getFsupplierClaimEntryId(),h.getFtenantId());
            if(claimEntry==null)throw new BizException("索赔分录不存在: "+source.getFsupplierClaimEntryId());
            BigDecimal next=money(claimEntry.getFdeductedAmount()).add(money(source.getFamount()));
            if(next.compareTo(money(claimEntry.getFagreedAmount()))>0)
                throw new BizException("累计扣款金额超过索赔确认金额: claimEntry="+claimEntry.getFid());
            claimEntry.setFdeductedAmount(next);claimEntry.setFmodifyBy(operatorId);claimEntry.setFmodifyTime(LocalDateTime.now());
            requireOne(claimEntryMapper.updateById(claimEntry),"供应商索赔分录");
            deductionTotal=deductionTotal.add(source.getFamount());
        }

        List<SupplierClaimEntryEntity> claimEntries=claimEntryMapper.selectList(new LambdaQueryWrapper<SupplierClaimEntryEntity>()
                .eq(SupplierClaimEntryEntity::getFsupplierClaimId,claim.getFid()).orderByAsc(SupplierClaimEntryEntity::getFlineNo));
        BigDecimal totalDeducted=claimEntries.stream().map(e->money(e.getFdeductedAmount())).reduce(BigDecimal.ZERO,BigDecimal::add);
        claim.setFdeductedAmount(money(totalDeducted));
        if(money(claim.getFagreedAmount()).compareTo(BigDecimal.ZERO)==0)claim.setFdeductionStatus("COMPLETE");
        else if(claim.getFdeductedAmount().compareTo(money(claim.getFagreedAmount()))>=0)claim.setFdeductionStatus("COMPLETE");
        else if(claim.getFdeductedAmount().compareTo(BigDecimal.ZERO)>0)claim.setFdeductionStatus("PARTIAL");
        else claim.setFdeductionStatus("NONE");
        claim.setFmodifyBy(operatorId);claim.setFmodifyTime(LocalDateTime.now());
        requireOne(claimMapper.updateById(claim),"供应商索赔单");

        h.setFamount(money(deductionTotal));h.setFstatus(STATUS_CONFIRMED);h.setFapprovalStatus(APPROVAL_AUDITED);touch(h,operatorId);
        requireOne(deductionMapper.updateById(h),"采购扣款单");
        outboxService.append(h.getFtenantId(),h.getForgId(),"PURCHASE_DEDUCTION_CONFIRMED","PURCHASE_DEDUCTION",
                h.getFid(),version(h),"ERP_PURCHASE_DEDUCTION",h.getFnumber(),h.getFdate(),operatorId,eventPayload(h,claim,entries));
        return new DeductionDetail(h,entries);
    }

    @Transactional(rollbackFor=Exception.class)
    public DeductionDetail reject(Long fid,String tenantId,Long operatorId){
        PurchaseDeductionEntity h=requireDeduction(fid,tenantId,true);
        if(!STATUS_DRAFT.equals(h.getFstatus())||!APPROVAL_SUBMITTED.equals(h.getFapprovalStatus()))
            throw new BizException("仅已提交采购扣款单允许驳回");
        h.setFapprovalStatus(APPROVAL_REJECTED);touch(h,operatorId);requireOne(deductionMapper.updateById(h),"采购扣款单");
        return detail(fid,tenantId);
    }

    @Transactional(rollbackFor=Exception.class)
    public DeductionDetail cancel(Long fid,String tenantId,Long operatorId){
        PurchaseDeductionEntity h=requireDeduction(fid,tenantId,true);
        if(STATUS_CONFIRMED.equals(h.getFstatus()))throw new BizException("已确认采购扣款不能直接取消");
        if(STATUS_CANCELLED.equals(h.getFstatus()))return detail(fid,tenantId);
        h.setFstatus(STATUS_CANCELLED);touch(h,operatorId);requireOne(deductionMapper.updateById(h),"采购扣款单");return detail(fid,tenantId);
    }

    private PurchaseDeductionEntity requireDeduction(Long id,String tenant,boolean lock){
        String t=requireTenant(tenant);PurchaseDeductionEntity h=lock?deductionMapper.selectByIdForUpdate(id,t):
                deductionMapper.selectOne(new LambdaQueryWrapper<PurchaseDeductionEntity>().eq(PurchaseDeductionEntity::getFid,id)
                        .eq(PurchaseDeductionEntity::getFtenantId,t).last("limit 1"));
        if(h==null)throw new BizException("采购扣款单不存在: "+id);return h;
    }
    private List<PurchaseDeductionEntryEntity> listEntries(Long id){return deductionEntryMapper.selectList(new LambdaQueryWrapper<PurchaseDeductionEntryEntity>()
            .eq(PurchaseDeductionEntryEntity::getFpurchaseDeductionId,id).orderByAsc(PurchaseDeductionEntryEntity::getFlineNo));}
    private Map<String,Object> eventPayload(PurchaseDeductionEntity h,SupplierClaimEntity c,List<PurchaseDeductionEntryEntity> es){
        Map<String,Object>p=new LinkedHashMap<>();p.put("purchaseDeductionId",h.getFid());p.put("purchaseDeductionNo",h.getFnumber());
        p.put("supplierClaimId",c.getFid());p.put("purchaseOrderId",h.getFpurchaseOrderId());p.put("businessPartnerId",h.getFbusinessPartnerId());
        p.put("businessPartnerCode",h.getFbusinessPartnerCode());p.put("businessPartnerName",h.getFbusinessPartnerName());
        p.put("currencyCode",h.getFcurrencyCode());p.put("amount",h.getFamount());
        p.put("entries",es.stream().map(e->{Map<String,Object>x=new LinkedHashMap<>();x.put("purchaseDeductionEntryId",e.getFid());
            x.put("supplierClaimEntryId",e.getFsupplierClaimEntryId());x.put("purchaseOrderId",e.getFpurchaseOrderId());
            x.put("purchaseOrderEntryId",e.getFpurchaseOrderEntryId());x.put("materialId",e.getFmaterialId());x.put("materialCode",e.getFmaterialCode());
            x.put("amount",e.getFamount());return x;}).toList());return p;
    }
    private void ensureNumberUnique(String t,String n){Long c=deductionMapper.selectCount(new LambdaQueryWrapper<PurchaseDeductionEntity>()
            .eq(PurchaseDeductionEntity::getFtenantId,t).eq(PurchaseDeductionEntity::getFnumber,n));if(c!=null&&c>0)throw new BizException("采购扣款单号已存在: "+n);}
    private String number(LocalDate d,Long id){String s=String.valueOf(id);s=s.substring(Math.max(0,s.length()-8));return "DED"+d.format(DateTimeFormatter.BASIC_ISO_DATE)+"-"+s;}
    private String requireTenant(String t){if(!StringUtils.hasText(t))throw new BizException("tenantId 不能为空");return t.trim();}
    private String trim(String v){return StringUtils.hasText(v)?v.trim():null;}
    private BigDecimal money(BigDecimal v){return (v==null?BigDecimal.ZERO:v).setScale(2,RoundingMode.HALF_UP);}
    private long version(PurchaseDeductionEntity v){return v.getFversion()==null?0:v.getFversion().longValue();}
    private void initHeader(PurchaseDeductionEntity v,Long op){LocalDateTime n=LocalDateTime.now();v.setFcreateBy(op);v.setFcreateTime(n);v.setFmodifyBy(op);v.setFmodifyTime(n);v.setFdeleteFlag(0);v.setFversion(0);}
    private void initEntry(PurchaseDeductionEntryEntity v,Long op,LocalDateTime n){v.setFcreateBy(op);v.setFcreateTime(n);v.setFmodifyBy(op);v.setFmodifyTime(n);v.setFdeleteFlag(0);v.setFversion(0);}
    private void touch(PurchaseDeductionEntity v,Long op){v.setFmodifyBy(op);v.setFmodifyTime(LocalDateTime.now());}
    private void requireOne(int n,String w){if(n!=1)throw new BizException(w+"已被其他请求修改，请刷新后重试");}
}
