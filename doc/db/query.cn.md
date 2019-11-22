# 构建查询

有2种方式来构建查询

1. [Db](getting_started.cn.md) 方法

```
Db::execute()
Db::queryValue()
Db::queryRow()
Db::queryRows()
```

2. [query builder](query_builder.cn.md) 方法

查询构建器，是通过提供类sql的一系列方法，来帮助开发者快速构建原生sql，可兼容不同的数据库（如mysql/oracle）。

