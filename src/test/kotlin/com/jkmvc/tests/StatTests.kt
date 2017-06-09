package com.jkmvc.tests

import com.jkmvc.db.Db
import com.jkmvc.db.DbQueryBuilder
import com.jkmvc.db.Record
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import java.util.Calendar




class StatTests{

    val db: Db = Db.getDb("test")

    // 计算每天的id范围
    @Test
    fun testCreateTable(){
        db.execute("""
        CREATE TABLE IF NOT EXISTS `sk_join_date` (
          `date` int(10) NOT NULL DEFAULT '0' COMMENT '日期',
          `end_jid` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '结束参与编号',
          PRIMARY KEY (`date`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
        """);
        println("创建sk_join_date表")
        db.execute("""
        CREATE TABLE IF NOT EXISTS `sk_buyer_join_stat` (
          `uid` int(10) NOT NULL DEFAULT '0' COMMENT '试客id',
          `join_num_day` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '最大参与数的日期',
          `max_join_num` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '最大参与数',
          `join_price_day` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '最大参与总价的日期',
          `max_join_price` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '最大参与总价',
          `report_num_day` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '最大报告数的日期',
          `max_report_num` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '最大报告数',
          PRIMARY KEY (`uid`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='试客参与统计';
        """);
        println("创建sk_buyer_join_stat表")
    }

    // 计算每天的id范围
    @Test
    fun testPrepareDate(){
        val timeOfDay = 3600 * 24;
        val df = SimpleDateFormat("yyyy-MM-dd");
        var startTime = System.currentTimeMillis();

        // 第一天
        val firstRecord = DbQueryBuilder(db).select("jid", "dateline").table("sk_join").orderBy("jid", "asc").find<Record>()
        var date = Date(firstRecord!!.getLong("dateline")!! * 1000);
        date.hours = 0
        date.minutes = 0
        date.seconds = 0
        var time:Long = date.time / 1000

        var id:Long = firstRecord["jid"];
        // 每次处理10w个
        while (true) {
            var i = 0
            var first = true;
            // 查10w个
            println("查第 $i 个10w")
            val records = DbQueryBuilder(db).select("jid", "dateline")
                    .table("sk_join")
                    .where("jid", ">=", id)
                    .limit(100000)
                    .findAll<Record>()
            if(records.isEmpty())
                break;

            for (record in records){
                // 比较日期
                val currTime = record.getLong("dateline")!!;
                if(currTime >= time + timeOfDay){ // 新的一天
                    // 新增参与日期表
                    println(df.format(Date(time * 1000)) + "最大参与id：" + id)
                    if(first){ // 第一次replace，有可能之前已插入过
                        db.execute("REPLACE INTO sk_join_date(date, end_jid) VALUES(?, ?)", listOf(time, id))
                        first = false;
                    }else{ // 其他insert
                        db.execute("INSERT INTO sk_join_date(date, end_jid) VALUES(?, ?)", listOf(time, id))
                    }
                    // 记录最新日期
                    val newTime:Long = record["dateline"]
                    val day:Long = (newTime - time) / timeOfDay;
                    time += day * timeOfDay
                }
                id = record["jid"] // 记录最新id
            }

            id++;
            i++;
        }

        db.execute("REPLACE INTO sk_join_date(date, end_jid) VALUES(?, ?)", listOf(time, id))
        val costTime:Long = (System.currentTimeMillis() - startTime) / 1000;
        println("总耗时: " + costTime)
    }

    // test每天的记录数
    @Test
    fun testCountDate() {
        // 第一个id
        val firstRecord = DbQueryBuilder(db).select("jid", "dateline").table("sk_join").orderBy("jid", "asc").find<Record>()
        var id = firstRecord!!.getLong("jid")!!

        // 遍历每天
        val dates = DbQueryBuilder(db).select("date", "end_jid")
                .table("sk_join_date")
                .orderBy("date", "asc")
                .findAll<Record>()
        for (date in dates){
            val endId = date.getLong("end_jid")!!
            println("日期：" + date["date"] + ", 数量：" +  (endId - id))
            id = endId
        }
    }

