#  sql构建器

sql构建器是通过提供类sql的一系列方法，来帮助开发者快速构建sql。

Jkmvc提供了2个层次的sql构建器
1. `com.jkmvc.db.DbQueryBuilder` 
2. `com.jkmvc.orm.OrmQueryBuilder`

# 1 



## 4 例子

```
// 获得 Db 对象
val db: Db = Db.getDb()

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