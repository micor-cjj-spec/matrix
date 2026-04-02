# 组织财务配置后端开发提示词

## 目标
基于现有 `BizfiFiOrgFinanceConfig`、`BizfiFiOrgFinanceConfigController`、`BizfiFiOrgFinanceConfigServiceImpl` 继续增强组织财务配置能力。

## 必须参考的代码路径
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiOrgFinanceConfig.java`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiOrgFinanceConfigController.java`
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiOrgFinanceConfigServiceImpl.java`

## 当前代码事实
1. 同一组织只允许一条配置。
2. 本位币默认 `CNY`。
3. 当前期间格式为 `yyyy-MM`。
4. 期间控制模式仅支持 `STRICT / FLEXIBLE`。
5. 状态仅支持 `ENABLED / DISABLED`。

## 开发要求
1. 保持现有实体结构和按组织查询接口不变。
2. 继续复用唯一性和格式校验逻辑。
3. 新增能力要兼容凭证分析和结转清单对本配置的读取方式。
4. 输出代码时列出修改文件路径。

## 可扩展方向
- 增加独立状态切换接口
- 增加配置变更日志
- 增加默认凭证字有效性校验
- 增加组织批量初始化能力

## 输出格式要求
1. 改造目标
2. 影响类清单
3. 关键代码
4. 风险点
5. 文件路径清单
