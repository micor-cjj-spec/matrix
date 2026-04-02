# 凭证字后端开发提示词

## 目标
基于现有 `BizfiFiVoucherType`、`BizfiFiVoucherTypeController`、`BizfiFiVoucherTypeServiceImpl` 继续迭代凭证字能力，不重新起一套凭证字模型。

## 必须参考的代码路径
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiVoucherType.java`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiVoucherTypeController.java`
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiVoucherTypeServiceImpl.java`

## 当前代码事实
1. 同组织下 `fcode` 唯一。
2. 状态仅支持 `ENABLED / DISABLED`。
3. 编码保存时统一转大写。
4. 已提供新增、修改、删除、启用、停用、分页查询接口。

## 开发要求
1. 保持现有实体和接口不变。
2. 继续复用组织维度唯一性校验。
3. 编码标准化逻辑保留在服务层。
4. 输出代码时列出新增或修改文件路径。

## 可扩展方向
- 增加默认凭证字推荐查询
- 增加排序调整能力
- 增加删除前被引用检查
- 增加批量启停用能力

## 输出格式要求
1. 改造目标
2. 影响类清单
3. 关键代码
4. 风险点
5. 文件路径清单
