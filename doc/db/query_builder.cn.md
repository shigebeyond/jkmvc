#  查询构建器

查询构建器，是通过提供类sql的一系列方法，来帮助开发者快速构建原生sql，可兼容不同的数据库（如mysql/oracle）。

## 1 创建查询构建器

直接创建`DbQueryBuilder`对象, 第一个参数是 `Db` 对象. 

```
val query = DbQueryBuilder(Db.instance())
val query = DbQueryBuilder() // 第一个参数有默认值 Db.instance() 
```

## 2 Select语句

### 2.1 `from` 表

Select 语句需要指定表名，通过 `from()` 方法来指定。该方法有2个参数 
1. `table` 表名
2. `alias`, 表别名，默认值是null

```
query.from("user") // 无别名
query.from("user", "u") // 有别名
query.from(DbExpr("user", "u")) // 等价于上一行
```

其中, `DbExpr` 类参考

### 2.2 `where` 条件

我们使用`where()`, `andWhere()` and `orWhere()`方法来过滤查询结果。这些方法需要3个参数： 1 字段名 2 操作符 3 字段值. 

```
query.from("user").where("username", "=", "john");
```

多次调用 `where()` 方法会构建多个过滤条件，条件之间用布尔操作符`AND`/`OR` （就是方法前缀）来连接. 其中 `where()` 方法只是简单调用了 `andWhere()`. 

```
query.from("user").where("username", "=", "john").orWhere("username", "=", "jane");
```

你可以使用任意的操作符，如 `IN`, `BETWEEN`, `>`, `=<`, `!=`...  

如果你使用的是多值操作符，则字段值需要传递数组`Array`或列表`List`

```
query.from("user").where("logins", "<=", 1);
query.from("user").where("logins", ">", 50);
query.from("user").where("username", "IN", arrayOf("john","mark","matt"));
query.from("user").where("joindate", "BETWEEN", arrayOf(then, now));
```

### 2.3 `select` 字段名

默认情况下，Select语句会select全部字段（`SELECT * ...`）, 但是你也可以通过调用`DbQueryBuilder::select()` 来指定返回的字段

```
query.select("username", "password").from("user").where("username", "=", "john");
```

我们来解释一下上面的链式方法调用。首先，我们调用`select()` 来指定返回的字段；其次，我们调用 `from()` 来指定要查询的表；最后，我们调用 `where()` 来搜索我们想要的数据。

生成的sql如下

```
SELECT `username`, `password` FROM `user` WHERE `username` = "john"
```

注意：字段名/表名/字段值被自动转义了，这就是使用 query builder 的一个主要好处。

#### 2.3.1 字段别名

如果你要在select字段时，指定字段别名，你需要在调用 `select()` 时，使用 `Pair<String, String>` 对象作为参数：

```
query.select(DbExpr(("username", "u"), DbExpr("password", "p")).from("user");
```

生成sql如下：

```
SELECT `username` AS `u`, `password` AS `p` FROM `user`
```

#### 2.3.2 `distinct` 不重复的返回值

如果你要控制返回的字段值是重复，还是不重复，你可以调用 `distinct(f: Boolean)` 并传递布尔参数 `f` 来控制 

```
query.select("username").distinct(true).from("posts");
```

生成sql如下：

```
SELECT DISTINCT `username` FROM `posts`
```

### 2.4 `LIMIT` 限制行数

有时候我们查询的表里有大量数据，但通常我们只需要某几行数据。此时通过调用 `limit(start:Int, offset:Int = 0)` 方法来限制返回某几行

```
query.from(`posts`).limit(10, 30);
```

生成sql如下：

```
SELECT * FROM `posts` LIMIT 10 OFFSET 30
```

### 2.5 `ORDER BY` 排序

你可以需要返回的数据按某个顺序来排列，如按时间倒序/按分数生序。 你可以调用 `orderBy(column:String, direction:String? = null)` 来实现排序，该方法需要2个参数： 1 排序字段名 2 排序方法（可选，默认为`ASC`）。多次调用 `orderBy()` 会追加多个排序字段。

```
query.from(`posts`).orderBy(`published`, `DESC`);
```

生成sql如下：

```
SELECT * FROM `posts` ORDER BY `published` DESC
```
### 2.6 执行sql，并获得结果

方法 | 作用
--- | ---
find(vararg params: Any?, transform: (Map<String, Any?>) -> T): T? | 查询一条记录，其中 `transform` 参数是一个lambda，用来将数据库的一行，转换为一条记录
find(vararg params: Any?): T? |  查询一条记录, 不需要`transform` 参数，但是依赖于返回类型来转换数据，同时返回类型只限定于以下3种类型： 1. `Map` 类 2. `IOrm` 的子类 3. 任意类型，只要有带 `Map` 参数的构造函数
findAll(vararg params: Any?, transform: (Map<String, Any?>) -> T): List<T> | 查询多条记录，其中 `transform` 参数是一个lambda，用来将数据库的一行，转换为一条记录
findAll(vararg params: Any?): List<T> | 查询多条记录, 不需要`transform` 参数，但是依赖于返回类型来转换数据，同时返回类型只限定于以下3种类型： 1. `Map` 类 2. `IOrm` 的子类 3. 任意类型，只要有带 `Map` 参数的构造函数
findColumn(vararg params: Any?): List<Any?> | 查询单列的多行数据
count(vararg params: Any?):Long | 查询行数

