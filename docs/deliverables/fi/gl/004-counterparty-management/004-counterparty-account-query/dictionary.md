# 往来账查询口径说明

## 1. 查询条件口径
- `docTypeRoot`：往来类型
- `counterparty`：往来方
- `docType`：单据类型
- `status`：单据状态
- `openOnly`：仅看未核销
- `keyword`：关键字
- `asOfDate`：统计日期

## 2. 汇总统计口径
- `docCount`：单据数
- `totalAmount`：原额
- `writtenOffAmount`：已核销金额
- `openAmount`：未核销金额
- `openDocCount`：未核销单据数

## 3. 单据级结果口径
- `records`：按单据展示原额、已核销、未核销、核销状态、账龄和关联凭证信息
- 账龄按统计日期与单据日期差异计算
- 核销状态应区分未核销、部分核销、已核销等状态

## 4. warnings 与关联凭证口径
- warnings 由后端返回
- 当前页面用于检索和核对，可查看关联凭证信息

## 5. 待确认项
- 账龄区间规则是否需要更细业务定义
- 核销状态枚举口径是否需要独立文档化
