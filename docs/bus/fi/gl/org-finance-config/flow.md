# 流程设计

## 状态定义
- ENABLED
- DISABLED

## 状态流转
ENABLED <-> DISABLED

## 业务流程
1. 为组织新增财务配置。fileciteturn237file0L24-L26
2. 保存时统一处理本位币、当前期间、期间控制模式、状态等字段。fileciteturn240file0L95-L128
3. 通过按组织查询接口提供运行时读取。fileciteturn237file0L20-L22
4. 凭证分析和结转清单会读取当前期间、默认凭证字、本位币等信息。fileciteturn224file0L107-L211

## 特殊说明
- 当前没有独立启用/停用接口，状态通过新增或修改时维护。fileciteturn237file0L24-L33
- 每个组织只允许存在一条配置记录。fileciteturn240file0L83-L90
