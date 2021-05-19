# Query Builder

Query Builder is a sql builder. It provides methods similar to sql syntax, and generates real sql adapted to  different databases(eg. mysql/oracle).

## 1 Create query builder

Just new a `DbQueryBuilder` object, and takes `Db` object as first parameterr. 

```
val query = DbQueryBuilder(Db.instance())
val query = DbQueryBuilder() // first parameter'sdefalut value is Db.instance() 
```

## 2 Select

### 2.1 from table

Select queries ussually require a table and they are referenced using the `from()` method. The `from()` method takes 2 parameters 
1. table name
2. table alias, default is null

```
query.from("user", "u")
query.from("user")
```

### 2.2 where condition

Limiting the results of queries is done using the `where()`, `andWhere()` and `orWhere()` methods. These methods take three parameters: a column, an operator, and a value. 

```
query.from("user").where("username", "=", "john");
```

Multiple `where()` methods may be used to string together multiple clauses connected by the boolean operator `AND`/`OR` in the method'sprefix. The `where()` method is a wrapper that just calls `andWhere()`. 

```
query.from("user").where("username", "=", "john").orWhere("username", "=", "jane");
```

You can use any operator you want.  Examples include `IN`, `BETWEEN`, `>`, `=<`, `!=`, etc.  Use an array for operators that require more than one value.

```
query.from("user").where("logins", "<=", 1);
query.from("user").where("logins", ">", 50);
query.from("user").where("username", "IN", arrayOf("john","mark","matt"));
query.from("user").where("joindate", "BETWEEN", Pair(then, now)); // equals next code
query.from("user").whereBetween("joindate", then, now);
```

### 2.3 select columns

By default, select clause will select all columns (`SELECT * ...`), but you can also specify which columns you want returned by passing parameters to `DbQueryBuilder::select`

```
query.select("username", "password").from("user").where("username", "=", "john");
```

Now take a minute to look at what this method chain is doing. First, we select colums using `select()` method. Next, we set table(s) using the `from()` method. Last, we search for a specific rows using the `where()` method. 

And it will generate following sql:

```
SELECT `username`, `password` FROM `user` WHERE `username` = "john"
```

Notice that the column and table names are automatically escaped, as well as the values. This is one of the key benefits of using the query builder. 

#### 2.3.1 Select column AS aliase

It is also possible to create `AS` aliases when selecting, by passing a `Pair<String, String>` object as each parameter to `select()`:

```
query.select(DbExpr("username", "u"), DbExpr("password", "p")).from("user");
```

This query would generate the following SQL:

```
SELECT `username` AS `u`, `password` AS `p` FROM `user`
```

#### 2.3.2 Select distinct columns

Unique column values may be turned on or off (default) by passing true or false, respectively, to the `distinct()` method.

```
query.select("username").distinct(true).from("posts");
```

This query would generate the following SQL:

```
SELECT DISTINCT `username` FROM `posts`
```

### 2.4 LIMIT

When querying large sets of data, it is often better to limit the results and page through the data one chunk at a time. This is done using the `limit(start:Int, offset:Int = 0)` methods.

```
query.from("posts").limit(10, 30);
```

This query would generate the following SQL:

```
SELECT * FROM `posts` LIMIT 10 OFFSET 30
```

### 2.5 ORDER BY

Often you will want the results in a particular order and rather than sorting the results, it's better to have the results returned to you in the correct order. You can do this by using the `orderBy(column:String, direction:String? = null)` method. It takes the column name and an optional direction string as the parameters. Multiple `orderBy()` methods can be used to add additional sorting capability.

```
query.from("posts").orderBy(`published`, `DESC`);
```

This query would generate the following SQL:

```
SELECT * FROM `posts` ORDER BY `published` DESC
```
### 2.6 Execute sql and get result

1. Low level method, which needs `transform`

method | usage
--- | ---
findResult(params: List<*>, db: IDb, transform: (DbResultSet) -> T): T? | find result set，the `transform` parameter is a lambda which transforms result set into target class' object
findRow(params: List<*>, db: IDb, transform: (DbResultRow) -> T): T? | find single row, the `transform` parameter is a lambda which transforms db row to result row
findRows(params: List<*>, db: IDb, transform: (DbResultRow) -> T): List<T> | find multiple rows, the `transform` parameter is a lambda which transforms db row to result row
findColumn(params: List<*>, clazz: KClass<T>?, db: IDb): List<T> | find multiple row in single column
inline findColumn(params: List<*>, db: IDb): List<T> | find multiple row in single column, `inline` saves the a parameter
findValue(params: List<*>, clazz: KClass<T>?, db: IDb): T? | find a value in a row
inline findValue(params: List<*>, db: IDb): T? | find a value in a row, `inline` saves a parameter

