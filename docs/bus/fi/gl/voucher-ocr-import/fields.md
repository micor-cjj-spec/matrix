# 字段设计

## 解析结果字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| rawText | OCR原始文本 | string | 是 | 解析后的原文 |
| voucher.fdate | 凭证日期 | string | 是 | 默认可回填当天 |
| voucher.fsummary | 凭证摘要 | string | 是 | OCR抽取或默认值 |
| voucher.famount | 凭证金额 | number | 是 | OCR抽取的最大金额 |
| lines | 分录候选列表 | array | 是 | 识别并推断出的分录 |
| engine | OCR引擎 | string | 否 | 当前实现返回引擎标识 |
| warning | 警告信息 | string | 否 | 需人工核对提示 |

## 分录候选字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| faccountCode | 科目编码 | string | 是 | 推断结果 |
| faccountName | 科目名称 | string | 否 | 推断结果 |
| fsummary | 分录摘要 | string | 是 | |
| fdebitAmount | 借方金额 | number | 否 | |
| fcreditAmount | 贷方金额 | number | 否 | |
| fdetailName | 明细名称 | string | 否 | 从文本中推断 |

## 说明
解析结果本身不直接落库，确认导入后才转为正式凭证草稿和凭证明细。
