# 凭证字测试提示词

## 目标
围绕现有凭证字实现，输出覆盖新增、修改、删除、启停用、分页查询和唯一性校验的测试方案。

## 必须参考的代码路径
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiVoucherType.java`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiVoucherTypeController.java`
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiVoucherTypeServiceImpl.java`

## 测试重点
1. 同组织下编码唯一性
2. 编码自动转大写
3. 状态枚举校验
4. 启用/停用切换
5. 排序默认值
6. 分页与筛选正确性
7. 删除后再新增回归

## 输出要求
1. 测试范围
2. 正常场景用例
3. 异常场景用例
4. 状态流转用例
5. 接口回归清单
