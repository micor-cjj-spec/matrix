# 总账-三大报表开发规格建议（V1）

> 模块：GL（总账）
> 主题：三大报表开发规格
> 更新时间：2026-03-28

## 1. 目标
在现有 `matrix` + `matrix-web` 项目基础上，把三大报表从“业务设想”收敛到“可以排期开发”的规格。

本文重点解决：
- 表结构先建哪些
- 后端类怎么分层
- 接口怎么命名更贴合当前项目
- 前端页面和 API 文件放哪里
- 第一阶段先做哪些，避免一开始就铺太大

## 2. 与当前项目的对齐结论
### 2.1 后端现状
当前 `fi-service` 的总账代码主要集中在：
- `single.cjj.fi.gl.controller`
- `single.cjj.fi.gl.entity`
- `single.cjj.fi.gl.mapper`
- `single.cjj.fi.gl.service`

现有接口风格特点：
- 单资源控制器，如 `/voucher`、`/account-subject`
- 列表查询使用 `/list`
- 统一返回 `ApiResponse<T>`
- 简单主数据接口多采用 `GET/POST/PUT/DELETE`

### 2.2 前端现状
当前总账前端主要集中在：
- `src/views/login/ledger/**`
- `src/api/ledgerReport.js`
- `src/router/index.js`

现有前端模式特点：
- 报表页使用独立 `View.vue`
- API 文件按业务域拆分
- 接口统一通过 `request.js` 自动补 `/api`

### 2.3 设计结论
为了最少改动现有结构，三大报表建议继续挂在 `gl` 之下，但单独开一个 `report` 子域，避免把总账包继续堆满。

建议后端包结构：
- `single.cjj.fi.gl.report.controller`
- `single.cjj.fi.gl.report.dto`
- `single.cjj.fi.gl.report.entity`
- `single.cjj.fi.gl.report.mapper`
- `single.cjj.fi.gl.report.service`
- `single.cjj.fi.gl.report.service.impl`
- `single.cjj.fi.gl.report.vo`

## 3. 第一阶段推荐表结构
第一阶段建议只建 4 张核心表 + 1 张现金流表，先支持报表平台、资产负债表、利润表，并给现金流预留入口。

### 3.1 `bizfi_fi_report_template`
报表模板主表。

建议字段：
- `fid` BIGINT PK
- `fcode` VARCHAR(64) 模板编码，如 `BS-STD`
- `fname` VARCHAR(100) 模板名称
- `ftype` VARCHAR(32) 报表类型：`BALANCE_SHEET` / `PROFIT_STATEMENT` / `CASH_FLOW`
- `forg` BIGINT 组织 ID
- `fversion` VARCHAR(32) 版本号
- `fstatus` VARCHAR(20) 状态：`DRAFT` / `ENABLED` / `DISABLED`
- `fremark` VARCHAR(500)
- `fcreatetime`
- `fupdatetime`

索引建议：
- `uk_fcode_version (fcode, fversion, forg)`
- `idx_ftype (ftype)`

### 3.2 `bizfi_fi_report_item`
报表项目表，对应报表中的每一行。

建议字段：
- `fid` BIGINT PK
- `ftemplate_id` BIGINT 模板 ID
- `fparent_id` BIGINT 父项目 ID
- `fcode` VARCHAR(64) 行项目编码
- `fname` VARCHAR(200) 行项目名称
- `frow_no` VARCHAR(20) 行次
- `flevel` INT 层级
- `fline_type` VARCHAR(20) 行类型：`DETAIL` / `TOTAL` / `FORMULA` / `TEXT`
- `fperiod_mode` VARCHAR(20) 期间模式：`END` / `BEGIN` / `CURRENT` / `YTD`
- `fsign_rule` VARCHAR(20) 方向规则：`RAW` / `ABS` / `NEGATE`
- `fdrillable` TINYINT(1)
- `feditable_adjustment` TINYINT(1)
- `fsort` INT
- `fremark` VARCHAR(500)
- `fcreatetime`
- `fupdatetime`

索引建议：
- `idx_ftemplate_sort (ftemplate_id, fsort)`
- `idx_fparent (fparent_id)`

### 3.3 `bizfi_fi_report_rule`
报表取数规则表。

