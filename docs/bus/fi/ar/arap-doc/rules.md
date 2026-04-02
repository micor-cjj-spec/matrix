# 业务规则

## 单据规则
- `fdoctype` 不能为空，且当前业务根类型仅支持 AR/AP。fileciteturn167file0L414-L420 fileciteturn167file0L421-L426
- `fcounterparty` 不能为空，`famount` 必须大于 0。fileciteturn167file0L418-L420
- 未传 `fnumber` 时，系统按 `doctype-时间戳` 自动生成单据号。fileciteturn167file0L43-L48

## 状态规则
- 仅草稿可删除。fileciteturn167file0L63-L69
- 仅草稿/驳回可编辑、可提交。fileciteturn167file0L54-L61 fileciteturn167file0L349-L354
- 仅已提交可审核或驳回。fileciteturn167file0L356-L370
- 仅已审核单据可生成凭证。fileciteturn167file0L271-L280

## 信用规则
- 提交和审核前都会执行信用规则检查。fileciteturn167file0L349-L360
- 若命中“超额度且硬拦截”或“超逾期且硬拦截”，系统会直接抛出业务异常阻断操作。fileciteturn167file0L372-L413

## 凭证联动规则
- 单据类型与凭证借贷科目映射由代码内置表维护。fileciteturn167file0L28-L35
- 审核通过后自动生成凭证头和两条分录，并回写单据的凭证关联字段。fileciteturn167file0L281-L319

## 分析规则
- 账龄统计按单据日期到统计日期的天数分桶：0-30、31-60、61-90、91+。fileciteturn167file0L122-L151
- 信用预警按往来方聚合未结清金额，并结合额度阈值、逾期天数阈值判断是否告警。fileciteturn167file0L154-L213
