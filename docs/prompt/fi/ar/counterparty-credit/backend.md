# 往来方信用配置后端开发提示词

## 目标
基于现有 `BizfiFiCounterpartyCredit` 与 `BizfiFiArapDocServiceImpl` 中的信用控制逻辑，增强往来方信用配置能力。

## 必须参考的代码路径
- `fi-service/src/main/java/single/cjj/fi/ar/entity/BizfiFiCounterpartyCredit.java`
- `fi-service/src/main/java/single/cjj/fi/ar/service/BizfiFiArapDocService.java`
- `fi-service/src/main/java/single/cjj/fi/ar/service/impl/BizfiFiArapDocServiceImpl.java`
- `fi-service/src/main/java/single/cjj/fi/ar/controller/BizfiFiArapDocController.java`

## 当前代码事实
1. 信用配置通过 `counterparty + docTypeRoot` 唯一定位。
2. 支持额度阈值、逾期阈值、启用标识、超额度/超逾期硬拦截开关。
3. 当前保存和查询信用配置的接口挂在 `arap-doc` 控制器下。
4. 提交和审核时会调用信用规则进行硬拦截。

## 开发要求
1. 不要脱离当前实体和保存逻辑重新建一套信用中心模型。
2. 新增逻辑时优先增强现有服务实现。
3. 保持 `AR / AP` 根类型校验规则不变。
4. 涉及默认值处理时要和当前实现保持一致。
5. 输出代码时列出新增或修改的类与方法。

## 可扩展方向
- 增加信用配置独立应用服务
- 增加批量导入或批量更新
- 增加操作日志
- 增加更多预警维度

## 输出格式要求
1. 改造目标
2. 影响类清单
3. 关键代码
4. 风险点
5. 文件路径清单