2. High level method, which auto transform to target class's object

method | usage
--- | ---
findMap(params: List<*>, convertingColumn: Boolean, db: IDb): T? |  find single `Map` object, it needs no `transform` parameter, but depends on the return type for transforming
findMaps(params: List<*>, convertingColumn: Boolean, db: IDb): List<T> | find multiple `Map` object, it needs no `transform` parameter, but depends on the return type for transforming
inline findModel(params: List<*>, db: IDb): T? |  find single `Orm` object, it needs no `transform` parameter, but depends on the return type for transforming
inline findModels(params: List<*>, db: IDb): List<T> | find multiple `Orm` object, it needs no `transform` parameter, but depends on the return type for transforming
inline findEntity(params: List<*>, db: IDb): T? | find single `OrmEntity` object, it needs no `transform` parameter, but depends on the return type for transforming
inline findEntities(params: List<*>, db: IDb): List<T> | find multiple `OrmEntity` object, it needs no `transform` parameter, but depends on the return type for transforming
count(params: List<*>, db: IDb):Long | count rows


There are some examples:

```
val query = DbQueryBuilder().from("user")
// SELECT * FROM `user`
val rows = query.findMaps()
// SELECT * FROM `user` LIMIT 1
val row = query.findMap()
// SELECT username FROM `user`
val usernames = query.select("username").findColumn<String>()
// SELECT count(1) FROM `user`
val count = query.count()
```

[!!] For a complete list of methods available while building a select query see [DbQueryBuilder].

## 3 Insert

To create rows into the database, use `table()` to pass table, using `insertColumns(vararg c:String)` to pass columns, using `value(vararg v:Any?)` to pass data, using `insert(generatedColumn:String)` to execute insert sql:

```
val id:Long = DbQueryBuilder().table("user").insertColumns("username", "password").value("fred", "p@5sW0Rd").insert("id");
```

This query would generate the following SQL:

```
INSERT INTO `user` (`username`, `password`) VALUES ("fred", "p@5sW0Rd")
```

And in `insert("id")`, we set `generatedColumn` parameter indicating the columns that should be returned from the inserted row or rows

So the return value is auto-generated id. 

[!!] For a complete list of methods available while building an insert query see [DbQueryBuilder].

## 4 Update

To modify an existing row, use `table()` to pass table, using `set()` to pass data, using `udpate()` to execute update sql:

```
val f:Boolean = DbQueryBuilder().table("user").set("username", "jane").where("username", "=", "john").update();
```

[!!] For a complete list of methods available while building an update query see [DbQueryBuilder].

## 5 Delete

To remove an existing row, use `table()` to pass table,using `delete()` to execute delete sql:

```
val f = DbQueryBuilder().table("user").where("username", "IN", arrayOf("john", "jane")).delete();
```

This query would generate the following SQL:

```
DELETE FROM `user` WHERE `username` IN ("john", "jane")
```

[!!] For a complete list of methods available while building a delete query see [DbQueryBuilder].

## 6 Advanced Queries

### 6.1 Joins

Multiple tables can be joined using the `join()` and `on()` methods. 

The `join()` method takes two parameters. 
* The first is either a table name, an `Pair<String, String>` object containing the table and alias. 
* The second parameter is the join type: LEFT, RIGHT, INNER, etc.

The `on()` method sets the conditions for the previous `join()` method and is very similar to the `where()` method in that it takes three parameters; 1. left column (name or DbExpr) 2. an operator 3. the right column (name or DbExpr). Multiple `on()` methods may be used to supply multiple conditions and they will be appended with an "AND" operator.

```
// This query will find all the posts related to "smith" with JOIN
query.select("authors.name", "posts.content").from("authors").join("posts").on("authors.id", "=", "posts.author_id").where("authors.name", "=", "smith").findMaps();
```

This query would generate the following SQL:

```
SELECT `authors`.`name`, `posts`.`content` FROM `authors` JOIN `posts` ON (`authors`.`id` = `posts`.`author_id`) WHERE `authors`.`name` = "smith"
```

If you want to do a LEFT, RIGHT or INNER JOIN you would do it like this `join("columName", "joinType")`:

```
// This query will find all the posts related to "smith" with LEFT JOIN
query.from("authors").join("posts", "LEFT").on("authors.id", "=", "posts.author_id").where("authors.name", "=", "smith").findMaps();
```

