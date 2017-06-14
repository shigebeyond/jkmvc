package com.jkmvc.tests

import com.jkmvc.db.*
import org.junit.Test


class DbTests{

    val db: Db = Db.getDb()

    val id: Long by lazy {
        val (hasNext, minId) = db.queryCell("select id from user order by id limit 1")
        println("随便选个id: " + minId)
        minId as Long;
    }

    @Test
    fun testConnection(){
        db.execute("""
        CREATE TABLE IF NOT EXISTS `user` (
          `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '用户编号',
          `name` varchar(50) NOT NULL DEFAULT '' COMMENT '用户名',
          `age` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '年龄',
          PRIMARY KEY (`id`)
        )ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户';
        """);
        println("创建user表")
        db.execute("""
        CREATE TABLE IF NOT EXISTS `address` (
          `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '地址编号',
          `user_id` int(11) unsigned NOT NULL COMMENT '用户编号',
          `addr` varchar(50) NOT NULL DEFAULT '' COMMENT '地址',
          `tel` varchar(50) NOT NULL DEFAULT '' COMMENT '电话',
          PRIMARY KEY (`id`)
        ) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8 COMMENT='地址';
        """);
        println("创建address表")
    }

    @Test
    fun testInsert(){
//       var id = DbQueryBuilder(db).table("user").value(mapOf("name" to "shi", "age" to 1)).insert(true);
        var id = DbQueryBuilder(db).table("user").insertColumns("name", "age").value("shi", 1).insert(true);
        println("插入user表：" + id)
    }

    @Test
    fun testBatchInsert(){
        val query = DbQueryBuilder(db).table("user").insertColumns("name", "age");
        for (i in id..(id+10)){
            query.value("shi-$i", i)
        }
        val id = query.insert(true);
        println("批量插入user表, 起始id：$id, 行数：10")
    }

    @Test
    fun testBatchInsert2(){
        // 初始化查询
        val initQuery:(DbQueryBuilder)->DbQueryBuilder = { query:DbQueryBuilder ->
            query.clear().table("user").insertColumns("name", "age") as DbQueryBuilder;
        }
        val query = initQuery(DbQueryBuilder(db))
        for(i in 0..1){
            for (j in 1..10){
                query.value("shi-$j", j)
            }
            val id = query.insert(true);
            println("批量插入user表, 起始id：$id, 行数：10")
            initQuery(query) // 重新初始化插入
        }
    }

    @Test
    fun testBatchInsert3(){
        // 构建参数
        val params:ArrayList<Any?> = ArrayList()
        for(i in 0..1){
            for (j in 1..10){
                params.add("shi-$j")
                params.add(j)
            }
        }

        // 批量插入
        DbQueryBuilder(db).table("user").insertColumns("name", "age").value("?", "?").batchExecute(ActionType.INSERT, params, 2)// 每次只处理2个参数
    }

    @Test
    fun testFind(){
        val record = DbQueryBuilder(db).table("user").where("id", "=", id).find<Record>()
        println("查询user表：" + record)
    }

    @Test
    fun testFindAll(){
        val records = DbQueryBuilder(db).table("user").orderBy("id").limit(1).findAll<Record>()
        println("查询user表：" + records)
    }

    @Test
    fun testFindPage(){
        val query: IDbQueryBuilder = DbQueryBuilder(db).table("user")
        val counter:IDbQueryBuilder = query.clone() as IDbQueryBuilder // 克隆query builder
        val records = query.orderBy("id").limit(10).findAll<Record>() // 查分页数据
        val count = counter.count() // 查总数
        println("查询user表：总数: " + count + ", 分页数据：" + records)
    }

    @Test
    fun testCount(){
        val count = DbQueryBuilder(db).table("user").count();
        println("统计user表：" + count)
    }

    @Test
    fun testUpdate(){
        val f = DbQueryBuilder(db).table("user").sets(mapOf("name" to "wang", "age" to 2)).where("id", "=", id).update();
        println("更新user表：" + f)
    }

    @Test
    fun testBatchUpdate(){
        var myid:Long = 10
        // 构建参数
        val params:ArrayList<Any?> = ArrayList()
        for(i in 0..1){
            for (j in 1..10){
                params.add("shi-$j")
                params.add(j)
                params.add(myid ++)
            }
        }

        // 批量插入
        DbQueryBuilder(db).table("user").set("name", "?").set("age", "?").where("id", "=", "?").batchExecute(ActionType.UPDATE, params, 3)// 每次只处理3个参数
    }

    @Test
    fun testDelete(){
        val f = DbQueryBuilder(db).table("user").where("id", "=", id).delete();
        println("删除user表：" + f)
    }


    @Test
    fun testBatchDelete(){
        var myid:Long = 10
        // 构建参数
        val params:ArrayList<Any?> = ArrayList()
        for(i in 0..1){
            for (j in 1..10){
                params.add(myid ++)
            }
        }

        // 批量插入
        DbQueryBuilder(db).table("user").where("id", "=", "?").batchExecute(ActionType.DELETE, params, 1)// 每次只处理1个参数
    }

    //预编译参数化的sql
    @Test
    fun testPrepare(){
        val query = DbQueryBuilder(db)
                .prepare(true)
                .table("user")
                .where("id", "=", "?" /* 被当做是参数*/)
        for(i in id..(id+10)){
            val record = query
                    .find<Record>(i) // 仅在第一次调用时编译与缓存sql，以后多次调用不再编辑，直接使用缓存的sql
            println("查询user_" + i + "：" + record)
        }
    }

}