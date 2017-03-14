package com.jkmvc.tests

import com.jkmvc.db.getDruidDataSource
import com.jkmvc.db.Db
import com.jkmvc.db.DbQueryBuilder
import java.sql.Connection
import org.junit.Test
import kotlin.test.assertFalse

class DbTests{

    @Test
    fun testConnection(){
        val dataSource = getDruidDataSource();
        val db: Db = Db.connect(dataSource);
        val count = DbQueryBuilder(db).table("user").count();
        println("查询user表：" + count)
    }

    @Test
    fun testInsert(){
        val dataSource = getDruidDataSource();
        val db: Db = Db.connect(dataSource);
        val id = DbQueryBuilder(db).table("user").value(mapOf("name" to "shi", "age" to 1)).insert();
        println("插入user表：" + id)
    }

    @Test
    fun testFind(){
        val dataSource = getDruidDataSource();
        val db: Db = Db.connect(dataSource);
        val record = DbQueryBuilder(db).table("user").where("id", "=", 1).find()
        println("查询user表：" + record)
    }

    @Test
    fun testFindAll(){
        val dataSource = getDruidDataSource();
        val db: Db = Db.connect(dataSource);
        val records = DbQueryBuilder(db).table("user").where("id", "=", 1).findAll()
        println("查询user表：" + records)
    }

    @Test
    fun testCount(){
        val dataSource = getDruidDataSource();
        val db: Db = Db.connect(dataSource);
        val count = DbQueryBuilder(db).table("user").count();
        println("统计user表：" + count)
    }

    @Test
    fun testUpdate(){
        val dataSource = getDruidDataSource();
        val db: Db = Db.connect(dataSource);
        val f = DbQueryBuilder(db).table("user").sets(mapOf("name" to "wang", "age" to 2)).where("id", "=", 1).update();
        println("更新user表：" + f)
    }

    @Test
    fun testDelete(){
        val dataSource = getDruidDataSource();
        val db: Db = Db.connect(dataSource);
        val f = DbQueryBuilder(db).table("user").where("id", "=", 1).delete();
        println("删除user表：" + f)
    }

}



