package single.cjj.fi.gl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.entity.BizfiFiAccount;
import single.cjj.fi.gl.entity.BizfiFiGlEntry;
import single.cjj.fi.gl.entity.BizfiFiVoucherLine;
import single.cjj.fi.gl.mapper.BizfiFiAccountMapper;
import single.cjj.fi.gl.mapper.BizfiFiGlEntryMapper;
import single.cjj.fi.gl.mapper.BizfiFiVoucherLineMapper;
import single.cjj.fi.gl.report.entity.BizfiFiReportAccountMap;
import single.cjj.fi.gl.report.entity.BizfiFiReportItem;
import single.cjj.fi.gl.report.mapper.BizfiFiReportAccountMapMapper;
import single.cjj.fi.gl.report.mapper.BizfiFiReportItemMapper;
import single.cjj.fi.gl.service.BizfiFiDataHealthCheckService;
import single.cjj.fi.gl.vo.BizfiFiHealthCheckIssueVO;
import single.cjj.fi.gl.vo.BizfiFiHealthCheckResultVO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 财务基础数据健康检查
 */
@Service
public class BizfiFiDataHealthCheckServiceImpl implements BizfiFiDataHealthCheckService {

    @Autowired
    private BizfiFiAccountMapper accountMapper;

    @Autowired
    private BizfiFiVoucherLineMapper voucherLineMapper;

    @Autowired
    private BizfiFiGlEntryMapper glEntryMapper;

    @Autowired
    private BizfiFiReportAccountMapMapper reportAccountMapMapper;

    @Autowired
    private BizfiFiReportItemMapper reportItemMapper;

