# 凭证OCR导入测试提示词

## 目标
围绕现有 OCR 解析与确认导入实现，输出覆盖文件上传、解析结果、人工确认和异常处理的测试方案。

## 必须参考的代码路径
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiVoucherOcrServiceImpl.java`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiVoucherController.java`
- `fi-service/src/main/java/single/cjj/fi/gl/dto/BizfiFiVoucherOcrImportRequest.java`

## 测试重点
1. 空文件上传报错
2. OCR 正常解析返回结构化结果
3. 乱码场景自动降级重试
4. 日期、摘要、金额抽取结果正确性
5. 分录候选推断结果合理性
6. 确认导入后成功生成凭证草稿与分录
7. 缺少凭证头数据时确认接口拦截

## 输出要求
1. 测试范围
2. 正常场景用例
3. 异常场景用例
4. OCR特殊场景用例
5. 导入确认用例
6. 接口回归清单
