# Manipulate database

## 1 Configure database connection

vim src/main/resources/database.yaml

```
#  database name
default:
    driverClass: com.mysql.jdbc.Driver
    url: jdbc:mysql://127.0.0.1/test?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root
    # column's naming conventions: underline
    columnUnderline: true
    # column's naming conventions: all upperCase
    columnUpperCase: false
```

You can configure multiple database connections, as long as you use a different database name.

## 2 Get `Db` object

Database manipulation class is `com.jkmvc.db.Db`, there are 2 usage
1. Manage database connections
2. Execute sql

Use `Db::instance(name:String = "default"):Db` to get `Db` object, it will get the configuration item corresponding to the `name` in file `database.yaml`, and establish a database connection.

Just like:

```
// get `Db` object
val db = Db.instance();
```

## 3 Use the `Db` object to execute sql

Let's take a look at `Db` class's properties and methods

### 3.1 Metadata related properties and methods

Property / Method | Function
--- --- --- ---
dbType: DbType | Get the database type by driverClass
listColumns (table: String): List <String> | Get all the columns of the table
close (): Unit | Close database connection

### 3.2 Transaction-related methods

Method | Function
--- --- --- ---
begin (): Unit | Begin a transaction
commit (): Boolean | Commit the transaction
rollback (): Boolean | Rollbak the transaction
transaction (statement: Db. () -> T): T | Executes the transaction, encapsulating the transaction code with `begin()` / `commit()` / `rollback()`
isInTransaction (): Boolean | Check whether in a transaction

### 3.3 Update-sql executing method

Method | Function
--- --- --- ---
execute (sql: String, params: List <Any?>? = null, generatedColumn: String? = null): Int | Execute a update-sql
batchExecute (sql: String, paramses: List <Any?>, paramSize: Int): IntArray | Batch update

### 3.4 Query-sql executing method

Method | Function
--- --- --- ---
queryCell (sql: String, params: List <Any?>? = null): Pair <Boolean, Any?> | Query a cell in a row
queryColumn (sql: String, params: List <Any?>?): List <Any?> | Query a column in multiple rows
queryResult (sql: String, params: List <Any?>? = null, action: (ResultSet) -> T): T | Query and get result with lambda
queryRow (sql: String, params: List <Any?>? = null, transform: (MutableMap <String, Any?>) -> T) | Query one row
queryRows (sql: String, params: List <Any?>? = null, transform: (MutableMap <String, Any?>) -> T): List <T> | Query multiple rows

### 3.5 Quote / Preview sql methods

Property / Method | Function
--- --- --- ---
previewSql (sql: String, params: List <Any?>? = null): String | preview sql
quote (value: Any?): String | quote value
quoteColumn (column: Pair <String, String>, with_brackets: Boolean = false): String | Quote column name: ``column`` for mysql, `"column"` for oracle, `"column"` for sql server
quoteColumn (column: String, alias: String? = null, with_brackets: Boolean = false): String | quoteColumn (column: Any): String | Quote column name
quoteColumns (columns: Collection <Any>, with_brackets: Boolean = false): String | Quote multiple column names
quoteTable (table: String, alias: String? = null): String | quoteTable (string: String? String): String | Quote table name: ``table`` for mysql, `"table"` for oracle, `"table"` for sql server
quoteTable (table: Any): String | Quoted table name
quoteTables (tables: Collection <Any>, with_brackets: Boolean = false): String | Quote multiple table names

### 3.6 Database field and object property name-transforming methods, used in the model

Property / Method | Function
--- --- --- ---
column2Prop (column: String): String | Get the object property name according the db field name
prop2Column (prop: String): String | Get the db property name according the object property name

## 4 Example

```
// get `Db` object
val db: Db = Db.instance()

// begin a transaction
db.transaction {
    // create a table
    db.execute("""
        CREATE TABLE IF NOT EXISTS `user` (
            `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '用户编号',
            `name` varchar(50) NOT NULL DEFAULT '' COMMENT '用户名',
            `age` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '年龄',
            `avatar` varchar(250) DEFAULT NULL COMMENT '头像',
            PRIMARY KEY (`id`)
        )ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户';
        """);

    // insert
    // val id = db.execute("insert into user(name, age) values(?, ?)" /*sql*/, listOf("shi", 1)/*sql paramters*/, "id"/*auto-increment id, as the return value*/) // return the generated id
    println("insert a user：" + id)

    // select single row
    val record = db.queryRow("select * from user limit 1" /*sql*/, null /*sql parameters*/, Map::class.recordTranformer /*transfrom lambda*/) // return a row as `Map` object
    println("select a user：" + record)

    // count
    val (hasNext, count) = db.queryCell("select count(1) from user" /*sql*/)
    println("count users: " + count)

    // update
    var f = db.execute("update user set name = ?, age = ? where id =?" /*sql*/, listOf("shi", 1, id) /*sql parameters*/) // return the updated rows count
    println("update a user：" + f)

    // select multiple rows
    val records = db.queryRows("select * from user limit 10" /*sql*/, null /*sql parameters*/, Map::class.recordTranformer /*transfrom lambda*/) // 返回 Map 类型的多行数据
    println("select multiple users: " + records)

    // delete
    f = db.execute("delete from user where id =?" /*sql*/, listOf(id) /*sql parameters*/) // return the deleted rows count
    println("delete a user：" + f)
}
```