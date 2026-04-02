# 流程设计

## 状态定义
- ENABLED
- DISABLED

## 状态流转
ENABLED <-> DISABLED

## 业务流程
1. 新增凭证字并维护编码、名称、前缀、排序等信息。fileciteturn236file0L20-L27
2. 保存时统一执行编码大写、状态归一和唯一性校验。fileciteturn239file0L106-L131
3. 通过启用/停用接口切换状态。fileciteturn236file0L34-L41
4. 被组织财务配置引用为默认凭证字，供结转检查和凭证分类使用。fileciteturn224file0L151-L211

## 特殊说明
- 当前删除接口未限制状态，但实际使用时应谨慎处理被组织参数引用的记录。fileciteturn239file0L42-L45
