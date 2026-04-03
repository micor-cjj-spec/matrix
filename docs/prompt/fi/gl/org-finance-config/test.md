# 组织财务配置测试提示词

## 目标
围绕现有组织财务配置实现，输出覆盖新增、修改、删除、按组织查询、分页查询和默认值处理的测试方案。

## 必须参考的代码路径
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiOrgFinanceConfig.java`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiOrgFinanceConfigController.java`
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiOrgFinanceConfigServiceImpl.java`

## 测试重点
1. 同组织唯一性
2. 本位币默认值 `CNY`
3. 当前期间 `yyyy-MM` 格式校验
4. 期间控制模式枚举校验
5. 状态枚举校验
6. 按组织查询正确性
7. 分页和筛选正确性

## 输出要求
1. 测试范围
2. 正常场景用例
3. 异常场景用例
4. 默认值与格式校验用例
5. 接口回归清单
