package single.cjj.erp.sales.commercial.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.crm.opportunity.entity.CrmOpportunityEntity;
import single.cjj.erp.crm.opportunity.mapper.CrmOpportunityMapper;
import single.cjj.erp.crm.support.CustomerPartnerValidator;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.integration.base.BaseBusinessPartnerContracts.BusinessPartnerDetail;
import single.cjj.erp.sales.commercial.dto.SalesCommercialContracts.*;
import single.cjj.erp.sales.commercial.entity.*;
import single.cjj.erp.sales.commercial.mapper.*;
import single.cjj.erp.sales.commercial.workflow.SalesWorkflowCoordinator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SalesCommercialService {
    private final SalesQuoteMapper quotes;
    private final SalesQuoteEntryMapper quoteEntries;
    private final SalesContractMapper contracts;
    private final SalesContractEntryMapper contractEntries;
    private final CrmOpportunityMapper opportunities;
    private final CustomerPartnerValidator customers;
    private final BusinessEventOutboxService outbox;
    private final SalesCommercialActionAuditMapper audits;
    private final SalesWorkflowCoordinator workflow;

    public SalesCommercialService(
            SalesQuoteMapper quotes, SalesQuoteEntryMapper quoteEntries,
            SalesContractMapper contracts, SalesContractEntryMapper contractEntries,
            CrmOpportunityMapper opportunities, CustomerPartnerValidator customers,
            BusinessEventOutboxService outbox,
            SalesCommercialActionAuditMapper audits,
            SalesWorkflowCoordinator workflow) {
        this.quotes = quotes;
        this.quoteEntries = quoteEntries;
        this.contracts = contracts;
        this.contractEntries = contractEntries;
        this.opportunities = opportunities;
        this.customers = customers;
        this.outbox = outbox;
        this.audits = audits;
        this.workflow = workflow;
    }

    public IPage<SalesQuoteEntity> quotePage(String tenantId, Long orgId, String status, int page, int size) {
        return quotes.selectPage(new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size))),
                new LambdaQueryWrapper<SalesQuoteEntity>()
                        .eq(SalesQuoteEntity::getFtenantId, tenant(tenantId))
                        .eq(orgId != null, SalesQuoteEntity::getForgId, orgId)
                        .eq(StringUtils.hasText(status), SalesQuoteEntity::getFstatus, status)
                        .orderByDesc(SalesQuoteEntity::getFdate).orderByDesc(SalesQuoteEntity::getFid));
    }

    public QuoteDetail quoteDetail(Long id, String tenantId) {
        SalesQuoteEntity q = requireQuote(id, tenantId, false);
        return new QuoteDetail(q, quoteEntries.selectList(new LambdaQueryWrapper<SalesQuoteEntryEntity>()
                .eq(SalesQuoteEntryEntity::getFtenantId, q.getFtenantId())
                .eq(SalesQuoteEntryEntity::getFquoteId, id)
                .orderByAsc(SalesQuoteEntryEntity::getFlineNo)));
    }

    @Transactional(rollbackFor = Exception.class)
    public QuoteDetail createQuote(CreateQuote request, Long operator) {
        String tenant = tenant(request.ftenantId());
        CrmOpportunityEntity opportunity = opportunities.selectOne(
                new LambdaQueryWrapper<CrmOpportunityEntity>()
                        .eq(CrmOpportunityEntity::getFid, request.fopportunityId())
                        .eq(CrmOpportunityEntity::getFtenantId, tenant)
                        .last("LIMIT 1"));
        if (opportunity == null || "LOST".equals(opportunity.getFstatus())) {
            throw new BizException("仅允许关联当前租户的有效商机");
        }
        if (request.forgId() != null && !Objects.equals(opportunity.getForgId(), request.forgId())) {
            throw new BizException("报价组织必须与商机组织一致");
        }
        if (!Objects.equals(opportunity.getFbusinessPartnerId(), request.fbusinessPartnerId())) {
            throw new BizException("报价客户必须与商机客户一致");
        }
        BusinessPartnerDetail partner = customers.requireActiveCustomer(request.fbusinessPartnerId(), tenant);
        String type = StringUtils.hasText(request.fquoteType())
                ? request.fquoteType().trim().toUpperCase(Locale.ROOT) : "QUOTE";
        if (!Set.of("QUOTE", "TENDER").contains(type)) throw new BizException("报价类型必须为 QUOTE 或 TENDER");
        if ("TENDER".equals(type) && !StringUtils.hasText(request.ftenderReference())) {
            throw new BizException("投标报价必须填写招标编号");
        }
        if (request.fvalidUntil().isBefore(LocalDate.now())) throw new BizException("报价有效期不得早于当前日期");
        if (request.entries() == null || request.entries().isEmpty()) throw new BizException("报价明细不能为空");

        LocalDate date = LocalDate.now();
        Long id = IdWorker.getId();
        String no = StringUtils.hasText(request.fnumber())
                ? request.fnumber().trim() : number("SQ", date, id);
        if (quotes.selectCount(new LambdaQueryWrapper<SalesQuoteEntity>()
                .eq(SalesQuoteEntity::getFtenantId, tenant).eq(SalesQuoteEntity::getFnumber, no)) > 0) {
            throw new BizException("销售报价单号已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        SalesQuoteEntity q = new SalesQuoteEntity();
        q.setFid(id); q.setFtenantId(tenant); q.setForgId(opportunity.getForgId());
        q.setFnumber(no); q.setFdate(date);
        q.setFquoteType(type); q.setFtenderReference(blankToNull(request.ftenderReference()));
        q.setFopportunityId(opportunity.getFid());
        q.setFbusinessPartnerId(partner.fid());
        q.setFbusinessPartnerCode(partner.fcode());
        q.setFbusinessPartnerName(partner.fname());
        q.setFcurrencyCode(request.fcurrencyCode().trim().toUpperCase(Locale.ROOT));
        q.setFvalidUntil(request.fvalidUntil());
        q.setFdeliveryTermCode(blankToNull(request.fdeliveryTermCode()));
        q.setFpaymentTermCode(blankToNull(request.fpaymentTermCode()));
        q.setFstatus("DRAFT");
        q.setFcreateBy(operator); q.setFmodifyBy(operator);
        q.setFcreateTime(now); q.setFmodifyTime(now);
        q.setFdeleteFlag(0); q.setFversion(0);

        CalculatedQuoteLines calculation = calculateQuoteLines(id, tenant, request.entries(), now);
        List<SalesQuoteEntryEntity> lines = calculation.entries();
        BigDecimal net = calculation.net(), tax = calculation.tax();
        q.setFnetAmount(net); q.setFtaxAmount(tax); q.setFgrossAmount(net.add(tax));
        one(quotes.insert(q), "销售报价");
        for (SalesQuoteEntryEntity e : lines) one(quoteEntries.insert(e), "销售报价明细");
        recordAudit(tenant, q.getForgId(), "SALES_QUOTE", q.getFid(), "CREATE",
                null, "DRAFT", operator);
        return new QuoteDetail(q, lines);
    }

    private record CalculatedQuoteLines(List<SalesQuoteEntryEntity> entries,
                                        BigDecimal net, BigDecimal tax) {}

    private CalculatedQuoteLines calculateQuoteLines(Long quoteId, String tenant,
                                                      List<QuoteLine> requested, LocalDateTime now) {
        if (requested == null || requested.isEmpty() || requested.size() > 100) {
            throw new BizException("报价明细必须包含 1-100 行");
        }
        List<SalesQuoteEntryEntity> lines = new ArrayList<>();
        BigDecimal net = BigDecimal.ZERO, tax = BigDecimal.ZERO;
        for (QuoteLine line : requested) {
            if (line == null || !StringUtils.hasText(line.fdescription()) || line.fquantity() == null
                    || line.fquantity().signum() <= 0 || line.funitPrice() == null
                    || line.funitPrice().signum() < 0 || line.ftaxRate() == null
                    || line.ftaxRate().signum() < 0 || line.ftaxRate().compareTo(new BigDecimal("100")) > 0) {
                throw new BizException("报价明细数量、价格或税率不合法");
            }
            SalesQuoteEntryEntity e = new SalesQuoteEntryEntity();
            e.setFid(IdWorker.getId()); e.setFtenantId(tenant); e.setFquoteId(quoteId);
            e.setFlineNo(lines.size() + 1); e.setFmaterialCode(blankToNull(line.fmaterialCode()));
            e.setFdescription(line.fdescription().trim());
            e.setFquantity(line.fquantity()); e.setFunitPrice(line.funitPrice());
            e.setFtaxRate(line.ftaxRate());
            e.setFnetAmount(money(line.fquantity().multiply(line.funitPrice())));
            e.setFtaxAmount(money(e.getFnetAmount().multiply(line.ftaxRate()).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)));
            e.setFgrossAmount(e.getFnetAmount().add(e.getFtaxAmount()));
            e.setFcreateTime(now); e.setFdeleteFlag(0);
            net = net.add(e.getFnetAmount()); tax = tax.add(e.getFtaxAmount()); lines.add(e);
        }
        return new CalculatedQuoteLines(lines, net, tax);
    }

    @Transactional(rollbackFor = Exception.class)
    public QuoteDetail updateQuote(Long id, UpdateQuote request, Long operator) {
        String tenant = tenant(request.ftenantId());
        SalesQuoteEntity q = requireQuote(id, tenant, true);
        if (!"DRAFT".equals(q.getFstatus())) {
            throw new BizException("只有 DRAFT 报价可以修改");
        }
        if (request.fvalidUntil().isBefore(LocalDate.now())) {
            throw new BizException("报价有效期不得早于当前日期");
        }
        LocalDateTime now = LocalDateTime.now();
        CalculatedQuoteLines calc = calculateQuoteLines(id, tenant, request.entries(), now);
        q.setFvalidUntil(request.fvalidUntil());
        q.setFdeliveryTermCode(blankToNull(request.fdeliveryTermCode()));
        q.setFpaymentTermCode(blankToNull(request.fpaymentTermCode()));
        q.setFnetAmount(calc.net());
        q.setFtaxAmount(calc.tax());
        q.setFgrossAmount(calc.net().add(calc.tax()));
        q.setFmodifyBy(operator);
        q.setFmodifyTime(now);
        one(quotes.updateById(q), "销售报价");
        // Draft lines have no downstream references. Physically replace them to
        // avoid collision with the existing unique (tenant, quote, lineNo) index.
        quoteEntries.deleteDraftEntries(id, tenant);
        for (SalesQuoteEntryEntity e : calc.entries()) {
            one(quoteEntries.insert(e), "报价明细");
        }
        recordAudit(tenant, q.getForgId(), "SALES_QUOTE", q.getFid(), "UPDATE",
                "DRAFT", "DRAFT", operator);
        return new QuoteDetail(q, calc.entries());
    }

    @Transactional(rollbackFor = Exception.class)
    public SalesQuoteEntity transitionQuote(Long id, String tenantId, String action, Long operator) {
        if ("workflowReject".equals(action) || (workflow.enabled() && "approve".equals(action))) {
            throw new BizException("流程审批结果只能由 Workflow 可信回调写入");
        }
        if (workflow.enabled() && "withdraw".equals(action)) {
            throw new BizException("流程撤回需要先撤销 Workflow 实例，当前暂不支持");
        }
        return transitionQuoteInternal(id, tenantId, action, operator);
    }

    @Transactional(rollbackFor = Exception.class)
    public SalesQuoteEntity transitionQuoteFromWorkflow(Long id, String tenantId,
                                                         boolean approved, Long operator) {
        if (!workflow.enabled()) throw new BizException("销售 Workflow 未开启");
        return transitionQuoteInternal(id, tenantId, approved ? "approve" : "workflowReject", operator);
    }

    private SalesQuoteEntity transitionQuoteInternal(Long id, String tenantId,
                                                      String action, Long operator) {
        SalesQuoteEntity q = requireQuote(id, tenantId, true);
        String before = q.getFstatus();
        if ("approve".equals(action)
                && (q.getFcreateBy() == null || Objects.equals(operator, q.getFcreateBy()))) {
            throw new BizException("审批人与报价制单人不能为同一人");
        }
        String next = switch (action) {
            case "submit" -> next(q.getFstatus(), "DRAFT", "SUBMITTED");
            case "approve" -> next(q.getFstatus(), "SUBMITTED", "APPROVED");
            case "send" -> next(q.getFstatus(), "APPROVED", "SENT");
            case "accept" -> next(q.getFstatus(), "SENT", "ACCEPTED");
            case "reject" -> next(q.getFstatus(), "SENT", "REJECTED");
            case "withdraw" -> next(q.getFstatus(), "SUBMITTED", "DRAFT");
            case "workflowReject" -> next(q.getFstatus(), "SUBMITTED", "APPROVAL_REJECTED");
            case "cancel" -> next(q.getFstatus(), "DRAFT", "CANCELLED");
            case "expire" -> {
                if (!"SENT".equals(q.getFstatus()) || !q.getFvalidUntil().isBefore(LocalDate.now())) {
                    throw new BizException("只有已过有效期的 SENT 报价允许标记过期");
                }
                yield "EXPIRED";
            }
            default -> throw new BizException("不支持的报价动作");
        };
        if (Set.of("SENT", "ACCEPTED").contains(next)
                && q.getFvalidUntil().isBefore(LocalDate.now())) {
            throw new BizException("报价已过期，不能接受");
        }
        q.setFstatus(next); q.setFmodifyBy(operator); q.setFmodifyTime(LocalDateTime.now());
        if ("ACCEPTED".equals(next)) {
            q.setFacceptedBy(operator); q.setFacceptedTime(LocalDateTime.now());
        }
        one(quotes.updateById(q), "销售报价");
        recordAudit(q.getFtenantId(), q.getForgId(), "SALES_QUOTE", q.getFid(),
                action.toUpperCase(Locale.ROOT), before, next, operator);
        if ("submit".equals(action)) {
            workflow.enqueue(q.getFtenantId(), q.getForgId(), "SALES_QUOTE", q.getFid(), operator);
        }
        if ("ACCEPTED".equals(next)) {
            outbox.append(q.getFtenantId(), q.getForgId(), "SALES", "SALES_QUOTE_ACCEPTED",
                    "SALES_QUOTE", q.getFid(), version(q.getFversion()),
                    "ERP_SALES_QUOTE", q.getFnumber(), q.getFdate(), operator,
                    Map.of("quoteId", q.getFid(), "opportunityId", q.getFopportunityId(),
                            "businessPartnerId", q.getFbusinessPartnerId(), "grossAmount", q.getFgrossAmount()));
        }
        if ("CANCELLED".equals(next) || "EXPIRED".equals(next)) {
            outbox.append(q.getFtenantId(), q.getForgId(), "SALES",
                    "CANCELLED".equals(next) ? "SALES_QUOTE_CANCELLED" : "SALES_QUOTE_EXPIRED",
                    "SALES_QUOTE", q.getFid(), version(q.getFversion()),
                    "ERP_SALES_QUOTE", q.getFnumber(), q.getFdate(), operator,
                    Map.of("quoteId", q.getFid(), "status", next, "businessPartnerId", q.getFbusinessPartnerId()));
        }
        return q;
    }

    public IPage<SalesContractEntity> contractPage(String tenantId, Long orgId, int page, int size) {
        return contracts.selectPage(new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size))),
                new LambdaQueryWrapper<SalesContractEntity>()
                        .eq(SalesContractEntity::getFtenantId, tenant(tenantId))
                        .eq(orgId != null, SalesContractEntity::getForgId, orgId)
                        .orderByDesc(SalesContractEntity::getFdate).orderByDesc(SalesContractEntity::getFid));
    }

    public ContractDetail contractDetail(Long id, String tenantId) {
        SalesContractEntity c = requireContract(id, tenantId, false);
        return new ContractDetail(c, contractEntries.selectList(
                new LambdaQueryWrapper<SalesContractEntryEntity>()
                        .eq(SalesContractEntryEntity::getFtenantId, c.getFtenantId())
                        .eq(SalesContractEntryEntity::getFcontractId, id)
                        .orderByAsc(SalesContractEntryEntity::getFlineNo)));
    }

    @Transactional(rollbackFor = Exception.class)
    public ContractDetail createContract(CreateContract request, Long operator) {
        String tenant = tenant(request.ftenantId());
        if (request.fstartDate().isAfter(request.fendDate())) throw new BizException("合同结束日期不能早于开始日期");
        SalesQuoteEntity quote = requireQuote(request.fquoteId(), tenant, true);
        if (!"ACCEPTED".equals(quote.getFstatus())) throw new BizException("只能从客户已接受的报价创建销售合同");
        if (contracts.selectCount(new LambdaQueryWrapper<SalesContractEntity>()
                .eq(SalesContractEntity::getFtenantId, tenant)
                .eq(SalesContractEntity::getFquoteId, quote.getFid())) > 0) {
            throw new BizException("该报价已创建销售合同");
        }
        // Revalidate master data at contract creation. No duplicate customer copies.
        customers.requireActiveCustomer(quote.getFbusinessPartnerId(), tenant);
        Long id = IdWorker.getId();
        LocalDate date = LocalDate.now();
        String no = StringUtils.hasText(request.fnumber())
                ? request.fnumber().trim() : number("SC", date, id);
        if (contracts.selectCount(new LambdaQueryWrapper<SalesContractEntity>()
                .eq(SalesContractEntity::getFtenantId, tenant).eq(SalesContractEntity::getFnumber, no)) > 0) {
            throw new BizException("销售合同编号已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        SalesContractEntity c = new SalesContractEntity();
        c.setFid(id); c.setFtenantId(tenant); c.setForgId(quote.getForgId());
        c.setFnumber(no); c.setFdate(date); c.setFtitle(request.ftitle().trim());
        c.setFquoteId(quote.getFid()); c.setFopportunityId(quote.getFopportunityId());
        c.setFbusinessPartnerId(quote.getFbusinessPartnerId());
        c.setFbusinessPartnerCode(quote.getFbusinessPartnerCode());
        c.setFbusinessPartnerName(quote.getFbusinessPartnerName());
        c.setFcurrencyCode(quote.getFcurrencyCode());
        c.setFstartDate(request.fstartDate()); c.setFendDate(request.fendDate());
        c.setFdeliveryTermCode(quote.getFdeliveryTermCode()); c.setFpaymentTermCode(quote.getFpaymentTermCode());
        c.setFnetAmount(quote.getFnetAmount()); c.setFtaxAmount(quote.getFtaxAmount());
        c.setFgrossAmount(quote.getFgrossAmount());
        c.setFstatus("DRAFT"); c.setFapprovalStatus("DRAFT");
        c.setFcreateBy(operator); c.setFcreateTime(now);
        c.setFmodifyBy(operator); c.setFmodifyTime(now);
        c.setFdeleteFlag(0); c.setFversion(0);
        one(contracts.insert(c), "销售合同");

        List<SalesQuoteEntryEntity> original = quoteDetail(quote.getFid(), tenant).entries();
        if (original.isEmpty()) throw new BizException("报价没有明细，无法创建合同");
        List<SalesContractEntryEntity> lines = new ArrayList<>();
        for (SalesQuoteEntryEntity e : original) {
            SalesContractEntryEntity line = new SalesContractEntryEntity();
            line.setFid(IdWorker.getId()); line.setFtenantId(tenant); line.setFcontractId(id);
            line.setFsourceQuoteEntryId(e.getFid()); line.setFlineNo(e.getFlineNo());
            line.setFmaterialCode(e.getFmaterialCode()); line.setFdescription(e.getFdescription());
            line.setFquantity(e.getFquantity()); line.setFunitPrice(e.getFunitPrice());
            line.setFtaxRate(e.getFtaxRate()); line.setFnetAmount(e.getFnetAmount());
            line.setFtaxAmount(e.getFtaxAmount()); line.setFgrossAmount(e.getFgrossAmount());
            line.setForderedQuantity(BigDecimal.ZERO); line.setFdeliveredQuantity(BigDecimal.ZERO);
            line.setFinvoicedQuantity(BigDecimal.ZERO); line.setFsettledAmount(BigDecimal.ZERO);
            line.setFcreateTime(now); line.setFdeleteFlag(0);
            one(contractEntries.insert(line), "销售合同明细"); lines.add(line);
        }
        recordAudit(tenant, c.getForgId(), "SALES_CONTRACT", c.getFid(),
                "CREATE", null, "DRAFT/DRAFT", operator);
        return new ContractDetail(c, lines);
    }

    @Transactional(rollbackFor = Exception.class)
    public SalesContractEntity transitionContract(Long id, String tenantId, String action, Long operator) {
        if ("workflowReject".equals(action) || (workflow.enabled() && "approve".equals(action))) {
            throw new BizException("流程审批结果只能由 Workflow 可信回调写入");
        }
        return transitionContractInternal(id, tenantId, action, operator);
    }

    @Transactional(rollbackFor = Exception.class)
    public SalesContractEntity transitionContractFromWorkflow(Long id, String tenantId,
                                                                boolean approved, Long operator) {
        if (!workflow.enabled()) throw new BizException("销售 Workflow 未开启");
        return transitionContractInternal(id, tenantId, approved ? "approve" : "workflowReject", operator);
    }

    private SalesContractEntity transitionContractInternal(Long id, String tenantId,
                                                            String action, Long operator) {
        SalesContractEntity c = requireContract(id, tenantId, true);
        String before = c.getFstatus() + "/" + c.getFapprovalStatus();
        if ("approve".equals(action)
                && (c.getFcreateBy() == null || Objects.equals(operator, c.getFcreateBy()))) {
            throw new BizException("审批人与合同制单人不能为同一人");
        }
        switch (action) {
            case "submit" -> c.setFapprovalStatus(next(c.getFapprovalStatus(), "DRAFT", "SUBMITTED"));
            case "approve" -> {
                c.setFapprovalStatus(next(c.getFapprovalStatus(), "SUBMITTED", "APPROVED"));
                c.setFstatus("EFFECTIVE"); c.setFapprovedBy(operator); c.setFapprovedTime(LocalDateTime.now());
            }
            case "workflowReject" -> {
                c.setFapprovalStatus(next(c.getFapprovalStatus(), "SUBMITTED", "REJECTED"));
            }
            default -> throw new BizException("不支持的合同动作");
        }
        c.setFmodifyBy(operator); c.setFmodifyTime(LocalDateTime.now());
        one(contracts.updateById(c), "销售合同");
        recordAudit(c.getFtenantId(), c.getForgId(), "SALES_CONTRACT", c.getFid(),
                action.toUpperCase(Locale.ROOT), before,
                c.getFstatus() + "/" + c.getFapprovalStatus(), operator);
        if ("submit".equals(action)) {
            workflow.enqueue(c.getFtenantId(), c.getForgId(), "SALES_CONTRACT", c.getFid(), operator);
        }
        if ("approve".equals(action)) {
            outbox.append(c.getFtenantId(), c.getForgId(), "SALES", "SALES_CONTRACT_EFFECTIVE",
                    "SALES_CONTRACT", c.getFid(), version(c.getFversion()),
                    "ERP_SALES_CONTRACT", c.getFnumber(), c.getFdate(), operator,
                    Map.of("contractId", c.getFid(), "quoteId", c.getFquoteId(),
                            "businessPartnerId", c.getFbusinessPartnerId(), "grossAmount", c.getFgrossAmount()));
        }
        return c;
    }


    public List<SalesCommercialActionAuditEntity> auditHistory(
            String documentType, Long documentId, String tenantId, Long orgId) {
        if (!Set.of("SALES_QUOTE", "SALES_CONTRACT").contains(documentType)) {
            throw new BizException("不支持的审计单据类型");
        }
        if (orgId == null) throw new BizException("审计组织不能为空");
        return audits.selectList(new LambdaQueryWrapper<SalesCommercialActionAuditEntity>()
                .eq(SalesCommercialActionAuditEntity::getFtenantId, tenant(tenantId))
                .eq(SalesCommercialActionAuditEntity::getForgId, orgId)
                .eq(SalesCommercialActionAuditEntity::getFdocumentType, documentType)
                .eq(SalesCommercialActionAuditEntity::getFdocumentId, documentId)
                .orderByDesc(SalesCommercialActionAuditEntity::getFcreateTime)
                .orderByDesc(SalesCommercialActionAuditEntity::getFid)
                .last("LIMIT 200"));
    }

    private void recordAudit(String tenantId, Long orgId, String documentType,
                             Long documentId, String action, String before,
                             String after, Long operator) {
        if (orgId == null || operator == null || operator <= 0) {
            throw new BizException("缺少可靠的审计组织或操作人，禁止修改销售单据");
        }
        SalesCommercialActionAuditEntity record = new SalesCommercialActionAuditEntity();
        record.setFid(IdWorker.getId());
        record.setFtenantId(tenantId);
        record.setForgId(orgId);
        record.setFdocumentType(documentType);
        record.setFdocumentId(documentId);
        record.setFaction(action);
        record.setFbeforeStatus(before);
        record.setFafterStatus(after);
        record.setFoperatorId(operator);
        record.setFcreateTime(LocalDateTime.now());
        one(audits.insert(record), "销售操作审计");
    }

    private SalesQuoteEntity requireQuote(Long id, String tenantId, boolean lock) {
        String tenant = tenant(tenantId);
        SalesQuoteEntity q = lock ? quotes.selectByIdForUpdate(id, tenant)
                : quotes.selectOne(new LambdaQueryWrapper<SalesQuoteEntity>()
                        .eq(SalesQuoteEntity::getFtenantId, tenant).eq(SalesQuoteEntity::getFid, id).last("LIMIT 1"));
        if (q == null) throw new BizException("销售报价不存在或无权访问");
        return q;
    }

    private SalesContractEntity requireContract(Long id, String tenantId, boolean lock) {
        String tenant = tenant(tenantId);
        SalesContractEntity c = lock ? contracts.selectByIdForUpdate(id, tenant)
                : contracts.selectOne(new LambdaQueryWrapper<SalesContractEntity>()
                        .eq(SalesContractEntity::getFtenantId, tenant).eq(SalesContractEntity::getFid, id).last("LIMIT 1"));
        if (c == null) throw new BizException("销售合同不存在或无权访问");
        return c;
    }

    private String tenant(String value) {
        if (!StringUtils.hasText(value)) throw new BizException("tenantId 不能为空");
        return value.trim();
    }
    private String next(String actual, String required, String target) {
        if (!required.equals(actual)) throw new BizException("当前状态 " + actual + " 不允许此操作，应为 " + required);
        return target;
    }
    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private long version(Integer v) { return v == null ? 0 : v.longValue(); }
    private void one(int affected, String label) {
        if (affected != 1) throw new BizException(label + "写入失败或版本冲突");
    }
    private String blankToNull(String s) { return StringUtils.hasText(s) ? s.trim() : null; }
    private String number(String prefix, LocalDate d, Long id) {
        String raw = String.valueOf(id);
        return prefix + d.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + raw.substring(Math.max(0, raw.length()-8));
    }
}
