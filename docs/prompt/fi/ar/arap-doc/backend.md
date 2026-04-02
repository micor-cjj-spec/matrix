# 往来单后端开发提示词

## 目标
基于 `fi-service` 中现有的 `BizfiFiArapDoc`、`BizfiFiArapDocController`、`BizfiFiArapDocService`、`BizfiFiArapDocServiceImpl` 继续迭代统一往来单能力，而不是新建一套平行模型。

## 必须参考的代码路径
- `fi-service/src/main/java/single/cjj/fi/ar/entity/BizfiFiArapDoc.java`
- `fi-service/src/main/java/single/cjj/fi/ar/controller/BizfiFiArapDocController.java`
- `fi-service/src/main/java/single/cjj/fi/ar/service/BizfiFiArapDocService.java`
- `fi-service/src/main/java/single/cjj/fi/ar/service/impl/BizfiFiArapDocServiceImpl.java`

## 当前代码事实
1. 统一单表模型承接 AR/AP 及其细分类型，通过 `fdoctype` 区分业务。
2. 当前状态机为 `DRAFT / SUBMITTED / AUDITED / REJECTED`。
3. 审核通过后会自动生成并关联总账凭证。
4. 已提供提交、审核、驳回、分页查询、账龄汇总、信用预警、凭证反查等接口。

## 开发要求
1. 优先复用现有实体和服务，不要先拆成多套独立单据模型。
2. 新增能力时，保持现有接口风格、命名风格和异常风格一致。
3. 所有状态变更都必须校验当前状态是否合法。
4. 涉及信用控制时，必须复用现有信用规则校验逻辑。
5. 涉及生成凭证时，必须保证幂等，避免重复生成。
6. 输出代码时列出新增或修改的文件路径。

## 可扩展方向
- 细化不同 `fdoctype` 的业务校验
- 增加统一查询 DTO / VO
- 增强分页查询条件
- 增加更细的操作日志或审计能力
- 增加按单据类型分层的应用服务

## 输出格式要求
请按以下顺序输出：
1. 改造思路
2. 影响的类和方法
3. 关键代码
4. 文件路径清单
5. 风险点和兼容性说明
