# 财务月结工作台交付说明

## 场景

财务月结工作台 / 关账前检查中心。

## 追溯链路

- Draft：`docs/draft/2026/04/28/001-month-end-close-workbench`
- BUS：`docs/bus/fi/gl/007-period-end-processing/007-month-end-close-workbench`
- Prompt：`docs/prompt/fi/gl/007-period-end-processing/007-month-end-close-workbench`

## 代码落点

后端：

- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiPeriodProcessController.java`
- `fi-service/src/main/java/single/cjj/fi/gl/service/BizfiFiPeriodProcessService.java`
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiPeriodProcessServiceImpl.java`
- `fi-service/src/main/java/single/cjj/fi/gl/vo/MonthEndWorkbenchResultVO.java`
- `fi-service/src/main/java/single/cjj/fi/gl/vo/MonthEndCheckItemVO.java`
- `fi-service/src/main/java/single/cjj/fi/gl/vo/MonthEndStepVO.java`

前端：

- `matrix-web/src/views/login/ledger/period-process/MonthEndCloseWorkbenchView.vue`
- `matrix-web/src/api/periodProcess.js`
- `matrix-web/src/router/index.js`
- `matrix-web/src/views/login/ledger/GeneralLedgerView.vue`
- `matrix-web/src/views/login/ledger/period-process/periodProcessShared.js`

## 首版边界

- 只读检查，不执行正式关账。
- 不新增数据库表。
- 不保存检查批次。
- 不做反关账和审批流。

