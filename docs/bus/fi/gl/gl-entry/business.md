# 总账分录业务说明

## 1. 业务名称
总账分录（GL Entry）

## 2. 业务背景
总账分录是凭证过账后的结果数据，用于将凭证明细沉淀为正式账务记录。当前代码中 `BizfiFiGlEntry` 由凭证过账时生成，并记录凭证、分录行、科目、借贷金额、现金流项目、过账时间和过账人等信息。fileciteturn223file0L1-L29 fileciteturn217file0L93-L125

## 3. 业务目标
- 承接凭证过账结果
- 为总账查询和账务沉淀提供正式结果数据
- 支撑凭证冲销、过账幂等和后续分析能力

## 4. 单据定位
总账分录属于 `fi/gl` 域的结果型账务对象，不是人工录入单据，而是凭证过账后形成的正式账务记录。fileciteturn223file0L12-L29

## 5. 核心业务内容
- 记录 `voucherId / voucherLineId / voucherNumber / voucherDate`
- 记录科目、摘要、借贷金额和现金流项目
- 记录过账人和过账时间
- 在重复过账场景下先清理旧分录，再重新写入，保证幂等。fileciteturn217file0L93-L107

## 6. 上下游关系
### 上游
- 凭证头 `BizfiFiVoucher`
- 凭证明细 `BizfiFiVoucherLine`

### 下游
- 总账查询
- 账务分析
- 冲销与追溯

## 7. 关键控制点
- 仅已审核凭证可过账，过账后才产生总账分录。fileciteturn217file0L88-L96
- 过账时必须按凭证明细逐行生成总账分录。fileciteturn217file0L108-L125
- 冲销时会生成反向凭证和对应反向总账分录。fileciteturn217file0L176-L227
