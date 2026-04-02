# 凭证字业务说明

## 1. 业务名称
凭证字（Voucher Type）

## 2. 业务背景
凭证字用于对凭证进行分类与编号辅助控制。当前代码中 `BizfiFiVoucherType` 包含组织、编码、名称、编号前缀、排序、状态等字段，并提供新增、修改、删除、启用、停用、分页查询接口。fileciteturn226file0L1-L29 fileciteturn236file0L1-L57

## 3. 业务目标
- 为不同组织维护可用的凭证字清单
- 支撑默认凭证字配置和结转清单检查
- 为凭证治理和编号规则预留基础配置

## 4. 单据定位
凭证字属于 `fi/gl` 域的基础配置对象，不是业务单据流转主体，但会影响凭证分类和组织财务参数。fileciteturn224file0L151-L211

## 5. 核心业务内容
- 维护 `forg + fcode` 维度下唯一的凭证字配置fileciteturn239file0L89-L97
- 维护编码、名称、编号前缀、排序、状态等属性fileciteturn226file0L12-L29
- 支持启用和停用状态切换fileciteturn236file0L34-L41
- 被组织财务参数中的默认凭证字所引用fileciteturn227file0L16-L21

## 6. 上下游关系
### 上游
- 组织信息
- 组织财务参数

### 下游
- 凭证录入与分类
- 结转清单与默认凭证字检查fileciteturn224file0L151-L211

## 7. 关键控制点
- 同组织下凭证字编码必须唯一。fileciteturn239file0L89-L97
- 状态仅支持 `ENABLED / DISABLED`。fileciteturn239file0L119-L129
- 默认值和编码标准化统一在服务层处理。fileciteturn239file0L106-L131