This query would generate the following SQL:

```
SELECT `authors`.`name`, `posts`.`content` FROM `authors` LEFT JOIN `posts` ON (`authors`.`id` = `posts`.`author_id`) WHERE `authors`.`name` = "smith"
```

[!!] When joining multiple tables with similar column names, it's best to prefix the columns with the table name or table alias to avoid errors. Ambiguous column names should also be aliased so that they can be referenced easier.

### 6.2 Aggregate Functions

Aggregate functions like `COUNT()`, `SUM()`, `AVG()`, etc. will most likely be used with the `groupBy()` and possibly the `having()` methods in order to group and filter the results on a set of columns.

```
query.select("username", DbExpr("COUNT(`id`)", "total_posts", false)).from("posts").groupBy("username").having("total_posts", ">=", 10).findMaps();
```

This will generate the following query:

```
SELECT `username`, COUNT(`id`) AS `total_posts` FROM `posts` GROUP BY `username` HAVING `total_posts` >= 10
```

### 6.3 Subqueries

Query Builder objects can be passed as parameters to many of the methods to create subqueries. Let's take the previous example query and pass it to a new query.

```
// subquery
val sub = DbQueryBuilder().select("username", DbExpr("COUNT(`id`)", "total_posts", false))
        .from("posts").groupBy("username").having("total_posts", ">=", 10)

// join subquery
DbQueryBuilder().select("profiles.*", "posts.total_posts").from("profiles")
.join(DbExpr(sub, "posts", false), "INNER").on("profiles.username", "=", "posts.username").findMaps()
```

This will generate the following query:

```
SELECT `profiles`.*, `posts`.`total_posts` FROM `profiles` INNER JOIN
( SELECT `username`, COUNT(`id`) AS `total_posts` FROM `posts` GROUP BY `username` HAVING `total_posts` >= 10 ) `posts`
ON `profiles`.`username` = `posts`.`username`
```

Insert queries can also use a select query for the input values

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

### 6.4 Boolean Operators and Nested Clauses 

Multiple Where and Having clauses are added to the query with Boolean operators connecting each expression. The default operator for both methods is `AND` which is the same as the `and` prefixed method. The `OR` operator can be specified by prefixing the methods with `or`. Where and Having clauses can be nested or grouped by post fixing either method with `open` and then followed by a method with a `close`. 

```
query.from("user")
    .whereOpen()
        .where("id", "IN", arrayOf(1, 2, 3, 5))
        .andWhereOpen()
            .where("lastLogin", "<=", System.currentTimeMillis() / 1000)
            .orWhere("lastLogin", "IS", null)
        .andWhereClose()
    .whereClose()
    .andWhere("removed","IS", null);
```

This will generate the following query:

```
SELECT  * FROM `user` WHERE ( `id` IN (1, 2, 3, 5)  AND ( `lastLogin` <= 1511069644 OR `lastLogin` IS null )) AND `removed` IS null
```

### 6.5 Database Expressions

In `update/insert` Query Builder, the column value is always be escaped by `Db::quote(value:Any?)`. But there are cases were you need a complex expression or other database functions, which you don't want the Query Builder to try and escape. In these cases, you will need to use a database expression `DbExpr`.  **A database expression is taken as direct input and no escaping is performed.**

```
DbQueryBuilder().table("user")
    .set("login_count", DbExpr("login_count + 1")) // Equals: .set("login_count", "login_count + 1", true)
    .where("id", "=", 45)
    .update();
```

This will generate the following query, assuming `$id = 45`:

```
UPDATE `user` SET `login_count` = `login_count` + 1 WHERE `id` = 45
```

[!!] You must validate or escape any user input inside of `DbExpr(input:String)` as it will obviously not be escaped it for you.

## 7 Example

```
// Get Db object
val db: Db = Db.instance()

// start transaction
db.transaction {
    // insert
    var id = DbQueryBuilder(db).table("user").insertColumns("name", "age").value("shi", 1).insert("id");
    println("insert into user: " + id)

    // query a row
    val row = DbQueryBuilder(db).table("user").where("id", "=", id).findMap()
    println("query user: " + row)

    // update
    var f = DbQueryBuilder(db).table("user").sets(mapOf("name" to "wang", "age" to 2)).where("id", "=", id).update();
    println("update user: " + f)

    // query multiple rows
    val rows = DbQueryBuilder(db).table("user").orderBy("id").limit(1).findMaps()
    println("query user: " + rows)

    // delete
    f = DbQueryBuilder(db).table("user").where("id", "=", id).delete();
    println("delete user: " + f)
}
```