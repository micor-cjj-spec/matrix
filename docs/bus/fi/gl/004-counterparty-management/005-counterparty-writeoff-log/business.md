# 往来核销日志业务说明

## 1. 业务名称
往来核销日志

## 2. 业务定位
往来核销日志用于追溯自动核销批次和落库的源单据/结算单据配对明细，是清账结果的审计与追踪页面。

## 3. 业务目标
- 查看批次级核销结果
- 按方案号下钻到具体链接明细
- 核对批次金额、链接数、操作人、操作时间

## 4. 数据来源
后端通过 `BizfiFiArapManageServiceImpl.writeoffLog` 读取 `BizfiFiArapWriteoffLog` 与 `BizfiFiArapWriteoffLink` 生成结果。