建议字段：
- `fid` BIGINT PK
- `fitem_id` BIGINT 报表项目 ID
- `frule_type` VARCHAR(32)
- `frule_expr` TEXT
- `fsign_transform` VARCHAR(20)
- `fpriority` INT
- `fstatus` VARCHAR(20)
- `fcreatetime`
- `fupdatetime`

第一阶段支持的 `frule_type` 建议：
- `ACCOUNT`
- `ACCOUNT_RANGE`
- `ITEM_SUM`
- `FORMULA`
- `PL_TYPE`
- `CASHFLOW_ITEM`

`frule_expr` 建议先用 JSON 文本保存，避免一开始就把规则拆得过细。

示例：
```json
{
  "accountCodes": ["1001", "1002"],
  "includeChildren": true,
  "direction": "END_BALANCE"
}
```

### 3.4 `bizfi_fi_report_account_map`
科目到报表项目映射表。

建议字段：
- `fid` BIGINT PK
- `ftemplate_id` BIGINT
- `fitem_id` BIGINT
- `faccount_id` BIGINT
- `fmapping_type` VARCHAR(20)：`DIRECT` / `PL` / `CASHFLOW`
- `feffective_from` VARCHAR(20)
- `feffective_to` VARCHAR(20)
- `fremark` VARCHAR(500)
- `fcreatetime`
- `fupdatetime`

说明：
- 第一阶段可以保留 `bizfi_fi_account.freport_item`
- 但建议新映射表同步建设，后续逐步替代字段硬绑定

### 3.5 `bizfi_fi_cashflow_item`
现金流量项目主数据表。

建议字段：
- `fid` BIGINT PK
- `fcode` VARCHAR(64)
- `fname` VARCHAR(200)
- `fparent_id` BIGINT
- `fcategory` VARCHAR(20)：`OPERATE` / `INVEST` / `FINANCE`
- `fdirection` VARCHAR(20)：`INFLOW` / `OUTFLOW` / `NET`
- `fsort` INT
- `fstatus` VARCHAR(20)
- `fremark` VARCHAR(500)
- `fcreatetime`
- `fupdatetime`

## 4. 第二阶段预留表结构
这些表不建议第一天就建，但文档先留口径。

### 4.1 `bizfi_fi_cashflow_adjust`
现金流人工补录/调整表。

### 4.2 `bizfi_fi_report_snapshot`
月结或对外报送后的报表快照表。

### 4.3 `bizfi_fi_report_check_log`
报表平衡校验、勾稽校验结果留痕表。

## 5. Java 类清单建议
### 5.1 实体类
建议新增：
- `BizfiFiReportTemplate`
- `BizfiFiReportItem`
- `BizfiFiReportRule`
- `BizfiFiReportAccountMap`
- `BizfiFiCashflowItem`

第二阶段新增：
- `BizfiFiCashflowAdjust`
- `BizfiFiReportSnapshot`

### 5.2 Mapper
建议新增：
- `BizfiFiReportTemplateMapper`
- `BizfiFiReportItemMapper`
- `BizfiFiReportRuleMapper`
- `BizfiFiReportAccountMapMapper`
- `BizfiFiCashflowItemMapper`

### 5.3 DTO
建议新增：
- `ReportQueryRequest`
- `ReportDrilldownRequest`
- `ReportTemplateSaveRequest`
- `ReportItemSaveRequest`
- `ReportRuleSaveRequest`
- `CashflowAdjustSaveRequest`

### 5.4 VO
建议新增：
- `ReportRowVO`
- `ReportQueryResultVO`
- `ReportDrilldownVO`
- `ReportWarningVO`
- `ReportCheckResultVO`

### 5.5 Service
建议拆成 3 层职责：

主数据服务：
- `BizfiFiReportTemplateService`
- `BizfiFiReportItemService`
- `BizfiFiReportRuleService`
- `BizfiFiReportAccountMapService`
- `BizfiFiCashflowItemService`

计算服务：
- `BizfiFiBalanceSheetService`
- `BizfiFiProfitStatementService`
- `BizfiFiCashFlowService`

公共能力服务：
- `BizfiFiReportEngineService`
- `BizfiFiReportDrilldownService`
- `BizfiFiReportCheckService`

## 6. 接口清单建议
### 6.1 主数据接口
为了贴合现有控制器风格，建议沿用“资源 + /list”的命名。

