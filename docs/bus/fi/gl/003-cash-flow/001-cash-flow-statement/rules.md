# 业务规则

## 查询规则
- `period` 必须是 `yyyy-MM`，无效时直接返回 warning 和失败校验。
- `currency` 为空时默认 `CNY`。
- `templateId` 为空时优先取启用模板；若没有模板则返回 scaffold 行或提示。
- `showZero=false` 时隐藏金额为 0 的行。

## 归集规则
- 当前仅基于已过账总账分录。
- 现金类科目判断依赖会计科目的 `cash / bank / equivalent` 标记。
- 显式现金流项目编码优先于启发式分类。
- 未知现金流项目编码不会纳入主表，只返回 warning。
- 纯现金划转凭证会被主表剔除，避免夸大外部现金流。

## 公式规则
- `CF_NET_INCREASE = CF_OPERATING + CF_INVESTING + CF_FINANCING`

## 提示规则
- 启发式分类、未知编码、纯现金划转、组织隔离风险都会以 `warnings` 暴露。
