# BOTP 单据转换后端开发提示词

在 Matrix 后端新增独立 Maven 模块 `botp-service`，使用 Java 17、Spring Boot 3.2.5 和统一 `ApiResponse`。

实现范围：

1. `BotpApplication` 启动类。
2. 规则、映射、单据引用、目标草稿、执行请求和执行结果契约。
3. `BotpDocumentAdapter` SPI 和适配器注册表。
4. 映射引擎：支持 SOURCE_FIELD、CONSTANT、CONTEXT 点路径读取。
5. 规则仓储接口和内存演示实现。
6. 执行服务：预览、同步执行、请求幂等、执行结果查询。
7. 演示适配器：DEMO_ORDER → DEMO_DELIVERY。
8. REST API：规则查询、转换预览、执行、执行结果查询。

约束：

- 不允许 BOTP 直接操作业务单据表。
- 不允许任意脚本和 SQL。
- 必填映射为空时终止执行。
- 执行任务必须记录规则版本。
- 目标创建幂等键使用 `botp:{tenantId}:{executionId}:{targetIndex}`。
- 类和方法保持单一职责，所有异常返回明确业务信息。
