# 业务规则

## 基础规则
- 仅已审核凭证可过账。fileciteturn217file0L88-L96
- 过账前会校验凭证明细借贷平衡和合法性。fileciteturn217file0L94-L99 fileciteturn217file0L335-L364

## 幂等规则
- 过账时会先删除当前凭证已有总账分录，再重新插入。fileciteturn217file0L101-L107
- 批量过账逐笔调用单笔过账，成功和失败分开统计。fileciteturn217file0L127-L158

## 冲销规则
- 仅已过账凭证可冲销。fileciteturn217file0L176-L181
- 冲销通过生成反向凭证和反向分录实现，而不是直接删除原总账分录。fileciteturn217file0L183-L227

## 数据规则
- 总账分录的借贷金额、摘要、现金流量项目来自凭证明细或凭证头。fileciteturn217file0L108-L125
- 总账分录记录过账人和过账时间，作为正式账务沉淀依据。fileciteturn223file0L24-L29
