# 业务规则

## 基础规则
- 组织ID不能为空。fileciteturn239file0L106-L112
- 凭证字编码不能为空，保存时统一转为大写。fileciteturn239file0L112-L117
- 凭证字名称不能为空。fileciteturn239file0L113-L116

## 唯一性规则
- 同组织下凭证字编码不能重复。fileciteturn239file0L89-L97

## 状态规则
- 状态仅支持 `ENABLED / DISABLED`。fileciteturn239file0L119-L129
- 启用、停用通过独立接口切换。fileciteturn236file0L34-L41

## 默认值规则
- `fsort` 为空时默认 0。fileciteturn239file0L117-L118
- 未显式传状态时，新增默认取 `ENABLED`。fileciteturn239file0L119-L123

## 联动规则
- 组织财务配置中的 `fdefaultVoucherType` 会引用凭证字编码。fileciteturn227file0L16-L21
- 凭证分析和结转清单会检查默认凭证字是否存在且处于启用态。fileciteturn224file0L151-L211
