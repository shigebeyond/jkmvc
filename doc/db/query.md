# Making Queries

There are two different ways to make queries. 

1. [Db](getting_started.md) methods

```
Db::execute()
Db::queryCell()
Db::queryRow()
Db::queryRows()
```

2. [query builder](query_builder.md) methods

`DbQueryBuilder` provides methods similar to sql syntax, and generates real sql adapted to different databases(eg. mysql/oracle).

