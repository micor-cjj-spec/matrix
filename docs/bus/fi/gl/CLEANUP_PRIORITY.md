# 总账旧路径清理优先级（gl）

> 基于 `docs/bus/fi/gl/README.md` 当前仍列出的历史目录，以及 `dev` 分支已落地的新菜单分组目录整理。

## P0：已存在明确新路径，应优先删除旧路径

### 1. `docs/bus/fi/gl/voucher/`
- 对应新路径：`docs/bus/fi/gl/001-voucher-processing/001-voucher/`
- 当前状态：新路径已完整补齐，旧路径已加 `DEPRECATED.md`
- 建议：优先删除旧路径

### 2. `docs/bus/fi/gl/cashflow-item/`
- 对应新路径：`docs/bus/fi/gl/010-basic-settings/002-cash-flow-item/`
- 当前状态：新路径已完整补齐，旧路径已加 `DEPRECATED.md`
- 建议：优先删除旧路径

## P1：旧路径已有新分组承接，但新分组当前仍未实现或不是一一映射

### 3. `docs/bus/fi/gl/opening-balance/`
- 相关新分组：`docs/bus/fi/gl/009-initialization/001-subject-opening-balance/`
- 当前状态：新分组按代码状态为 `not_implemented`
- 建议：删除前确认是否保留历史说明

### 4. `docs/bus/fi/gl/period-close/`
- 相关新分组：`docs/bus/fi/gl/007-period-end-processing/`
- 当前状态：新分组整体为 `not_implemented`
- 建议：待期末处理代码落地后删除，或直接按“代码优先”原则删旧路径

## P2：旧路径无明确新实现承接，需先确认归属再删

### 5. `docs/bus/fi/gl/account/`
- 说明：按当前结构更适合迁出 `gl`，归到更上层“财务云 / 基础资料”

### 6. `docs/bus/fi/gl/ledger/`
- 说明：当前新结构没有与其一一对应的新菜单实现

### 7. `docs/bus/fi/gl/voucher-word/`
- 说明：与 `010-basic-settings/001-voucher-type/` 相关，但并不完全等同

### 8. `docs/bus/fi/gl/accounting-period/`
- 说明：当前新结构中无独立实现目录，仅与 `007-period-end-processing` 存在弱关联

### 9. `docs/bus/fi/gl/carry-forward-template/`
- 说明：当前新结构中无独立实现目录，仅与 `007-period-end-processing` 存在弱关联

## 当前建议执行顺序
1. 删除 P0 目录
2. 再决定 P1 是否直接按代码现状删除
3. P2 先迁归属或保留废弃标记，不建议立刻硬删
