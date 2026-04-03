# 总账分录后端开发提示词

## 目标
基于现有 `BizfiFiGlEntry` 和 `BizfiFiVoucherServiceImpl` 的过账/冲销逻辑，增强总账分录查询与追溯能力，不要把总账分录做成新的人工录入模块。

## 必须参考的代码路径
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiGlEntry.java`
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiVoucherServiceImpl.java`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiVoucherController.java`

## 当前代码事实
1. 总账分录由凭证过账时自动生成。
2. 过账前会先清理当前凭证已存在的总账分录，保证幂等。
3. 冲销时生成反向凭证和反向总账分录。
4. 当前没有独立总账分录 Controller。

## 开发要求
1. 不要为总账分录新增人工编辑能力。
2. 优先补查询、追溯、分析类能力。
3. 新增查询接口时，保证能按凭证、日期、科目、过账人等维度过滤。
4. 保持和现有过账逻辑兼容，不能破坏幂等行为。
5. 输出代码时列出修改文件路径。

## 可扩展方向
- 独立总账分录查询接口
- 按科目区间查询
- 总账分录与凭证双向跳转
- 过账日志明细查询

## 输出格式要求
1. 改造目标
2. 影响类清单
3. 关键代码
4. 风险点
5. 文件路径清单
