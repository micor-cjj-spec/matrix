# 会计期间测试提示词

## 目标
围绕现有会计期间实现，输出覆盖新增、修改、删除、关账、反开和分页查询的测试方案。

## 必须参考的代码路径
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiAccountingPeriod.java`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiAccountingPeriodController.java`
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiAccountingPeriodServiceImpl.java`

## 测试重点
1. `forg + fperiod` 唯一性
2. `yyyy-MM` 格式校验
3. 开始日期、结束日期范围校验
4. OPEN/CLOSED 状态切换
5. 已关闭期间删除限制
6. 反开仅允许 CLOSED 状态
7. 分页和筛选条件正确性

## 输出要求
1. 测试范围
2. 正常场景用例
3. 异常场景用例
4. 状态流转用例
5. 接口回归清单