#### 报表模板
- `GET /report-template/{fid}`
- `POST /report-template`
- `PUT /report-template`
- `DELETE /report-template/{fid}`
- `GET /report-template/list`

#### 报表项目
- `GET /report-item/{fid}`
- `POST /report-item`
- `PUT /report-item`
- `DELETE /report-item/{fid}`
- `GET /report-item/list`
- `GET /report-item/tree`

#### 报表规则
- `GET /report-rule/{fid}`
- `POST /report-rule`
- `PUT /report-rule`
- `DELETE /report-rule/{fid}`
- `GET /report-rule/list`

#### 科目报表映射
- `GET /report-account-map/{fid}`
- `POST /report-account-map`
- `PUT /report-account-map`
- `DELETE /report-account-map/{fid}`
- `GET /report-account-map/list`

#### 现金流项目
- `GET /cashflow-item/{fid}`
- `POST /cashflow-item`
- `PUT /cashflow-item`
- `DELETE /cashflow-item/{fid}`
- `GET /cashflow-item/list`

### 6.2 报表查询接口
第一阶段建议直接查询，不分页。

#### 资产负债表
- `GET /balance-sheet`
- `GET /balance-sheet/drilldown`
- `GET /balance-sheet/check`

#### 利润表
- `GET /profit-statement`
- `GET /profit-statement/drilldown`
- `GET /profit-statement/check`

#### 现金流量表
- `GET /cash-flow`
- `GET /cash-flow/drilldown`
- `GET /cash-flow/check`

### 6.3 导出接口
第一阶段可先不单独做异步导出，建议同步导出：
- `GET /balance-sheet/export`
- `GET /profit-statement/export`
- `GET /cash-flow/export`

## 7. 接口入参建议
### 7.1 `GET /balance-sheet`
建议参数：
- `orgId`
- `period`
- `currency`
- `templateId`
- `showZero`

### 7.2 `GET /profit-statement`
建议参数：
- `orgId`
- `startPeriod`
- `endPeriod`
- `currency`
- `templateId`
- `showZero`

### 7.3 `GET /cash-flow`
建议参数：
- `orgId`
- `startPeriod`
- `endPeriod`
- `currency`
- `templateId`
- `includeAdjust`
- `showZero`

### 7.4 `GET /report-item/list`
建议参数：
- `templateId`
- `type`
- `name`
- `page`
- `size`

## 8. 接口返回结构建议
### 8.1 统一外层
保持当前项目风格：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

### 8.2 报表查询结果
建议 `data` 结构：

```json
{
  "reportType": "BALANCE_SHEET",
  "templateId": 1,
  "templateName": "资产负债表（标准版）",
  "orgId": 1,
  "period": "2026-03",
  "currency": "CNY",
  "rows": [
    {
      "itemId": 101,
      "itemCode": "BS_001",
      "itemName": "货币资金",
      "rowNo": "1",
      "level": 1,
      "lineType": "DETAIL",
      "amount": 100000.00,
      "beginAmount": 90000.00,
      "currentAmount": null,
      "ytdAmount": null,
      "drillable": true,
      "warnings": []
    }
  ],
  "checks": [
    {
      "code": "BALANCE_CHECK",
      "passed": true,
      "message": "资产总计 = 负债和所有者权益总计"
    }
  ],
  "warnings": []
}
```

说明：
- 报表查询不要复用 IPage
- 报表行建议统一为 `rows`
- 余额表和利润表共享同一个 `ReportRowVO`，不同字段允许为空

### 8.3 主数据列表结果
主数据接口保持你们现有习惯，返回 `IPage<T>` 即可，这样前端更容易复用现有表格页面。

## 9. 前端文件清单建议
### 9.1 API 文件
建议新增：
- `src/api/financialReport.js`
- `src/api/reportTemplate.js`
- `src/api/reportItem.js`
- `src/api/reportRule.js`
- `src/api/reportAccountMap.js`
- `src/api/cashflowItem.js`

其中：
- 查询类接口放在 `financialReport.js`
- 主数据维护类接口按资源拆分