    @Override
    public BizfiFiHealthCheckResultVO check(Long forg, Long templateId, Integer sampleSize) {
        int limit = normalizeSampleSize(sampleSize);
        BizfiFiHealthCheckResultVO result = new BizfiFiHealthCheckResultVO();
        result.setForg(forg);
        result.setTemplateId(templateId);
        result.setCheckedAt(LocalDateTime.now());
        result.setNotes(new ArrayList<>());
        result.setIssues(new ArrayList<>());

        List<BizfiFiAccount> allAccounts = accountMapper.selectList(new LambdaQueryWrapper<BizfiFiAccount>()
                .orderByAsc(BizfiFiAccount::getFcode)
                .orderByAsc(BizfiFiAccount::getFid));
        List<BizfiFiAccount> scopedAccounts = allAccounts.stream()
                .filter(account -> forg == null || Objects.equals(forg, account.getForg()))
                .toList();
        Map<Long, BizfiFiAccount> accountById = allAccounts.stream()
                .filter(account -> account.getFid() != null)
                .collect(Collectors.toMap(BizfiFiAccount::getFid, Function.identity(), (left, right) -> left));
        Set<String> accountCodes = allAccounts.stream()
                .map(BizfiFiAccount::getFcode)
                .map(this::normalizeCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(TreeSet::new));

        List<BizfiFiReportAccountMap> reportMaps = reportAccountMapMapper.selectList(
                new LambdaQueryWrapper<BizfiFiReportAccountMap>()
                        .eq(templateId != null, BizfiFiReportAccountMap::getFtemplateId, templateId)
                        .orderByAsc(BizfiFiReportAccountMap::getFid));
        Set<Long> reportItemIds = reportItemMapper.selectList(
                        new LambdaQueryWrapper<BizfiFiReportItem>()
                                .eq(templateId != null, BizfiFiReportItem::getFtemplateId, templateId))
                .stream()
                .map(BizfiFiReportItem::getFid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        addIssueIfPresent(result.getIssues(),
                "MISSING_ACCOUNT_MASTER_VOUCHER_LINE",
                "凭证分录缺少科目主数据",
                "HIGH",
                missingCodes(loadDistinctCodes(voucherLineMapper, "faccount_code"), accountCodes),
                limit,
                "补录科目主数据，或修正凭证分录中的科目编码。");

        addIssueIfPresent(result.getIssues(),
                "MISSING_ACCOUNT_MASTER_GL_ENTRY",
                "总账分录缺少科目主数据",
                "HIGH",
                missingCodes(loadDistinctCodes(glEntryMapper, "faccount_code"), accountCodes),
                limit,
                "补录科目主数据，并评估是否需要回补历史分录来源。");

        Set<Long> mappedAccountIds = reportMaps.stream()
                .map(BizfiFiReportAccountMap::getFaccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<String> accountsWithoutMapping = scopedAccounts.stream()
                .filter(account -> account.getFid() != null)
                .filter(account -> account.getFreportItem() == null)
                .filter(account -> !mappedAccountIds.contains(account.getFid()))
                .map(this::formatAccount)
                .sorted()
                .toList();
        addIssueIfPresent(result.getIssues(),
                "ACCOUNT_WITHOUT_REPORT_MAPPING",
                "科目缺少报表映射",
                "MEDIUM",
                accountsWithoutMapping,
                limit,
                "为科目维护 freport_item 或新增 bizfi_fi_report_account_map 映射。");

        List<String> brokenAccountMappings = reportMaps.stream()
                .filter(map -> map.getFaccountId() == null || !accountById.containsKey(map.getFaccountId()))
                .map(map -> formatMapRef(map, "ACCOUNT"))
                .distinct()
                .sorted()
                .toList();
        addIssueIfPresent(result.getIssues(),
                "BROKEN_REPORT_MAPPING_ACCOUNT",
                "报表映射引用了不存在的科目",
                "HIGH",
                brokenAccountMappings,
                limit,
                "修复映射表中的 faccount_id，或补录缺失科目。");

        List<String> brokenItemMappings = reportMaps.stream()
                .filter(map -> map.getFitemId() == null || !reportItemIds.contains(map.getFitemId()))
                .map(map -> formatMapRef(map, "ITEM"))
                .distinct()
                .sorted()
                .toList();
        addIssueIfPresent(result.getIssues(),
                "BROKEN_REPORT_MAPPING_ITEM",
                "报表映射引用了不存在的报表项目",
                "HIGH",
                brokenItemMappings,
                limit,
                "修复映射表中的 fitem_id，或补录缺失报表项目。");

        if (forg != null) {
            result.getNotes().add("科目覆盖检查已按组织过滤: forg=" + forg
                    + "；凭证行和总账分录仍按全局科目主数据检查，因为现有明细表暂未带组织维度。");
        }
        if (templateId != null) {
            result.getNotes().add("报表映射检查已按模板过滤: templateId=" + templateId);
        }
        if (result.getIssues().isEmpty()) {
            result.getNotes().add("当前检查范围内未发现已建模的基础数据缺口。");
        }

        result.setIssueTypeCount(result.getIssues().size());
        result.setTotalIssueCount(result.getIssues().stream()
                .map(BizfiFiHealthCheckIssueVO::getCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum());
        result.setHealthy(result.getIssues().isEmpty());
        result.getIssues().sort(Comparator.comparing(BizfiFiHealthCheckIssueVO::getSeverity)
                .thenComparing(BizfiFiHealthCheckIssueVO::getCode));
        return result;
    }

    private void addIssueIfPresent(List<BizfiFiHealthCheckIssueVO> issues,
                                   String code,
                                   String name,
                                   String severity,
                                   List<String> details,
                                   int sampleLimit,
                                   String suggestion) {
        if (details == null || details.isEmpty()) {
            return;
        }
        issues.add(new BizfiFiHealthCheckIssueVO(
                code,
                name,
                severity,
                details.size(),
                details.stream().limit(sampleLimit).toList(),
                suggestion
        ));
    }

    private List<String> missingCodes(List<String> sourceCodes, Set<String> accountCodes) {
        return sourceCodes.stream()
                .filter(code -> !accountCodes.contains(code))
                .distinct()
                .sorted()
                .toList();
    }

    private <T> List<String> loadDistinctCodes(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, String column) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT " + column)
                .isNotNull(column)
                .ne(column, "");
        return mapper.selectObjs(wrapper).stream()
                .map(obj -> obj == null ? null : obj.toString())
                .map(this::normalizeCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    private String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String formatAccount(BizfiFiAccount account) {
        String code = StringUtils.hasText(account.getFcode()) ? account.getFcode().trim() : "NO_CODE";
        String name = StringUtils.hasText(account.getFname()) ? account.getFname().trim() : "NO_NAME";
        return code + "-" + name;
    }

    private String formatMapRef(BizfiFiReportAccountMap accountMap, String missingTarget) {
        return "map#" + accountMap.getFid()
                + "(templateId=" + accountMap.getFtemplateId()
                + ", accountId=" + accountMap.getFaccountId()
                + ", itemId=" + accountMap.getFitemId()
                + ", missing=" + missingTarget + ")";
    }

    private int normalizeSampleSize(Integer sampleSize) {
        if (sampleSize == null) {
            return 10;
        }
        return Math.max(1, Math.min(sampleSize, 50));
    }
}