    // test日期格式化
    @Test
    fun testFormatDate(){
        val time = Date(1474387200000);

        val formatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val ctime = formatter.format(time)
        println(ctime)

        val c = Calendar.getInstance()
//        c.time = time
        c.timeInMillis = 1474387200000
        val day:Int = c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DATE); // 月份从0开始
        println(day)
    }


    // 统计每天的最高数
    @Test
    fun testStatDate(){
        var startTime = System.currentTimeMillis();
        val c = Calendar.getInstance()
        // 遍历每天
        val dates = DbQueryBuilder(db).select("date", "end_jid")
                .table("sk_join_date")
                .orderBy("date", "asc")
                .findAll<Record>()
        var startId = 0
        val stat = HashMap<Int, DayStat>()
        for (date in dates){
            // 统计一天
//            val day:Int = date.getInt("date")!!
            c.timeInMillis = (date.getInt("date")!!).toLong() * 1000
            val day:Int = c.get(Calendar.YEAR) * 10000 + c.get(Calendar.MONTH) * 100 + c.get(Calendar.DATE);
            println("统计一天: $day")
            val endId:Int = date["end_jid"];

            val limit = 100000;
            var n = 0
            var startTime2 = System.currentTimeMillis();
            while (true){
                val records = DbQueryBuilder(db).select("jid", "state", "price", "report_id", "buyer_uid")
                        .table("sk_join")
                        .where("jid", ">", startId)
                        .where("jid", "<=", endId)
                        .orderBy("jid", "asc")
                        .limit(limit)
                        .findAll<Record>();
                if(records.isEmpty())
                    break;

                n += records.size
                for(record in records){
                    val uid:Int = record["buyer_uid"]
                    if(!stat.contains(uid))
                        stat[uid] = DayStat()
                    stat[uid]!!.addJoin(day, record)
                }
                if(records.size < limit)
                    break;

                startId = records.last()["jid"]; // 记录最新的id
            }
            val costTime2:Long = (System.currentTimeMillis() - startTime2) / 1000;
            println("行数：$n , 总耗时： $costTime2 秒")

            startId = endId;
        }

        // 插入所有用户的统计结果
        println("插入所有用户的统计结果: " + stat.size + "行")
        var i = 0;
        val query = DbQueryBuilder(db).table("sk_buyer_join_stat").insertColumns("uid", "join_num_day", "max_join_num", "join_price_day", "max_join_price", "report_num_day", "max_report_num");
        for ((k, v) in stat) {
            v.finish()
            query.value(k, v.joinNum.max.day, v.joinNum.max.num, v.joinPrice.max.day, v.joinPrice.max.num, v.reportNum.max.day, v.reportNum.max.num)
            if(++i >= 1000) { // 逢1k批量插入
                println(" 逢1k批量插入")
                i = 0
                query.insert();
                query.clear().table("sk_buyer_join_stat").insertColumns("uid", "join_num_day", "max_join_num", "join_price_day", "max_join_price", "report_num_day", "max_report_num");
            }
        }
        if(i > 0)
            query.insert();

        val costTime:Long = (System.currentTimeMillis() - startTime) / 1000;
        println("总耗时: $costTime 秒")
    }

}

// 可比较的一天数据
data class DayCompare(var day:Int, var num:Double){
    /**
     * 比较操作符
     */
    public operator fun compareTo(other:DayCompare):Int{
        return (num - other.num).toInt();
    }

    public fun addNum(n:Double){
        num += n;
    }
}

// 记录最大数的一天
// max 为现有的最大的一天
// curr 为当前的一天，记录中间状态，如果要结束中间状态，记得调用 finish()
data class DayMax(var max:DayCompare = DayCompare(0, 0.0), var curr:DayCompare = DayCompare(0, 0.0)){

    public fun addItem(day:Int, num:Double){
        if(curr.day == day){ // 同一天
            curr.addNum(num)
        } else { // 新的一天
            // 当前天已统计完毕，直接与最大值比较
            if(curr  > max){
                max = curr;
            }

            // 切换到新的一天
            curr = DayCompare(day, num)
        }
    }

    public fun finish(){
        if(curr  > max){
            max = curr;
        }
    }
}
/**
 * 日统计项: 统计日的最高值（3个指标）
 */
data class DayStat(var joinNum:DayMax = DayMax(), var joinPrice:DayMax = DayMax(), var reportNum:DayMax = DayMax()){

    /**
     * 添加参与记录
     */
    public fun addJoin(day:Int, join:Record){
        joinNum.addItem(day, 1.0)
        joinPrice.addItem(day, join["price"])
        if(join.getLong("report_id")!! > 0)
            reportNum.addItem(day, 1.0)
    }

    public fun finish(){
        joinNum.finish()
        joinPrice.finish()
        reportNum.finish()
    }
}