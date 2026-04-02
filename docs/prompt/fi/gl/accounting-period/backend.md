# 会计期间后端开发提示词

## 目标
基于现有 `BizfiFiAccountingPeriod`、`BizfiFiAccountingPeriodController`、`BizfiFiAccountingPeriodServiceImpl` 继续迭代会计期间能力，不要重起一套平行期间模型。

## 必须参考的代码路径
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiAccountingPeriod.java`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiAccountingPeriodController.java`
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiAccountingPeriodServiceImpl.java`

## 当前代码事实
1. 会计期间按 `forg + fperiod` 唯一。
2. 期间格式固定为 `yyyy-MM`。
3. 状态仅支持 `OPEN / CLOSED`。
4. 已提供新增、修改、删除、关闭、反开、分页查询能力。

## 开发要求
1. 保持现有实体和接口风格不变。
2. 继续复用当前的唯一性校验和状态校验逻辑。
3. 期间日期范围必须落在同一月份内。
4. 反开仅允许对已关闭期间执行。
5. 输出代码时列出改动文件路径。

## 可扩展方向
- 增加批量初始化年度期间
- 增加关账前校验钩子
- 增加期间状态变更日志
- 增加按组织自动生成全年期间能力

## 输出格式要求
1. 改造目标
2. 影响类清单
3. 关键代码
4. 风险点
5. 文件路径清单
