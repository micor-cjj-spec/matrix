# 流程设计

## 状态定义
- `DRAFT`：草稿
- `SUBMITTED`：已提交
- `AUDITED`：已审核
- `POSTED`：已过账
- `REJECTED`：已驳回
- `REVERSED`：已冲销

## 主流程
1. 新增凭证：前端调用 `POST /voucher`，后端保存为草稿。
2. 维护分录：前端在分录弹窗调用 `PUT /voucher/{fid}/lines` 保存分录。
3. 提交：`DRAFT / REJECTED -> SUBMITTED`。
4. 审核：`SUBMITTED -> AUDITED`。
5. 过账：`AUDITED -> POSTED`，同步生成总账分录 `BizfiFiGlEntry`。
6. 驳回：`SUBMITTED -> REJECTED`。
7. 冲销：`POSTED -> REVERSED`，并自动生成一张新的冲销凭证，状态直接为 `POSTED`。

## 冲销流程说明
- 原凭证必须为已过账。
- 冲销凭证号规则：`RV-原凭证号`。
- 冲销凭证日期为当前日期。
- 冲销分录借贷方向与原分录相反。
- 原凭证状态更新为 `REVERSED`，备注追加“已冲销到凭证:xxx”。

## 前端按钮与状态对应
- 草稿/驳回：编辑、分录、提交
- 已提交：审核、驳回
- 已审核：过账
- 已过账：冲销
- 草稿：删除
