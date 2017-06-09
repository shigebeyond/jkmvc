package com.jkmvc.tests

import com.jkmvc.db.Db
import com.jkmvc.db.DbQueryBuilder
import com.jkmvc.db.Record
import org.junit.Test


class DbTests{

    val db: Db = Db.getDb()
    var id = 0;

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
        println("创建user表")
    }

    @Test
    fun testInsert(){
//        id = DbQueryBuilder(db).table("user").value(mapOf("name" to "shi", "age" to 1)).insert(true);
        id = DbQueryBuilder(db).table("user").insertColumns("name", "age").value("shi", 1).insert(true);
        println("插入user表：" + id)
    }

    @Test
    fun testBatchInsert(){
        val query = DbQueryBuilder(db).table("user").insertColumns("name", "age");
        for (i in 0..9){
            query.value("shi-$i", i)
        }
        val id = query.insert(true);
        println("批量插入user表, 起始id：$id, 行数：10")
    }

    @Test
    fun testBatchInsert2(){
        val query = DbQueryBuilder(db).table("user").insertColumns("id", "name", "age");
        for(i in 0..1){
            for (j in 1..10){
                query.value(i * 10 + j, "shi-$j", j)
            }
            val id = query.insert(true);
            println("批量插入user表, 起始id：$id, 行数：10")
            query.clear().table("user").insertColumns("id", "name", "age");
        }
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
    fun testDelete(){
        val f = DbQueryBuilder(db).table("user").where("id", "=", id).delete();
        println("删除user表：" + f)
    }

}