# Making Queries

There are two different ways to make queries. 

1. [Db](db) methods 

```
Db::execute()
Db::queryCell()
Db::queryRow()
Db::queryRows()
```

2. [query builder](query_builder) methods

DbQueryBuilder provides methods similar to sql syntax, and generates real sql adapted to different databases(eg. mysql/oracle).

