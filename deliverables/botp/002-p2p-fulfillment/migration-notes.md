# P0-IMP-02 migration 注意事项

`deliverables/botp/002-p2p-fulfillment/schema.sql` 在已有关系表上增加唯一索引。

生产/共享环境执行前必须先检查历史数据是否存在重复：

```text
同 tenant + execution + 完整 source document key + 完整 target document key
同 relation + source entry + target entry
```

如存在历史重复，应先审计和合并/标记无效，再增加唯一索引；不要直接删除无法解释的关系记录。