参考以下例子：

```
val query = DbQueryBuilder().from("user")
// SELECT * FROM `user`
val records = query.findAll<Record>()
// SELECT * FROM `user` LIMIT 1
val record = query.find<Record>()
// SELECT username FROM `user`
val usernames = query.select("username").findColumn<String>()
// SELECT count(1) FROM `user`
val count = query.count()
```

[!!] 详情请参考类 [DbQueryBuilder].

## 3 Insert 语句

如果你要往数据库中插入记录，则你需要调用 `table()` 来指定表名，调用 `insertColumns(vararg c:String)` 来指定要插入的字段名, 调用 `value(vararg v:Any?)` 来指定要插入的字段值, 最后调用 `insert(generatedColumn:String)` 来执行最终的insert sql:

```
val id:Long = DbQueryBuilder().table("user").insertColumns("username", "password").value("fred", "p@5sW0Rd").insert("id");
```

生成sql如下：

```
INSERT INTO `user` (`username`, `password`) VALUES ("fred", "p@5sW0Rd")
```

其中在 `insert("id")` 的调用中, 需要一个参数 `generatedColumn` 来指定插入记录后返回的字段值，一般我们需要的是自增的id

[!!] 详情请参考类 [DbQueryBuilder].

## 4 Update 语句

如果你要更新数据库中存在的记录，则你需要调用 `table()` 来指定表名，调用 `set(column: String, value: Any?)` 来指定要更新的字段名与字段值, 最后调用 `udpate()` 来执行最终的update sql:

```
val f:Boolean = DbQueryBuilder().table("user").set("username", "jane").where("username", "=", "john").update();
```

[!!] 详情请参考类 [DbQueryBuilder].

## 5 Delete 语句

如果你要删除数据库中的记录，你需要调用 `table()` 来指定表名,调用 `delete()` 来执行最终的delete sql：

```
val f = DbQueryBuilder().table("user").where("username", "IN", arrayOf("john", "jane")).delete();
```

生成sql如下：

```
DELETE FROM `user` WHERE `username` IN ("john", "jane")
```

[!!] 详情请参考类 [DbQueryBuilder].

## 6 高级查询

### 6.1 `Join` 语句

如果你要联查多个表，则你需要使用 `join()` 与 `on()` 方法. 

`join()` 需要2个参数
* 表名：可以是 `String` 或 `Pair<String, String>` （包含表名+表别名）
* 连接方式: LEFT（左连接）, RIGHT（右连接）, INNER（内连接）.

`on()` 方法主要用于设置2个关联表的连接条件，与 `where()` 方法类似，但它需要3个参数; 1 左字段名 2 符号 3 右字段名. 多次调用 `on()` 方法会构建多个连接条件，条件之间用 "AND" 操作符来连接

```
// 使用`JOIN` 来查询出作者 "smith" 关联的所有文章
query.select("authors.name", "posts.content").from("authors").join("posts").on("authors.id", "=", "posts.author_id").where("authors.name", "=", "smith").findAll<Record>();
```

生成sql如下：

```
SELECT `authors`.`name`, `posts`.`content` FROM `authors` JOIN `posts` ON (`authors`.`id` = `posts`.`author_id`) WHERE `authors`.`name` = "smith"
```

如果你要使用不同的连接方式（LEFT / RIGHT / INNER），你只需要调用 `join("columName", "joinType")`，就是用第二个参数来指定连接方式：

```
// 使用`LEFT JOIN` 来查询出作者 "smith" 关联的所有文章
query.from("authors").join("posts", "LEFT").on("authors.id", "=", "posts.author_id").where("authors.name", "=", "smith");
```

生成sql如下：

```
SELECT `authors`.`name`, `posts`.`content` FROM `authors` LEFT JOIN `posts` ON (`authors`.`id` = `posts`.`author_id`) WHERE `authors`.`name` = "smith"
```

[!!] 如果你联查的多个表中存在同名的字段，则你在指定返回字段时，最好加上表前缀，来避免sql执行异常. 如果遇到`未明确定义的列（Ambiguous column name）`的错误时，你需要给字段加上表前缀，或者字段别名。

### 6.2 聚合函数

SQL中提供的聚合函数可以用来统计、求和、求最值等，如 `COUNT()`, `SUM()`, `AVG()`. 他们通常是结合 `groupBy()` 来分组统计，或结合 `having()` 来过滤聚合结果

