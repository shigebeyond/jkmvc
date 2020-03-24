# 数据库操作

## 1 数据库配置, 支持读写分离

vim src/main/resources/dataSources.yaml

```
# 数据库名
default:
  # 主库
  master:
    driverClassName: com.mysql.jdbc.Driver
    url: jdbc:mysql://127.0.0.1/test?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root
  # 多个从库, 可省略
  slaves:
    -
      driverClassName: com.mysql.jdbc.Driver
      url: jdbc:mysql://127.0.0.1/test?useUnicode=true&characterEncoding=utf-8
      username: root
      password: root
```

你可以配置多个数据库，只要使用不同的数据库名就行


## 2 获得数据库操作对象

数据库操作类是 `net.jkcode.jkmvc.db.Db`，主要有2个作用
1. 管理数据库连接
2. 执行sql

使用 `Db::instance(name:String = "default"):Db` 来获得 Db 对象，他会找到配置文件 `dataSources.yaml` 中对应 name 的连接配置，并建立连接

例子

```
// 获得 Db 对象
val db = Db.instance();
```

## 3 使用 Db 对象来执行sql

我们先来看看 Db 类的属性与方法

### 3.1 元数据相关的属性与方法

属性/方法 | 作用
--- | ---
dbType: DbType | 获得数据库类型 根据driverClass来获得
listColumns(table: String): List<String> | 获得表的所有列
close(): Unit | 关闭

### 3.2 事务相关的方法

方法 | 作用
--- | ---
begin(): Unit | 开启事务
commit(): Boolean | 提交事务
rollback(): Boolean | 回滚事务
transaction(statement: Db.() -> T): T | 执行事务，封装了事务的开启/提交与回滚的通过逻辑
isInTransaction(): Boolean | 是否在事务中

### 3.3 执行更新sql的方法

方法 | 作用
--- | ---
execute(sql: String, params: List<*> = emptyList<Any>(), generatedColumn: String? = null): Long | 执行更新
batchExecute(sql: String, paramses: List<Any?>): IntArray | 批量更新: 每次更新sql参数不一样

### 3.4 执行查询sql的方法

1. 需要转换的底层方法

方法 | 作用
--- | ---
queryResult(sql: String, params: List<*> = emptyList<Any>(), transform: (DbResultSet) -> T): T | 查询多行
queryRows(sql: String, params: List<*> = emptyList<Any>(), transform: (DbResultRow) -> T): List<T> | 查询多行
queryRow(sql: String, params: List<*> = emptyList<Any>(), transform: (DbResultRow) -> T): T? | 查询一行(多列)
queryColumn(sql: String, params: List<*> = emptyList<Any>(), clazz: KClass<T>? = null): List<T?> | 查询一列(多行)
inline queryColumn(sql: String, params: List<*> = emptyList<Any>()): List<T?> | 查询一列(多行), 内联省了最后一个参数
queryValue(sql: String, params: List<*> = emptyList<Any>(), clazz: KClass<T>? = null): T? | 查询一行一列
inline queryValue(sql: String, params: List<*> = emptyList<Any>()): T? | 查询一行一列, 内联省了最后一个参数

2. 自动转换的高层方法

方法 | 作用
--- | ---
queryMaps(sql: String, params: List<*> = emptyList<Any>(), convertingColumn: Boolean): List<Map<String, Any?>> | 查询多行, 并将每行转为 `Map`
queryMap(sql: String, params: List<*> = emptyList<Any>(), convertingColumn: Boolean): Map<String, Any?>? | 查询一行, 并转为 `Map`

### 3.5 转义与预览的方法

属性/方法 | 作用
--- | ---
previewSql(sql: String, params: List<*> = emptyList<Any>()): String | 预览sql
quote(value: Any?): String | 转义值
quoteColumn(column: CharSequence): String | 转义字段名
quoteTable(table: CharSequence): String | 转义表名

### 3.6 数据库字段与对象属性名互转的方法，主要用在 model 中

属性/方法 | 作用
--- | ---
column2Prop(column: String): String | 根据db字段名，获得对象属性名
prop2Column(prop: String): String | 根据对象属性名，获得db字段名

## 4 例子

```
// 获得 Db 对象
val db: Db = Db.instance()

// 开启事务
db.transaction {
    // 建表
    db.execute("""
        CREATE TABLE IF NOT EXISTS `user` (
            `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '用户编号',
            `name` varchar(50) NOT NULL DEFAULT '' COMMENT '用户名',
            `age` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '年龄',
            `avatar` varchar(250) DEFAULT NULL COMMENT '头像',
            PRIMARY KEY (`id`)
        )ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户';
        """);

    // 查询
    // val id = db.execute("insert into user(name, age) values(?, ?)" /*sql*/, listOf("shi", 1)/*参数*/, "id"/*自增主键字段名，作为返回值*/) // 返回自增主键值
    println("插入user表：" + id)

    // 查询一条数据
    val row = db.queryRow("select * from user limit 1" /*sql*/, emptyList<Any>() /*参数*/, true /* 将字段名转为属性名, 如 to_uid => toUid */) // 返回 Map 类型的一行数据
    println("查询user表：" + row)

    // 统计行数
    val count = db.queryValue<Int>("select count(1) from user" /*sql*/).get()!!
    println("统计user表：" + count)

    // 更新
    var f = db.execute("update user set name = ?, age = ? where id =?" /*sql*/, listOf("shi", 1, id) /*参数*/) // 返回更新行数
    println("更新user表：" + f)

    // 查询多条数据
    val rows = db.queryRows("select * from user limit 10" /*sql*/, emptyList<Any>() /*参数*/, true /* 将字段名转为属性名, 如 to_uid => toUid */) // 返回 Map 类型的多行数据
    println("查询user表：" + rows)

    // 删除 
    f = db.execute("delete from user where id =?" /*sql*/, listOf(id) /*参数*/) // 返回更新行数
    println("删除user表：" + f)
}
```