### 9.2 页面文件
建议新增：
- `src/views/login/ledger/report/BalanceSheetView.vue`
- `src/views/login/ledger/report/ProfitStatementView.vue`
- `src/views/login/ledger/report/CashFlowView.vue`
- `src/views/login/ledger/report/ReportItemView.vue`
- `src/views/login/ledger/report/ReportTemplateView.vue`
- `src/views/login/ledger/report/ReportRuleView.vue`
- `src/views/login/ledger/report/ReportAccountMapView.vue`
- `src/views/login/ledger/report/CashflowItemView.vue`

### 9.3 通用组件
建议新增：
- `src/views/login/ledger/report/components/ReportQueryBar.vue`
- `src/views/login/ledger/report/components/ReportTable.vue`
- `src/views/login/ledger/report/components/ReportDrilldownDialog.vue`
- `src/views/login/ledger/report/components/ReportCheckPanel.vue`

## 10. 前端路由建议
建议在 `src/router/index.js` 中新增：
- `/ledger/report-template`
- `/ledger/report-item`
- `/ledger/report-rule`
- `/ledger/report-account-map`
- `/ledger/cashflow-item`
- `/ledger/balance-sheet`
- `/ledger/profit-statement`
- `/ledger/cash-flow`

建议与总账首页卡片对应关系如下：
- “报表项目” -> `/ledger/report-item`
- “资产负债表” -> `/ledger/balance-sheet`
- “利润表” -> `/ledger/profit-statement`
- “现金流量表” -> `/ledger/cash-flow`
- “现金流量项目” -> `/ledger/cashflow-item`
- “科目现金流量映射关系” -> `/ledger/report-account-map`

## 11. 第一阶段开发顺序
建议按下面顺序推进，能最快看到结果。

### 11.1 第一步：后端基础主数据
先建：
- `bizfi_fi_report_template`
- `bizfi_fi_report_item`
- `bizfi_fi_report_rule`
- `bizfi_fi_report_account_map`

同时落：
- Entity / Mapper / Service / Controller
- 基础增删改查接口

### 11.2 第二步：资产负债表查询
先打通：
- 查询参数
- 从余额表取数
- 报表行组装
- 平衡校验
- 前端只做查询展示

### 11.3 第三步：利润表查询
在资产负债表稳定后复用框架：
- 本期金额
- 本年累计
- 缺失科目映射提示

### 11.4 第四步：前端统一组件
抽出：
- 查询栏
- 报表表格
- 校验区
- 下钻弹窗

### 11.5 第五步：现金流主数据预埋
先只建 `cashflow-item` 主数据和接口，不立即做完整现金流量表引擎。

## 12. 迁移策略建议
考虑到当前科目表已有 `freport_item`、`fpltype`，建议采用平滑迁移：

1. 第一阶段查询利润表时，优先读取新映射表
2. 若新映射表无数据，再兜底读取 `bizfi_fi_account.freport_item` / `fpltype`
3. 待主数据维护页上线后，再逐步废弃旧字段直连逻辑

这样做的好处：
- 不阻塞当前已有科目资料
- 不要求一次性洗完所有历史数据
- 后续可以在不影响前台查询的情况下切换口径

## 13. 本轮可直接进入开发的清单
如果下一步就要开工，建议直接建立以下文件和接口：

后端：
- `BizfiFiReportTemplateController`
- `BizfiFiReportItemController`
- `BizfiFiBalanceSheetController`
- `BizfiFiProfitStatementController`
- `BizfiFiReportTemplateService`
- `BizfiFiReportItemService`
- `BizfiFiBalanceSheetService`
- `BizfiFiProfitStatementService`

前端：
- `src/api/financialReport.js`
- `src/api/reportItem.js`
- `src/views/login/ledger/report/BalanceSheetView.vue`
- `src/views/login/ledger/report/ProfitStatementView.vue`
- `src/views/login/ledger/report/ReportItemView.vue`

数据库：
- `bizfi_fi_report_template`
- `bizfi_fi_report_item`
- `bizfi_fi_report_rule`
- `bizfi_fi_report_account_map`

## 14. 待确认
- [ ] 报表模板是否按组织隔离，还是全局模板 + 组织启用关系
- [ ] 余额表是否已经具备资产负债表和利润表所需全部字段
- [ ] 第一阶段利润表累计口径是否按自然年
- [ ] 是否要求报表导出必须和页面显示格式完全一致
- [ ] 现金流量表第一阶段是否只做主表不做附表
