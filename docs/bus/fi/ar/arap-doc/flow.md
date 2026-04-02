# 流程设计

## 状态定义
- DRAFT（草稿）
- SUBMITTED（已提交）
- AUDITED（已审核）
- REJECTED（已驳回）fileciteturn167file0L24-L27

## 状态流转
DRAFT -> SUBMITTED -> AUDITED
DRAFT -> SUBMITTED -> REJECTED
REJECTED -> SUBMITTED -> AUDITED

## 业务流程
1. 创建单据，系统校验基础字段并默认落为草稿fileciteturn167file0L42-L50
2. 草稿或驳回单据可提交，提交前会检查信用硬拦截规则fileciteturn167file0L349-L354
3. 已提交单据可审核或驳回fileciteturn167file0L356-L370
4. 审核通过后，系统自动生成并关联凭证；若已有关联则幂等跳过fileciteturn167file0L356-L363 fileciteturn167file0L271-L319
5. 可按凭证反查相关往来单据，也可做账龄汇总和信用预警分析fileciteturn166file0L29-L37

## 特殊说明
- `fdoctype` 决定业务含义，不同类型映射到不同凭证科目对。当前实现里已经内置 AR/AP 及其细分类型到科目编码的映射。fileciteturn167file0L28-L35
- 审核通过后并不是只改状态，而是会直接尝试自动转凭证。fileciteturn167file0L356-L363
