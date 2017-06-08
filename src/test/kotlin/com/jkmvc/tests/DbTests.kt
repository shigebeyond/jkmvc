package com.jkmvc.tests

import com.jkmvc.db.Db
import com.jkmvc.db.DbQueryBuilder
import com.jkmvc.db.Record
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*
import java.util.GregorianCalendar




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
        id = DbQueryBuilder(db).table("user").value(mapOf("name" to "shi", "age" to 1)).insert();
        println("插入user表：" + id)
    }

    @Test
    fun testFind(){
        val record = DbQueryBuilder(db).table("user").where("id", "=", id).find<Record>()
        println("查询user表：" + record)
    }

    @Test
    fun testFindAll(){
        val records = DbQueryBuilder(db).table("user").findAll<Record>()
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

    @Test
    fun testDate(){
        // 第一天
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd");
        val firstRecord = DbQueryBuilder(db).select("jid", "dateline").table("sk_join").orderBy("jid", "asc").find<Record>()
        var firstDate = Date(firstRecord!!.getLong("dateline")!! * 1000);
        firstDate.hours = 0
        firstDate.minutes = 0
        firstDate.seconds = 0
        val calendar = GregorianCalendar()
        calendar.time = firstDate;

        var id:Long = firstRecord["jid"];
        while (true) {
            // 查某天的id范围
            val startTime: Long = calendar.timeInMillis / 1000;
            calendar.add(Calendar.DATE, 1)// 加一天
            val endTime: Long = calendar.timeInMillis / 1000;
            val record = DbQueryBuilder(db).select(Pair("min(jid)", "start_jid"), Pair("max(jid)", "end_jid"))
                    .table("sk_join")
                    .where("dateline", ">=", startTime)
                    .where("dateline", "<", endTime)
                    .where("jid", ">=", id)
                    .find<MutableMap<String, Any?>>()
            if(record == null)
                continue;

            // 插入参与日期表
            record["date"] = calendar.get(Calendar.YEAR) * 10000 + calendar.get(Calendar.MONDAY) * 100 + calendar.get(Calendar.DATE)
            DbQueryBuilder(db).table("sk_join_date").value(record).insert();

            id = (record["end_id"] as Long) + 1;
        }
    }


}



