# 总账旧路径清理清单（gl）

> 说明：以下内容用于收口 `docs/bus/fi/gl` 的历史目录。当前新结构已经按菜单分组落地，但 `dev` 上仍存在部分旧路径，需要后续统一删除或归档，避免新旧并存。

## 已确认仍存在的旧路径

### 1. 凭证旧路径
- 旧路径：`docs/bus/fi/gl/voucher/`
- 新路径：`docs/bus/fi/gl/001-voucher-processing/001-voucher/`
- 处理建议：删除旧路径，统一保留新路径。

### 2. 现金流量项目旧路径
- 旧路径：`docs/bus/fi/gl/cashflow-item/`
- 新路径：`docs/bus/fi/gl/010-basic-settings/002-cash-flow-item/`
- 处理建议：删除旧路径，统一保留新路径。

### 3. 期初余额旧路径
- 旧路径：`docs/bus/fi/gl/opening-balance/`
- 新路径：`docs/bus/fi/gl/009-initialization/001-subject-opening-balance/`
- 处理建议：由于新路径当前状态为 `not_implemented`，删除旧路径前应先确认是否需要保留历史说明；若总账目录完全以代码现状为准，则建议删除旧路径。

## 高优先级收口动作
1. 更新 `docs/bus/fi/gl/README.md`，替换旧的 `voucher/account/ledger` 旧口径描述。
2. 逐步删除已被新路径替代的旧路径。
3. 对仍无代码实现的新路径，保留 `not_implemented` 状态说明，不再沿用旧路径历史文案。

## 当前策略
- 目录结构以新菜单分组为准。
- 业务文档以代码现状为准。
- 历史旧路径不再继续扩写，后续只做删除/归档收口。
