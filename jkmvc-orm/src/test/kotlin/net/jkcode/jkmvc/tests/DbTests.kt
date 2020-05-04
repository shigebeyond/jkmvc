package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.db.*
import net.jkcode.jkutil.common.randomString
import net.jkcode.jkmvc.orm.DbKey
import net.jkcode.jkmvc.orm.PkEmptyRule
import org.junit.Test

class DbTests{

    val db: Db = Db.instance()

    val id: Int by lazy {
        val minId = db.queryValue<Int>("select id from user order by id limit 1" /*sql*/)!!
        println("随便选个id: " + minId)
        minId
    }

    @Test
    fun testPkEmptyRule(){
        val er = PkEmptyRule()
        println(er.rule)
        er.allow(0)
        println(er.rule)
        println(er.isAllow(1))
        er.allow(1)
        println(er.rule)
        println(er.isAllow(1))
    }

    @Test
    fun testDbMeta(){
        println("database type: " + db.dbType)
        println("database schema: " + db.schema)
        println("database table: " + db.tables.keys)

        //val table = db.tables.keys.first()
        val table = "xxxx"
        println("table [$table] 's columns: ")
        println(db.queryColumnsByTable(table))
        println("table [$table] 's primary keys: ")
        println(db.queryPrimaryKeysByTable(table))
    }

    @Test
    fun testDbKey(){
        val k1 = DbKey("a")
        val k2 = DbKey("a")
        println(k1 == k2) // false
        println(k1.columns.equals(k2.columns)) // false
        println(k1.columns.contentEquals(k2.columns)) // true
        println(k1.columns.first() == k2.columns.first()) // true
    }


    @Test
    fun testDefineTable(){
        val table = DbTable("user")
        val id = DbColumn("id", DbColumnLogicType.INT, null,11, 2, null, false, "主键", true)
        val name = DbColumn("name", DbColumnLogicType.VARCHAR, null,10, null, "''")
        val date = DbColumn("date", DbColumnLogicType.DATE, null,null, null, "null")
        table.addClumn(id)
        table.addClumn(name)
        table.addClumn(date)
        table.primaryKeys = listOf("id")
        println(table.generateCreateTableSql(Db.instance()))
    }

    @Test
    fun testFunction(){
        val db = Db.instance()
        // 确定参数
        println(DbFunction.toPhysicalFunction(db, "cast", listOf("xxx", "varchar")))
        println(DbFunction.toPhysicalQuery(db, "select cast(xxx as varchar)"));

        // 不定参数: concat在oracle/sql server测试才有效, 他才是非函数调用的格式
        println(DbFunction.toPhysicalFunction(db, "concat", listOf("xxx", "yyy")))
        println(DbFunction.toPhysicalQuery(db, "select concat(xxx, yyy)"));
    }

    @Test
    fun testColumn2Prop(){
        println(db.column2Prop("ZDZCBH_"))
    }

    @Test
    fun testConnection(){
        val file = "test." + db.dbType.toString().toLowerCase() + ".sql"
        val cld = Thread.currentThread().contextClassLoader
        val res = cld.getResource(file)
        var text = res.readText()
        // 去掉注释
        text = text.replace("^\\s*--\\s*$".toRegex(), "")
        // 分隔多条sql
        val sqls = text.split(";")
        for(sql in sqls)
            if(!sql.isBlank())
                db.execute(sql);
        println("创建表")
    }

    @Test
    fun testResultSet(){
        Db.instance().queryResult("select * from user"){
            if(it.next()) {
                val id = it.getInt("id")
                println(id)
            }else
                println("没有记录")
        }
    }

    @Test
    fun testPreviewSql(){
        println(db.previewSql("select * from user where name = ?", listOf("shi")))
    }

    @Test
    fun testInsert(){
        val id = db.execute("insert into user(name, age) values(?, ?)" /*sql*/, listOf("shi", 1)/*参数*/, "id"/*自增主键字段名，作为返回值*/) // 返回自增主键值
        println("插入user表：" + id)
    }

    @Test
    fun testFind(){
        //val row = db.queryMap("select * from user limit 1" /*sql*/, emptyList<Any>() /*参数*/, true /* 是否转换字段名 */) // 返回 Map 类型的一行数据
        val row = db.queryMap("select * from user where ? between age and id limit 1" /*sql*/, listOf(3) /*参数*/, true /* 是否转换字段名 */) // 返回 Map 类型的一行数据
        println("查询user表：" + row)
    }

    @Test
    fun testFindAll(){
        val rows = db.queryMaps("select * from user limit 10" /*sql*/, emptyList<Any>() /*参数*/, true /* 是否转换字段名 */) // 返回 Map 类型的多行数据
        println("查询user表：" + rows)
    }

    @Test
    fun testCount(){
        val count = db.queryValue<Int>("select count(1) from user" /*sql*/)!!
        println("统计user表：" + count)
    }

    @Test
    fun testUpdate(){
        val f = db.execute("update user set name = ?, age = ? where id =?" /*sql*/, listOf(randomString(5), true, id) /*参数*/) // 返回更新行数
        println("更新user表：" + f)
    }

    @Test
    fun testDelete(){
        val f = db.execute("delete from user where id =?" /*sql*/, listOf(id) /*参数*/) // 返回更新行数
        println("删除user表：" + f)
    }
}