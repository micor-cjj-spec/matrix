# 往来自动核销业务说明

## 1. 业务名称
往来自动核销

## 2. 业务定位
往来自动核销是在核销方案预览基础上的落库执行能力，用于批量生成自动核销日志和核销链接。

## 3. 业务目标
- 批量完成已审核往来单据的自动清账
- 持久化方案号、批次日志和核销明细
- 为对账单、核销日志、账龄分析提供真实核销结果来源

## 4. 数据来源
后端通过 `BizfiFiArapManageServiceImpl.autoWriteoff` 基于已审核单据生成匹配，并写入 `BizfiFiArapWriteoffLog / BizfiFiArapWriteoffLink`。
