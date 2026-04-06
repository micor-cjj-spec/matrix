# 往来多维分析表口径说明

## 1. 查询条件口径
- `docTypeRoot`：往来类型
- `groupDimension`：汇总维度
- `asOfDate`：统计日期

## 2. 汇总统计口径
- `groupCount`：分组数
- `totalAmount`：原额
- `writtenOffAmount`：已核销金额
- `openAmount`：未核销金额

## 3. 多维聚合结果口径
- `rows`：按选定维度聚合展示结果
- 可按往来方、单据类型、状态、角色等维度汇总
- 每组展示原额、已核销、未核销等字段

## 4. warnings 口径
- warnings 由后端返回
- 用于提示分组异常、余额集中或风险聚集等问题

## 5. 待确认项
- groupDimension 可选枚举是否需要独立文档化
- warnings 的正式触发规则是否需要更细说明
