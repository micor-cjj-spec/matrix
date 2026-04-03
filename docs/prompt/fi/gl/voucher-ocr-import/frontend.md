# 凭证OCR导入前端开发提示词

## 目标
设计凭证OCR导入页面，并与现有解析、确认接口保持一致。

## 必须参考的文档和代码
- `docs/bus/fi/gl/voucher-ocr-import/*.md`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiVoucherController.java`
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiVoucherOcrServiceImpl.java`

## 页面目标
1. 支持文件上传并触发解析
2. 展示原始文本和结构化候选结果
3. 支持人工修正凭证头和分录候选
4. 支持确认导入后跳转到凭证草稿页面

## 设计要求
1. 解析页和确认页可以做成同一页面的两个区域。
2. 需要突出显示警告信息和高风险识别字段。
3. 分录候选必须支持编辑和删除。
4. 确认导入前要做最基本的前端校验。

## 输出要求
1. 页面结构
2. 上传与解析交互设计
3. 结果确认区设计
4. 表单字段与分录表格设计
5. 接口调用方案
