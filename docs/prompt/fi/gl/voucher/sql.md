# 凭证 SQL 设计提示词

## 目标
根据业务文档生成凭证主表与分录表的数据库设计方案。

## 输入上下文
请结合以下文档：
- docs/bus/fi/gl/voucher/fields.md
- docs/bus/fi/gl/voucher/flow.md
- docs/bus/fi/gl/voucher/rules.md

## 输出要求
1. 生成主表和分录表 SQL
2. 设计主键、索引、审计字段
3. 考虑列表查询与详情查询场景
4. 输出完整建表语句
