# 凭证OCR导入后端开发提示词

## 目标
基于现有 `BizfiFiVoucherOcrServiceImpl` 和 `BizfiFiVoucherController` 继续增强凭证OCR导入能力，不重新起一套独立导入链路。

## 必须参考的代码路径
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiVoucherOcrServiceImpl.java`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiVoucherController.java`
- `fi-service/src/main/java/single/cjj/fi/gl/dto/BizfiFiVoucherOcrImportRequest.java`
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiVoucher.java`
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiVoucherLine.java`

## 当前代码事实
1. 已支持 `ocr/parse` 和 `ocr/confirm` 两步式导入。
2. 解析阶段会调用 OCR 服务，支持中文增强识别和乱码重试。
3. 解析结果会自动抽取日期、摘要、金额和分录候选。
4. 确认阶段会先保存凭证草稿，再保存凭证明细。

## 开发要求
1. 保持两步式导入，不要直接把 OCR 解析结果入正式库。
2. 新增能力时优先增强现有 OCR 服务实现。
3. 解析结果必须允许人工修正后再确认。
4. 错误信息要可读，不要只抛底层异常。
5. 输出代码时列出修改文件路径。

## 可扩展方向
- 支持更多模板识别
- 增强科目和摘要推断准确率
- 支持票据类型识别
- 增加导入历史和重试记录

## 输出格式要求
1. 改造目标
2. 影响类清单
3. 关键代码
4. 风险点
5. 文件路径清单