```
query.select("username", DbExpr("COUNT(`id`)", "total_posts", false)).from("posts").groupBy("username").having("total_posts", ">=", 10).findAll<Record>()
```

生成sql如下：

```
SELECT `username`, COUNT(`id`) AS `total_posts` FROM `posts` GROUP BY `username` HAVING `total_posts` >= 10
```

### 6.3 子查询

查询构建器对象可以作为很多方法的参数，来构建子查询。让我们用上面的查询，传给新的查询作为子查询：

```
// subquery
val sub = DbQueryBuilder().select("username", DbExpr("COUNT(`id`)", "total_posts", false))
        .from("posts").groupBy("username").having("total_posts", ">=", 10);

// join subquery
DbQueryBuilder().select("profiles.*", "posts.total_posts").from("profiles")
.joins(DbExpr(sub, "posts", false), "INNER").on("profiles.username", "=", "posts.username").findAll<Record>()
```

生成sql如下：

```
SELECT `profiles`.*, `posts`.`total_posts` FROM `profiles` INNER JOIN
( SELECT `username`, COUNT(`id`) AS `total_posts` FROM `posts` GROUP BY `username` HAVING `total_posts` >= 10 ) `posts`
ON `profiles`.`username` = `posts`.`username`
```

Insert 查询也可以接入 Select 子查询

```
// subquery
val sub = DbQueryBuilder().select("username", DbExpr("COUNT(`id`)", "total_posts", false))
.from("posts").groupBy("username").having("total_posts", ">=", 10);

// insert subquery
DbQueryBuilder().table("post_totals").insertColumns("username", "posts").values(sub).insert()
```

This will generate the following query:

```
INSERT INTO `post_totals` (`username`, `posts`) 
SELECT `username`, COUNT(`id`) AS `total_posts` FROM `posts` GROUP BY `username` HAVING `total_posts` >= 10 
```

### 6.4 布尔操作符与嵌套子句

多个 `WHERE` 与 `HAVING` 子句是用布尔操作符（`AND`/`OR`）来连接的。无前缀或前缀为 `and` 的方法的操作符是`AND`. 前缀为 `or` 的方法的操作符是`OR`. `WHERE` 与 `HAVING` 子句可以嵌套使用，你可以使用后缀为`open` 的方法来开启一个分组，使用后缀为 `close` 的方法来关闭一个分组. 

```
query.from("user")
    .whereOpen()
        .where("id", "IN", arrayOf(1, 2, 3, 5))
        .andWhereOpen()
            .where("lastLogin", "<=", System.currentTimeMillis() / 1000)
            .orWhere("lastLogin", "IS", null)
        .andWhereClose()
    .whereClose()
    .andWhere("removed","IS", null)
    .findAll<Record>()
```

生成sql如下：

```
SELECT  * FROM `user` WHERE ( `id` IN (1, 2, 3, 5)  AND ( `lastLogin` <= 1511069644 OR `lastLogin` IS null )) AND `removed` IS null
```

### 6.5 数据库表达式

在 `DbQueryBuilder` 的 `insert/update` 语句中，要保存的字段值总是要被转义 `Db::quote(value:Any?)`。但是有时候字段值是一个原生的表达式与函数调用，此时是不需要转义的。

因此，我们需要数据库表达式 `DbExpression`，用于添加不转义的字段值，表示要保存的字段值是一个sql表达式，如 now() / column1 + 1

```
DbQueryBuilder().table("user")
    .set("login_count", DbExpression("login_count + 1")) // 等价于 .set("login_count", "login_count + 1", true)
    .where("id", "=", 45)
    .update();
```

生成sql如下：

```
UPDATE `user` SET `login_count` = `login_count` + 1 WHERE `id` = 45
```

[!!] 你必须要事先保证：创建`DbExpression(input:String)` 时传递的表达式是有效并且已转义过的。

## 7 例子

```
// 获得 Db 对象
val db: Db = Db.instance()

// 开启事务
db.transaction {
    // 插入
    var id = DbQueryBuilder(db).table("user").insertColumns("name", "age").value("shi", 1).insert("id");
    println("插入user表：" + id)

    // 查询一条数据
    val record = DbQueryBuilder(db).table("user").where("id", "=", id).find<Record>()
    println("查询user表：" + record)

    // 更新
    var f = DbQueryBuilder(db).table("user").sets(mapOf("name" to "wang", "age" to 2)).where("id", "=", id).update();
    println("更新user表：" + f)

    // 查询多条数据
    val records = DbQueryBuilder(db).table("user").orderBy("id").limit(1).findAll<Record>()
    println("查询user表：" + records)

    // 删除
    f = DbQueryBuilder(db).table("user").where("id", "=", id).delete();
    println("删除user表：" + f)
}
```