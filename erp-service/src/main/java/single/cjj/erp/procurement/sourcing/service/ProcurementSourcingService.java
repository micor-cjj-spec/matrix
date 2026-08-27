package single.cjj.erp.procurement.sourcing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.procurement.request.entity.PurchaseRequestEntity;
import single.cjj.erp.procurement.request.entity.PurchaseRequestEntryEntity;
import single.cjj.erp.procurement.request.mapper.PurchaseRequestEntryMapper;
import single.cjj.erp.procurement.request.mapper.PurchaseRequestMapper;
import single.cjj.erp.procurement.sourcing.dto.SourcingContracts.*;
import single.cjj.erp.procurement.sourcing.entity.*;
import single.cjj.erp.procurement.sourcing.mapper.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ProcurementSourcingService {

    private static final String RFQ_DRAFT = "DRAFT";
    private static final String RFQ_PUBLISHED = "PUBLISHED";
    private static final String RFQ_CLOSED = "CLOSED";
    private static final String RFQ_CANCELLED = "CANCELLED";
    private static final String QUOTE_DRAFT = "DRAFT";
    private static final String QUOTE_SUBMITTED = "SUBMITTED";
    private static final String REQUEST_EFFECTIVE = "EFFECTIVE";
    private static final String REQUEST_APPROVED = "APPROVED";
    private static final String EXECUTION_NONE = "NONE";
    private static final String EXECUTION_SOURCING = "SOURCING";
    private static final String EXECUTION_CONTRACTING = "CONTRACTING";
    private static final String EVENT_AWARDED = "PURCHASE_SOURCING_AWARDED";

    private final ProcurementRfqMapper rfqMapper;
    private final ProcurementRfqEntryMapper rfqEntryMapper;
    private final ProcurementRfqSupplierMapper rfqSupplierMapper;
    private final SupplierQuoteMapper quoteMapper;
    private final SupplierQuoteEntryMapper quoteEntryMapper;
    private final SourcingAwardMapper awardMapper;
    private final SourcingAwardEntryMapper awardEntryMapper;
    private final PurchaseRequestMapper requestMapper;
    private final PurchaseRequestEntryMapper requestEntryMapper;
    private final BusinessEventOutboxService outboxService;

    public ProcurementSourcingService(
            ProcurementRfqMapper rfqMapper,
            ProcurementRfqEntryMapper rfqEntryMapper,
            ProcurementRfqSupplierMapper rfqSupplierMapper,
            SupplierQuoteMapper quoteMapper,
            SupplierQuoteEntryMapper quoteEntryMapper,
            SourcingAwardMapper awardMapper,
            SourcingAwardEntryMapper awardEntryMapper,
            PurchaseRequestMapper requestMapper,
            PurchaseRequestEntryMapper requestEntryMapper,
            BusinessEventOutboxService outboxService
    ) {
        this.rfqMapper = rfqMapper;
        this.rfqEntryMapper = rfqEntryMapper;
        this.rfqSupplierMapper = rfqSupplierMapper;
        this.quoteMapper = quoteMapper;
        this.quoteEntryMapper = quoteEntryMapper;
        this.awardMapper = awardMapper;
        this.awardEntryMapper = awardEntryMapper;
        this.requestMapper = requestMapper;
        this.requestEntryMapper = requestEntryMapper;
        this.outboxService = outboxService;
    }

    public RfqDetail rfqDetail(Long fid, String tenantId) {
        ProcurementRfqEntity rfq = requireRfq(fid, tenantId, false);
        return new RfqDetail(rfq, listRfqEntries(fid), listRfqSuppliers(fid));
    }

    public IPage<ProcurementRfqEntity> pageRfqs(
            String tenantId, Long orgId, String status, String number, int page, int size
    ) {
        String tenant = requireTenant(tenantId);
        return rfqMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.max(size, 1)),
                new LambdaQueryWrapper<ProcurementRfqEntity>()
                        .eq(ProcurementRfqEntity::getFtenantId, tenant)
                        .eq(orgId != null, ProcurementRfqEntity::getForgId, orgId)
                        .eq(StringUtils.hasText(status), ProcurementRfqEntity::getFstatus, status)
                        .like(StringUtils.hasText(number), ProcurementRfqEntity::getFnumber, number)
                        .orderByDesc(ProcurementRfqEntity::getFdate)
                        .orderByDesc(ProcurementRfqEntity::getFid)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqDetail createRfq(RfqCreateRequest request, Long operatorId) {
        String tenant = requireTenant(request.ftenantId());
        String currency = request.fcurrencyCode().trim();
        LocalDate date = request.fdate() == null ? LocalDate.now() : request.fdate();
        Long rfqId = IdWorker.getId();
        String number = StringUtils.hasText(request.fnumber())
                ? request.fnumber().trim() : buildNumber("RFQ", date, rfqId);
        ensureRfqNumberUnique(tenant, number);

        List<RfqEntryRequest> requestedEntries = new ArrayList<>(request.entries());
        requestedEntries.sort(Comparator.comparing(RfqEntryRequest::fpurchaseRequestEntryId));
        Set<Long> seenRequestEntries = new LinkedHashSet<>();
        Map<Long, PurchaseRequestEntity> lockedRequests = new LinkedHashMap<>();
        List<ProcurementRfqEntryEntity> entries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < requestedEntries.size(); i++) {
            RfqEntryRequest item = requestedEntries.get(i);
            if (!seenRequestEntries.add(item.fpurchaseRequestEntryId())) {
                throw new BizException("同一采购申请分录不能在同一询价单重复出现: " + item.fpurchaseRequestEntryId());
            }
            PurchaseRequestEntryEntity requestEntry =
                    requestEntryMapper.selectByIdForUpdate(item.fpurchaseRequestEntryId(), tenant);
            if (requestEntry == null || !item.fpurchaseRequestId().equals(requestEntry.getFpurchaseRequestId())) {
                throw new BizException("采购申请分录不存在或归属不匹配: " + item.fpurchaseRequestEntryId());
            }
            PurchaseRequestEntity purchaseRequest = lockedRequests.get(item.fpurchaseRequestId());
            if (purchaseRequest == null) {
                purchaseRequest = requestMapper.selectByIdForUpdate(item.fpurchaseRequestId(), tenant);
                lockedRequests.put(item.fpurchaseRequestId(), purchaseRequest);
            }
            validateRequestForSourcing(purchaseRequest, requestEntry, request.forgId(), currency);

            BigDecimal available = nz(requestEntry.getFquantity()).subtract(nz(requestEntry.getFsourcedQuantity()));
            if (item.fquantity().compareTo(available) > 0) {
                throw new BizException("询价数量超过采购申请未寻源数量: requestEntry="
                        + item.fpurchaseRequestEntryId() + ", available=" + available);
            }

            ProcurementRfqEntryEntity entry = new ProcurementRfqEntryEntity();
            entry.setFid(IdWorker.getId());
            entry.setFtenantId(tenant);
            entry.setForgId(request.forgId());
            entry.setFrfqId(rfqId);
            entry.setFlineNo(i + 1);
            entry.setFpurchaseRequestId(item.fpurchaseRequestId());
            entry.setFpurchaseRequestEntryId(item.fpurchaseRequestEntryId());
            entry.setFmaterialId(requestEntry.getFmaterialId());
            entry.setFmaterialCode(requestEntry.getFmaterialCode());
            entry.setFmaterialName(requestEntry.getFmaterialName());
            entry.setFspecification(requestEntry.getFspecification());
            entry.setFunitId(requestEntry.getFunitId());
            entry.setFquantity(item.fquantity());
            entry.setFawardedQuantity(BigDecimal.ZERO);
            entry.setFrequiredDate(requestEntry.getFrequiredDate());
            entry.setFprojectId(requestEntry.getFprojectId());
            entry.setFcostCenterId(requestEntry.getFcostCenterId());
            init(entry, operatorId, now);
            entries.add(entry);
        }

        List<ProcurementRfqSupplierEntity> suppliers =
                buildSuppliers(rfqId, tenant, request.forgId(), request.suppliers(), operatorId, now);

        ProcurementRfqEntity rfq = new ProcurementRfqEntity();
        rfq.setFid(rfqId);
        rfq.setFtenantId(tenant);
        rfq.setForgId(request.forgId());
        rfq.setFnumber(number);
        rfq.setFdate(date);
        rfq.setFtitle(trimToNull(request.ftitle()));
        rfq.setFcurrencyCode(currency);
        rfq.setFquotationDeadline(request.fquotationDeadline());
        rfq.setFstatus(RFQ_DRAFT);
        rfq.setFremark(trimToNull(request.fremark()));
        init(rfq, operatorId, now);

        requireOne(rfqMapper.insert(rfq), "询价单");
        for (ProcurementRfqEntryEntity entry : entries) requireOne(rfqEntryMapper.insert(entry), "询价分录");
        for (ProcurementRfqSupplierEntity supplier : suppliers) requireOne(rfqSupplierMapper.insert(supplier), "询价供应商");
        return new RfqDetail(rfq, List.copyOf(entries), List.copyOf(suppliers));
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqDetail publishRfq(Long fid, String tenantId, Long operatorId) {
        ProcurementRfqEntity rfq = requireRfq(fid, tenantId, true);
        if (!RFQ_DRAFT.equals(rfq.getFstatus())) throw new BizException("只有草稿询价单允许发布");
        List<ProcurementRfqEntryEntity> entries = listRfqEntries(fid);
        if (entries.isEmpty() || listRfqSuppliers(fid).isEmpty()) {
            throw new BizException("询价单发布前必须存在询价分录和邀请供应商");
        }
        if (rfq.getFquotationDeadline() != null && rfq.getFquotationDeadline().isBefore(LocalDateTime.now())) {
            throw new BizException("报价截止时间不能早于发布时间");
        }

        List<Long> requestIds = entries.stream().map(ProcurementRfqEntryEntity::getFpurchaseRequestId)
                .distinct().sorted().toList();
        for (Long requestId : requestIds) {
            PurchaseRequestEntity request = requestMapper.selectByIdForUpdate(requestId, rfq.getFtenantId());
            if (request == null || !REQUEST_EFFECTIVE.equals(request.getFstatus())
                    || !REQUEST_APPROVED.equals(request.getFapprovalStatus())) {
                throw new BizException("采购申请已不满足寻源条件: " + requestId);
            }
            if (!(EXECUTION_NONE.equals(request.getFexecutionStatus())
                    || EXECUTION_SOURCING.equals(request.getFexecutionStatus()))) {
                throw new BizException("采购申请当前执行状态不允许进入询价: " + requestId + "/" + request.getFexecutionStatus());
            }
            if (EXECUTION_NONE.equals(request.getFexecutionStatus())) {
                request.setFexecutionStatus(EXECUTION_SOURCING);
                touch(request, operatorId);
                requireOne(requestMapper.updateById(request), "采购申请");
            }
        }

        rfq.setFstatus(RFQ_PUBLISHED);
        rfq.setFpublishedTime(LocalDateTime.now());
        touch(rfq, operatorId);
        requireOne(rfqMapper.updateById(rfq), "询价单");
        return new RfqDetail(rfq, entries, listRfqSuppliers(fid));
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqDetail cancelDraftRfq(Long fid, String tenantId, Long operatorId) {
        ProcurementRfqEntity rfq = requireRfq(fid, tenantId, true);
        if (RFQ_CANCELLED.equals(rfq.getFstatus())) return rfqDetail(fid, tenantId);
        if (!RFQ_DRAFT.equals(rfq.getFstatus())) throw new BizException("v1 仅允许取消未发布的草稿询价单");
        rfq.setFstatus(RFQ_CANCELLED);
        touch(rfq, operatorId);
        requireOne(rfqMapper.updateById(rfq), "询价单");
        return new RfqDetail(rfq, listRfqEntries(fid), listRfqSuppliers(fid));
    }

    @Transactional(rollbackFor = Exception.class)
    public QuoteDetail createQuote(Long rfqId, QuoteCreateRequest request, Long operatorId) {
        String tenant = requireTenant(request.ftenantId());
        ProcurementRfqEntity rfq = requireRfq(rfqId, tenant, true);
        if (!RFQ_PUBLISHED.equals(rfq.getFstatus())) throw new BizException("只有已发布询价单允许录入供应商报价");

        ProcurementRfqSupplierEntity invitation = rfqSupplierMapper.selectOne(
                new LambdaQueryWrapper<ProcurementRfqSupplierEntity>()
                        .eq(ProcurementRfqSupplierEntity::getFtenantId, tenant)
                        .eq(ProcurementRfqSupplierEntity::getFrfqId, rfqId)
                        .eq(ProcurementRfqSupplierEntity::getFbusinessPartnerId, request.fbusinessPartnerId())
                        .last("limit 1"));
        if (invitation == null) throw new BizException("供应商不在当前询价邀请范围: " + request.fbusinessPartnerId());

        Long existing = quoteMapper.selectCount(new LambdaQueryWrapper<SupplierQuoteEntity>()
                .eq(SupplierQuoteEntity::getFrfqId, rfqId)
                .eq(SupplierQuoteEntity::getFbusinessPartnerId, request.fbusinessPartnerId()));
        if (existing != null && existing > 0) throw new BizException("同一供应商在同一询价单仅允许一份有效报价");

        Long quoteId = IdWorker.getId();
        LocalDate quoteDate = request.fquoteDate() == null ? LocalDate.now() : request.fquoteDate();
        String quoteNo = StringUtils.hasText(request.fquoteNo())
                ? request.fquoteNo().trim() : buildNumber("QT", quoteDate, quoteId);
        ensureQuoteNumberUnique(tenant, quoteNo);

        Set<Long> seen = new LinkedHashSet<>();
        List<SupplierQuoteEntryEntity> entries = new ArrayList<>();
        BigDecimal netTotal = BigDecimal.ZERO, taxTotal = BigDecimal.ZERO, grossTotal = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < request.entries().size(); i++) {
            QuoteEntryRequest item = request.entries().get(i);
            if (!seen.add(item.frfqEntryId())) throw new BizException("同一报价不能重复报价同一询价分录: " + item.frfqEntryId());
            ProcurementRfqEntryEntity rfqEntry = rfqEntryMapper.selectOne(
                    new LambdaQueryWrapper<ProcurementRfqEntryEntity>()
                            .eq(ProcurementRfqEntryEntity::getFid, item.frfqEntryId())
                            .eq(ProcurementRfqEntryEntity::getFrfqId, rfqId)
                            .eq(ProcurementRfqEntryEntity::getFtenantId, tenant).last("limit 1"));
            if (rfqEntry == null) throw new BizException("询价分录不存在: " + item.frfqEntryId());
            if (item.fquantity().compareTo(rfqEntry.getFquantity()) > 0) throw new BizException("报价数量不能超过询价数量: " + item.frfqEntryId());

            BigDecimal taxRate = item.ftaxRate() == null ? BigDecimal.ZERO : item.ftaxRate();
            BigDecimal net = money(item.fquantity().multiply(item.funitPrice()));
            BigDecimal tax = money(net.multiply(taxRate));
            BigDecimal gross = money(net.add(tax));

            SupplierQuoteEntryEntity entry = new SupplierQuoteEntryEntity();
            entry.setFid(IdWorker.getId()); entry.setFtenantId(tenant); entry.setForgId(rfq.getForgId());
            entry.setFquoteId(quoteId); entry.setFrfqEntryId(item.frfqEntryId()); entry.setFlineNo(i + 1);
            entry.setFquantity(item.fquantity()); entry.setFawardedQuantity(BigDecimal.ZERO); entry.setFunitPrice(item.funitPrice());
            entry.setFtaxRate(taxRate); entry.setFnetAmount(net); entry.setFtaxAmount(tax); entry.setFgrossAmount(gross);
            entry.setFdeliveryDate(item.fdeliveryDate()); entry.setFremark(trimToNull(item.fremark())); init(entry, operatorId, now);
            entries.add(entry); netTotal = netTotal.add(net); taxTotal = taxTotal.add(tax); grossTotal = grossTotal.add(gross);
        }

        SupplierQuoteEntity quote = new SupplierQuoteEntity();
        quote.setFid(quoteId); quote.setFtenantId(tenant); quote.setForgId(rfq.getForgId()); quote.setFrfqId(rfqId);
        quote.setFbusinessPartnerId(invitation.getFbusinessPartnerId()); quote.setFbusinessPartnerCode(invitation.getFbusinessPartnerCode());
        quote.setFbusinessPartnerName(invitation.getFbusinessPartnerName()); quote.setFquoteNo(quoteNo); quote.setFquoteDate(quoteDate);
        quote.setFvalidUntil(request.fvalidUntil()); quote.setFcurrencyCode(rfq.getFcurrencyCode()); quote.setFdeliveryDays(request.fdeliveryDays());
        quote.setFpaymentTerms(trimToNull(request.fpaymentTerms())); quote.setFnetAmount(money(netTotal)); quote.setFtaxAmount(money(taxTotal));
        quote.setFgrossAmount(money(grossTotal)); quote.setFstatus(QUOTE_DRAFT); quote.setFremark(trimToNull(request.fremark())); init(quote, operatorId, now);
        requireOne(quoteMapper.insert(quote), "供应商报价");
        for (SupplierQuoteEntryEntity entry : entries) requireOne(quoteEntryMapper.insert(entry), "供应商报价分录");
        return new QuoteDetail(quote, List.copyOf(entries));
    }

    @Transactional(rollbackFor = Exception.class)
    public QuoteDetail submitQuote(Long rfqId, Long quoteId, String tenantId, Long operatorId) {
        ProcurementRfqEntity rfq = requireRfq(rfqId, tenantId, true);
        if (!RFQ_PUBLISHED.equals(rfq.getFstatus())) throw new BizException("询价单非已发布状态，不能提交报价");
        if (rfq.getFquotationDeadline() != null && LocalDateTime.now().isAfter(rfq.getFquotationDeadline())) {
            throw new BizException("已超过报价截止时间");
        }
        SupplierQuoteEntity quote = quoteMapper.selectByIdForUpdate(quoteId, rfq.getFtenantId());
        if (quote == null || !rfqId.equals(quote.getFrfqId())) throw new BizException("供应商报价不存在: " + quoteId);
        if (!QUOTE_DRAFT.equals(quote.getFstatus())) throw new BizException("只有草稿报价允许提交");
        List<SupplierQuoteEntryEntity> entries = listQuoteEntries(quoteId);
        if (entries.isEmpty()) throw new BizException("供应商报价至少需要一条分录");
        quote.setFstatus(QUOTE_SUBMITTED); quote.setFsubmittedTime(LocalDateTime.now()); touch(quote, operatorId);
        requireOne(quoteMapper.updateById(quote), "供应商报价");
        return new QuoteDetail(quote, entries);
    }

    public List<SupplierQuoteEntity> listQuotes(Long rfqId, String tenantId) {
        ProcurementRfqEntity rfq = requireRfq(rfqId, tenantId, false);
        return quoteMapper.selectList(new LambdaQueryWrapper<SupplierQuoteEntity>()
                .eq(SupplierQuoteEntity::getFtenantId, rfq.getFtenantId())
                .eq(SupplierQuoteEntity::getFrfqId, rfqId)
                .orderByAsc(SupplierQuoteEntity::getFbusinessPartnerId).orderByAsc(SupplierQuoteEntity::getFid));
    }

    public List<ComparisonLine> comparison(Long rfqId, String tenantId) {
        ProcurementRfqEntity rfq = requireRfq(rfqId, tenantId, false);
        List<SupplierQuoteEntity> quotes = quoteMapper.selectList(new LambdaQueryWrapper<SupplierQuoteEntity>()
                .eq(SupplierQuoteEntity::getFtenantId, rfq.getFtenantId())
                .eq(SupplierQuoteEntity::getFrfqId, rfqId).eq(SupplierQuoteEntity::getFstatus, QUOTE_SUBMITTED));
        if (quotes.isEmpty()) return List.of();

        Map<Long, SupplierQuoteEntity> quoteById = new HashMap<>();
        for (SupplierQuoteEntity quote : quotes) quoteById.put(quote.getFid(), quote);
        List<SupplierQuoteEntryEntity> entries = quoteEntryMapper.selectList(
                new LambdaQueryWrapper<SupplierQuoteEntryEntity>().in(SupplierQuoteEntryEntity::getFquoteId, quoteById.keySet())
                        .orderByAsc(SupplierQuoteEntryEntity::getFrfqEntryId).orderByAsc(SupplierQuoteEntryEntity::getFgrossAmount));
        Map<Long, BigDecimal> lowest = new HashMap<>();
        for (SupplierQuoteEntryEntity entry : entries) {
            lowest.merge(entry.getFrfqEntryId(), grossUnitPrice(entry.getFunitPrice(), entry.getFtaxRate()), BigDecimal::min);
        }

        List<ComparisonLine> result = new ArrayList<>();
        for (SupplierQuoteEntryEntity entry : entries) {
            SupplierQuoteEntity quote = quoteById.get(entry.getFquoteId());
            BigDecimal gpu = grossUnitPrice(entry.getFunitPrice(), entry.getFtaxRate());
            result.add(new ComparisonLine(entry.getFrfqEntryId(), quote.getFid(), entry.getFid(),
                    quote.getFbusinessPartnerId(), quote.getFbusinessPartnerCode(), quote.getFbusinessPartnerName(),
                    entry.getFquantity(), entry.getFunitPrice(), entry.getFtaxRate(), gpu, entry.getFgrossAmount(),
                    entry.getFdeliveryDate(), gpu.compareTo(lowest.get(entry.getFrfqEntryId())) == 0));
        }
        return List.copyOf(result);
    }

    @Transactional(rollbackFor = Exception.class)
    public AwardDetail confirmAward(Long rfqId, AwardCreateRequest request, Long operatorId) {
        String tenant = requireTenant(request.ftenantId());
        ProcurementRfqEntity rfq = requireRfq(rfqId, tenant, true);
        if (!RFQ_PUBLISHED.equals(rfq.getFstatus())) throw new BizException("只有已发布询价单允许定标");

        Long awardId = IdWorker.getId();
        LocalDate date = request.fdate() == null ? LocalDate.now() : request.fdate();
        String number = StringUtils.hasText(request.fnumber())
                ? request.fnumber().trim() : buildNumber("AWD", date, awardId);
        ensureAwardNumberUnique(tenant, number);

        List<AwardEntryRequest> requested = new ArrayList<>(request.entries());
        requested.sort(Comparator.comparing(AwardEntryRequest::frfqEntryId).thenComparing(AwardEntryRequest::fquoteEntryId));
        Set<Long> seenQuoteEntries = new LinkedHashSet<>();
        Set<Long> affectedRequestIds = new LinkedHashSet<>();
        List<SourcingAwardEntryEntity> awardEntries = new ArrayList<>();
        BigDecimal grossTotal = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < requested.size(); i++) {
            AwardEntryRequest item = requested.get(i);
            if (!seenQuoteEntries.add(item.fquoteEntryId())) throw new BizException("同一定标单不能重复使用同一报价分录: " + item.fquoteEntryId());

            ProcurementRfqEntryEntity rfqEntry = rfqEntryMapper.selectByIdForUpdate(item.frfqEntryId(), tenant);
            if (rfqEntry == null || !rfqId.equals(rfqEntry.getFrfqId())) throw new BizException("询价分录不存在或归属不匹配: " + item.frfqEntryId());
            SupplierQuoteEntity quote = quoteMapper.selectByIdForUpdate(item.fquoteId(), tenant);
            if (quote == null || !rfqId.equals(quote.getFrfqId()) || !QUOTE_SUBMITTED.equals(quote.getFstatus())) {
                throw new BizException("定标只能引用当前询价的已提交报价: " + item.fquoteId());
            }
            SupplierQuoteEntryEntity quoteEntry = quoteEntryMapper.selectByIdForUpdate(item.fquoteEntryId(), tenant);
            if (quoteEntry == null || !item.fquoteId().equals(quoteEntry.getFquoteId())
                    || !item.frfqEntryId().equals(quoteEntry.getFrfqEntryId())) {
                throw new BizException("报价分录与询价/报价归属不匹配: " + item.fquoteEntryId());
            }

            BigDecimal qty = item.fawardedQuantity();
            BigDecimal rfqAvailable = nz(rfqEntry.getFquantity()).subtract(nz(rfqEntry.getFawardedQuantity()));
            BigDecimal quoteAvailable = nz(quoteEntry.getFquantity()).subtract(nz(quoteEntry.getFawardedQuantity()));
            if (qty.compareTo(rfqAvailable) > 0 || qty.compareTo(quoteAvailable) > 0) {
                throw new BizException("定标数量超过询价或报价剩余数量: rfqEntry=" + item.frfqEntryId());
            }

            PurchaseRequestEntryEntity requestEntry =
                    requestEntryMapper.selectByIdForUpdate(rfqEntry.getFpurchaseRequestEntryId(), tenant);
            if (requestEntry == null || !rfqEntry.getFpurchaseRequestId().equals(requestEntry.getFpurchaseRequestId())) {
                throw new BizException("采购申请分录不存在: " + rfqEntry.getFpurchaseRequestEntryId());
            }
            BigDecimal requestAvailable = nz(requestEntry.getFquantity()).subtract(nz(requestEntry.getFsourcedQuantity()));
            if (qty.compareTo(requestAvailable) > 0) throw new BizException("定标数量超过采购申请剩余寻源数量: requestEntry=" + requestEntry.getFid());

            BigDecimal net = money(qty.multiply(quoteEntry.getFunitPrice()));
            BigDecimal tax = money(net.multiply(nz(quoteEntry.getFtaxRate())));
            BigDecimal gross = money(net.add(tax));

            SourcingAwardEntryEntity awardEntry = new SourcingAwardEntryEntity();
            awardEntry.setFid(IdWorker.getId()); awardEntry.setFtenantId(tenant); awardEntry.setForgId(rfq.getForgId());
            awardEntry.setFawardId(awardId); awardEntry.setFlineNo(i + 1); awardEntry.setFrfqEntryId(rfqEntry.getFid());
            awardEntry.setFquoteId(quote.getFid()); awardEntry.setFquoteEntryId(quoteEntry.getFid());
            awardEntry.setFbusinessPartnerId(quote.getFbusinessPartnerId()); awardEntry.setFbusinessPartnerCode(quote.getFbusinessPartnerCode());
            awardEntry.setFbusinessPartnerName(quote.getFbusinessPartnerName()); awardEntry.setFawardedQuantity(qty);
            awardEntry.setFunitPrice(quoteEntry.getFunitPrice()); awardEntry.setFtaxRate(quoteEntry.getFtaxRate());
            awardEntry.setFnetAmount(net); awardEntry.setFtaxAmount(tax); awardEntry.setFgrossAmount(gross);
            awardEntry.setFreason(trimToNull(item.freason())); init(awardEntry, operatorId, now);
            awardEntries.add(awardEntry); grossTotal = grossTotal.add(gross);

            rfqEntry.setFawardedQuantity(nz(rfqEntry.getFawardedQuantity()).add(qty)); touch(rfqEntry, operatorId);
            requireOne(rfqEntryMapper.updateById(rfqEntry), "询价分录");
            quoteEntry.setFawardedQuantity(nz(quoteEntry.getFawardedQuantity()).add(qty)); touch(quoteEntry, operatorId);
            requireOne(quoteEntryMapper.updateById(quoteEntry), "报价分录");
            requestEntry.setFsourcedQuantity(nz(requestEntry.getFsourcedQuantity()).add(qty)); touch(requestEntry, operatorId);
            requireOne(requestEntryMapper.updateById(requestEntry), "采购申请分录");
            affectedRequestIds.add(requestEntry.getFpurchaseRequestId());
        }

        SourcingAwardEntity award = new SourcingAwardEntity();
        award.setFid(awardId); award.setFtenantId(tenant); award.setForgId(rfq.getForgId()); award.setFrfqId(rfqId);
        award.setFnumber(number); award.setFdate(date); award.setFgrossAmount(money(grossTotal)); award.setFstatus("CONFIRMED");
        award.setFremark(trimToNull(request.fremark())); init(award, operatorId, now);
        requireOne(awardMapper.insert(award), "采购定标");
        for (SourcingAwardEntryEntity entry : awardEntries) requireOne(awardEntryMapper.insert(entry), "采购定标分录");

        for (Long requestId : affectedRequestIds.stream().sorted().toList()) {
            refreshRequestExecution(requestId, tenant, operatorId);
        }
        refreshRfqCloseStatus(rfq, operatorId);

        outboxService.append(tenant, rfq.getForgId(), EVENT_AWARDED, "SOURCING_AWARD", awardId,
                award.getFversion() == null ? 0L : award.getFversion().longValue(), "ERP_SOURCING_AWARD",
                number, date, operatorId, awardPayload(rfq, award, awardEntries));
        return new AwardDetail(award, List.copyOf(awardEntries));
    }

    private void refreshRequestExecution(Long requestId, String tenantId, Long operatorId) {
        PurchaseRequestEntity request = requestMapper.selectByIdForUpdate(requestId, tenantId);
        if (request == null) throw new BizException("采购申请不存在: " + requestId);
        List<PurchaseRequestEntryEntity> entries = requestEntryMapper.selectList(
                new LambdaQueryWrapper<PurchaseRequestEntryEntity>()
                        .eq(PurchaseRequestEntryEntity::getFpurchaseRequestId, requestId)
                        .orderByAsc(PurchaseRequestEntryEntity::getFlineNo));
        boolean allSourced = !entries.isEmpty() && entries.stream().allMatch(
                item -> nz(item.getFsourcedQuantity()).compareTo(nz(item.getFquantity())) >= 0);
        request.setFexecutionStatus(allSourced ? EXECUTION_CONTRACTING : EXECUTION_SOURCING);
        touch(request, operatorId);
        requireOne(requestMapper.updateById(request), "采购申请");
    }

    private void refreshRfqCloseStatus(ProcurementRfqEntity rfq, Long operatorId) {
        List<ProcurementRfqEntryEntity> entries = listRfqEntries(rfq.getFid());
        boolean allAwarded = !entries.isEmpty() && entries.stream().allMatch(
                item -> nz(item.getFawardedQuantity()).compareTo(nz(item.getFquantity())) >= 0);
        if (allAwarded) {
            rfq.setFstatus(RFQ_CLOSED); rfq.setFclosedTime(LocalDateTime.now()); touch(rfq, operatorId);
            requireOne(rfqMapper.updateById(rfq), "询价单");
        }
    }

    private void validateRequestForSourcing(
            PurchaseRequestEntity request, PurchaseRequestEntryEntity entry, Long orgId, String currencyCode
    ) {
        if (request == null) throw new BizException("采购申请不存在: " + entry.getFpurchaseRequestId());
        if (!orgId.equals(request.getForgId()) || !orgId.equals(entry.getForgId())) {
            throw new BizException("询价组织与采购申请组织不一致: " + request.getFid());
        }
        if (!REQUEST_EFFECTIVE.equals(request.getFstatus()) || !REQUEST_APPROVED.equals(request.getFapprovalStatus())) {
            throw new BizException("只有已审批生效的采购申请允许寻源: " + request.getFid());
        }
        if (!(EXECUTION_NONE.equals(request.getFexecutionStatus()) || EXECUTION_SOURCING.equals(request.getFexecutionStatus()))) {
            throw new BizException("采购申请当前执行状态不允许新增询价: " + request.getFid() + "/" + request.getFexecutionStatus());
        }
        if (!currencyCode.equals(request.getFcurrencyCode())) throw new BizException("询价币种与采购申请币种不一致: " + request.getFid());
    }

    private List<ProcurementRfqSupplierEntity> buildSuppliers(
            Long rfqId, String tenantId, Long orgId, List<RfqSupplierRequest> requests, Long operatorId, LocalDateTime now
    ) {
        Set<Long> seen = new LinkedHashSet<>();
        List<ProcurementRfqSupplierEntity> result = new ArrayList<>();
        for (RfqSupplierRequest item : requests) {
            if (!seen.add(item.fbusinessPartnerId())) throw new BizException("询价供应商重复: " + item.fbusinessPartnerId());
            ProcurementRfqSupplierEntity supplier = new ProcurementRfqSupplierEntity();
            supplier.setFid(IdWorker.getId()); supplier.setFtenantId(tenantId); supplier.setForgId(orgId); supplier.setFrfqId(rfqId);
            supplier.setFbusinessPartnerId(item.fbusinessPartnerId()); supplier.setFbusinessPartnerCode(item.fbusinessPartnerCode().trim());
            supplier.setFbusinessPartnerName(item.fbusinessPartnerName().trim()); supplier.setFstatus("INVITED"); init(supplier, operatorId, now);
            result.add(supplier);
        }
        return result;
    }

    private ProcurementRfqEntity requireRfq(Long fid, String tenantId, boolean forUpdate) {
        String tenant = requireTenant(tenantId);
        ProcurementRfqEntity rfq = forUpdate ? rfqMapper.selectByIdForUpdate(fid, tenant)
                : rfqMapper.selectOne(new LambdaQueryWrapper<ProcurementRfqEntity>()
                        .eq(ProcurementRfqEntity::getFid, fid).eq(ProcurementRfqEntity::getFtenantId, tenant).last("limit 1"));
        if (rfq == null) throw new BizException("采购询价单不存在: " + fid);
        return rfq;
    }

    private List<ProcurementRfqEntryEntity> listRfqEntries(Long rfqId) {
        return rfqEntryMapper.selectList(new LambdaQueryWrapper<ProcurementRfqEntryEntity>()
                .eq(ProcurementRfqEntryEntity::getFrfqId, rfqId).orderByAsc(ProcurementRfqEntryEntity::getFlineNo));
    }
    private List<ProcurementRfqSupplierEntity> listRfqSuppliers(Long rfqId) {
        return rfqSupplierMapper.selectList(new LambdaQueryWrapper<ProcurementRfqSupplierEntity>()
                .eq(ProcurementRfqSupplierEntity::getFrfqId, rfqId).orderByAsc(ProcurementRfqSupplierEntity::getFbusinessPartnerId));
    }
    private List<SupplierQuoteEntryEntity> listQuoteEntries(Long quoteId) {
        return quoteEntryMapper.selectList(new LambdaQueryWrapper<SupplierQuoteEntryEntity>()
                .eq(SupplierQuoteEntryEntity::getFquoteId, quoteId).orderByAsc(SupplierQuoteEntryEntity::getFlineNo));
    }

    private Map<String,Object> awardPayload(ProcurementRfqEntity rfq,SourcingAwardEntity award,List<SourcingAwardEntryEntity> entries){
        Map<String,Object> payload=new LinkedHashMap<>();
        payload.put("rfqId",rfq.getFid()); payload.put("rfqNo",rfq.getFnumber()); payload.put("awardId",award.getFid());
        payload.put("awardNo",award.getFnumber()); payload.put("currencyCode",rfq.getFcurrencyCode()); payload.put("grossAmount",award.getFgrossAmount());
        List<Map<String,Object>> lines=new ArrayList<>();
        for(SourcingAwardEntryEntity entry:entries){
            ProcurementRfqEntryEntity source=rfqEntryMapper.selectById(entry.getFrfqEntryId());
            Map<String,Object> line=new LinkedHashMap<>();
            line.put("awardEntryId",entry.getFid()); line.put("rfqEntryId",entry.getFrfqEntryId());
            line.put("purchaseRequestId",source==null?null:source.getFpurchaseRequestId());
            line.put("purchaseRequestEntryId",source==null?null:source.getFpurchaseRequestEntryId());
            line.put("businessPartnerId",entry.getFbusinessPartnerId()); line.put("businessPartnerCode",entry.getFbusinessPartnerCode());
            line.put("businessPartnerName",entry.getFbusinessPartnerName()); line.put("awardedQuantity",entry.getFawardedQuantity());
            line.put("unitPrice",entry.getFunitPrice()); line.put("taxRate",entry.getFtaxRate()); line.put("grossAmount",entry.getFgrossAmount());
            lines.add(line);
        }
        payload.put("entries",lines); return payload;
    }

    private void ensureRfqNumberUnique(String tenantId,String number){
        Long count=rfqMapper.selectCount(new LambdaQueryWrapper<ProcurementRfqEntity>().eq(ProcurementRfqEntity::getFtenantId,tenantId).eq(ProcurementRfqEntity::getFnumber,number));
        if(count!=null&&count>0) throw new BizException("询价单号已存在: "+number);
    }
    private void ensureQuoteNumberUnique(String tenantId,String number){
        Long count=quoteMapper.selectCount(new LambdaQueryWrapper<SupplierQuoteEntity>().eq(SupplierQuoteEntity::getFtenantId,tenantId).eq(SupplierQuoteEntity::getFquoteNo,number));
        if(count!=null&&count>0) throw new BizException("报价单号已存在: "+number);
    }
    private void ensureAwardNumberUnique(String tenantId,String number){
        Long count=awardMapper.selectCount(new LambdaQueryWrapper<SourcingAwardEntity>().eq(SourcingAwardEntity::getFtenantId,tenantId).eq(SourcingAwardEntity::getFnumber,number));
        if(count!=null&&count>0) throw new BizException("定标单号已存在: "+number);
    }
    private String buildNumber(String prefix,LocalDate date,Long id){
        String suffix=String.valueOf(id); suffix=suffix.substring(Math.max(0,suffix.length()-8));
        return prefix+date.format(DateTimeFormatter.BASIC_ISO_DATE)+"-"+suffix;
    }
    private String requireTenant(String tenantId){if(!StringUtils.hasText(tenantId))throw new BizException("tenantId 不能为空");return tenantId.trim();}
    private String trimToNull(String value){return StringUtils.hasText(value)?value.trim():null;}
    private BigDecimal nz(BigDecimal value){return value==null?BigDecimal.ZERO:value;}
    private BigDecimal money(BigDecimal value){return nz(value).setScale(2,RoundingMode.HALF_UP);}
    private BigDecimal grossUnitPrice(BigDecimal unitPrice,BigDecimal taxRate){
        return nz(unitPrice).multiply(BigDecimal.ONE.add(nz(taxRate))).setScale(6,RoundingMode.HALF_UP);
    }

    private void init(Object entity,Long operatorId,LocalDateTime now){
        touch(entity,operatorId);
        if(entity instanceof ProcurementRfqEntity v){v.setFcreateBy(operatorId);v.setFcreateTime(now);v.setFdeleteFlag(0);v.setFversion(0);}
        else if(entity instanceof ProcurementRfqEntryEntity v){v.setFcreateBy(operatorId);v.setFcreateTime(now);v.setFdeleteFlag(0);v.setFversion(0);}
        else if(entity instanceof ProcurementRfqSupplierEntity v){v.setFcreateBy(operatorId);v.setFcreateTime(now);v.setFdeleteFlag(0);v.setFversion(0);}
        else if(entity instanceof SupplierQuoteEntity v){v.setFcreateBy(operatorId);v.setFcreateTime(now);v.setFdeleteFlag(0);v.setFversion(0);}
        else if(entity instanceof SupplierQuoteEntryEntity v){v.setFcreateBy(operatorId);v.setFcreateTime(now);v.setFdeleteFlag(0);v.setFversion(0);}
        else if(entity instanceof SourcingAwardEntity v){v.setFcreateBy(operatorId);v.setFcreateTime(now);v.setFdeleteFlag(0);v.setFversion(0);}
        else if(entity instanceof SourcingAwardEntryEntity v){v.setFcreateBy(operatorId);v.setFcreateTime(now);v.setFdeleteFlag(0);v.setFversion(0);}
    }
    private void touch(Object entity,Long operatorId){
        LocalDateTime now=LocalDateTime.now();
        if(entity instanceof ProcurementRfqEntity v){v.setFmodifyBy(operatorId);v.setFmodifyTime(now);}
        else if(entity instanceof ProcurementRfqEntryEntity v){v.setFmodifyBy(operatorId);v.setFmodifyTime(now);}
        else if(entity instanceof ProcurementRfqSupplierEntity v){v.setFmodifyBy(operatorId);v.setFmodifyTime(now);}
        else if(entity instanceof SupplierQuoteEntity v){v.setFmodifyBy(operatorId);v.setFmodifyTime(now);}
        else if(entity instanceof SupplierQuoteEntryEntity v){v.setFmodifyBy(operatorId);v.setFmodifyTime(now);}
        else if(entity instanceof SourcingAwardEntity v){v.setFmodifyBy(operatorId);v.setFmodifyTime(now);}
        else if(entity instanceof SourcingAwardEntryEntity v){v.setFmodifyBy(operatorId);v.setFmodifyTime(now);}
        else if(entity instanceof PurchaseRequestEntity v){v.setFmodifyBy(operatorId);v.setFmodifyTime(now);}
        else if(entity instanceof PurchaseRequestEntryEntity v){v.setFmodifyBy(operatorId);v.setFmodifyTime(now);}
    }
    private void requireOne(int updated,String objectName){
        if(updated!=1) throw new BizException(objectName+"已被其他请求修改，请刷新后重试");
    }
}
