# 财务云 README 过渡说明（fi）

## 当前情况
`docs/bus/fi/README.md` 仍保留历史口径描述，尚未完成正式替换。

## 当前应优先使用的新入口
请优先查看以下文件：
- `docs/bus/fi/INDEX.md`
- `docs/bus/fi/AUDIT_STATUS.md`
- `docs/bus/fi/HANDOFF.md`

## 新结构入口
当前财务云已建立的新分组包括：
- `gl/`
- `ar/`
- `ap/`
- `finance-base-data/`
- `fund/`
- `tax/`
- `fa/`
- `shared/`

## 使用原则
1. 新增或修订文档时，以 `INDEX.md` 所示的新分组结构为准。
2. 不再按旧 README 中的历史散列口径继续扩写。
3. 未实现模块仅保留状态说明，不虚构业务细节。
4. 已实现或可确认前端行为的模块，继续按当前代码扩写到对应新分组目录。

## 后续动作
待具备可稳定覆盖现有文件的能力后，再正式替换 `docs/bus/fi/README.md`。
