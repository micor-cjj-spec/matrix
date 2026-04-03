# 总账分录测试提示词

## 目标
围绕现有总账分录生成逻辑，输出覆盖过账、批量过账、冲销、幂等和追溯关系的测试方案。

## 必须参考的代码路径
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiGlEntry.java`
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiVoucherServiceImpl.java`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiVoucherController.java`

## 测试重点
1. 审核后过账生成总账分录
2. 重复过账的幂等行为
3. 批量过账成功/失败混合场景
4. 冲销后生成反向总账分录
5. 总账分录字段和来源凭证行的一致性
6. 现金流项目、摘要、借贷金额回写正确性

## 输出要求
1. 测试范围
2. 正常场景用例
3. 异常场景用例
4. 幂等与冲销用例
5. 追溯关系用例
6. 接口回归清单
