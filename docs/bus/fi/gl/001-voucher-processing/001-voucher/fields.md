# 字段设计

## 主表字段（BizfiFiVoucher）

| 字段 | 含义 | 说明 |
|---|---|---|
| fid | 主键ID | 凭证唯一标识 |
| fnumber | 凭证号 | 可前端传入；为空时后端按 `VyyyyMM####` 自动生成 |
| fdate | 凭证日期 | 必填；保存草稿和修改时都会校验期间是否已关账 |
| fsummary | 凭证摘要 | 必填 |
| famount | 凭证金额 | 主表金额；保存草稿时若未传或小于等于0，后端兜底为 1；提交/过账时以分录借方合计回写 |
| fstatus | 状态 | `DRAFT / SUBMITTED / AUDITED / POSTED / REJECTED / REVERSED` |
| fcreatedBy | 制单人 | 保存草稿时默认 `system` |
| fcreatedTime | 制单时间 | 保存草稿时写入 |
| fauditedBy | 审核人 | 审核或驳回时写入 |
| fauditedTime | 审核时间 | 审核或驳回时写入 |
| fpostedBy | 过账人 | 过账时写入 |
| fpostedTime | 过账时间 | 过账时写入 |
| fremark | 备注 | 冲销链路会追加关联信息 |

## 分录字段（BizfiFiVoucherLine）

| 字段 | 含义 | 说明 |
|---|---|---|
| fid | 分录ID | 保存分录时由后端重新生成 |
| fvoucherId | 凭证ID | 对应主表 `fid` |
| flineNo | 行号 | 保存时未传则后端按顺序补齐 |
| faccountCode | 科目编码 | 必填 |
| fsummary | 分录摘要 | 可为空；过账时为空则回退主表摘要 |
| fdebitAmount | 借方金额 | 与贷方金额互斥 |
| fcreditAmount | 贷方金额 | 与借方金额互斥 |
| fcurrency | 币种 | 可空；影响原币金额小数位处理 |
| frate | 汇率 | 统一按 6 位小数保存 |
| foriginalAmount | 原币金额 | JPY/KRW/VND 按 0 位小数，其余按 2 位小数 |
| fcashflowItem | 现金流项目 | 前后空格会被去除 |

## 前端当前实际维护字段

前端 `VoucherView.vue` 当前主表只维护：`fnumber / fdate / fsummary / famount / fremark`；
分录弹窗当前只维护：`faccountCode / fsummary / fdebitAmount / fcreditAmount / flineNo`。
