# 业务规则

## 基础规则
- 组织ID不能为空。fileciteturn240file0L95-L98
- 同一组织只允许存在一条财务配置。fileciteturn240file0L83-L90

## 格式与默认值规则
- 本位币为空时默认 `CNY`。fileciteturn240file0L99-L101
- 当前期间若有值，格式必须为 `yyyy-MM`。fileciteturn240file0L102-L109
- 期间控制模式仅支持 `STRICT / FLEXIBLE`。fileciteturn240file0L111-L118
- 状态仅支持 `ENABLED / DISABLED`。fileciteturn240file0L120-L125

## 联动规则
- 默认凭证字编码用于结转清单中的默认凭证字检查。fileciteturn224file0L151-L211
- 当前期间和本位币会被凭证分析及结转逻辑读取。fileciteturn224file0L107-L211
