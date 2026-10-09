# BOTP 单据转换测试提示词

为 BOTP V1 编写单元测试和接口测试，至少覆盖：

1. SOURCE_FIELD 点路径映射成功。
2. CONSTANT 和 CONTEXT 映射成功。
3. 必填映射为空时抛出明确异常。
4. 单头和分录一对一映射正确。
5. 未注册源单或目标单适配器时失败。
6. 规则不存在或非 PUBLISHED 状态时失败。
7. 相同 `tenantId + sourceSystem + requestId` 重复执行返回相同 executionId。
8. 预览不调用目标适配器创建方法。
9. 目标创建使用稳定幂等键。
10. 查询不存在的 executionId 返回 404 语义。

测试不得依赖真实 MySQL、Nacos、RabbitMQ 或外部系统。演示适配器使用内存数据，确保测试可重复执行。
