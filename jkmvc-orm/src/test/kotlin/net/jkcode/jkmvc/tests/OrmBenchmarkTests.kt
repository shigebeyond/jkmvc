package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.common.randomInt
import net.jkcode.jkmvc.db.Db
import net.jkcode.jkmvc.query.DbExpr
import net.jkcode.jkmvc.query.DbQueryBuilder
import net.jkcode.jkmvc.tests.entity.MessageEntity
import net.jkcode.jkmvc.tests.model.MessageModel
import org.junit.Test
import java.text.MessageFormat

/**
 * orm性能测试
 *    纯sql vs orm : 执行10w次, 看执行时间差
 *
 *    纯sql: 22164.07 ms
 *    orm:  68296.76 ms
 */
class OrmBenchmarkTests{

    private val num = 100000

    /**
     * RunTime: 18217.71 ms
     * TPS: 18217.71
     *
     * RunTime: 22329.81 ms
     * TPS: 22329.81
     */
    @Test
    fun testSql(){
        test{id ->
            Db.instance().queryRow("select * from message where id = $id", emptyList()) { row ->
                val msg = MessageEntity()
                msg.fromMap(row)
                msg
            }
        }
    }

    /**
     * RunTime: 53153.4 ms
     * TPS: 53153.4
     *
     * RunTime: 51891.99 ms
     * TPS: 51891.99

     * 通过jprofile得知, 相对于纯sql, 内存消耗大
     * 其中 char是5w个(是纯sql的5倍), String
     */
    @Test
    fun testOrm(){
        test{id ->
            MessageModel.queryBuilder().where("id", "=", id).findEntity<MessageModel, MessageEntity>()
        }
    }

    /**
     * RunTime: 67406.3 ms
     * TPS: 67406.3

     * RunTime: 60453.64 ms
     * TPS: 60453.64
     *
     * 通过jprofile得知, 内存跟orm差不多
     * findEntity() 跟 findRow{} 差不多
     */
    @Test
    fun testQuery(){
        test{id ->
            DbQueryBuilder().table("message").where("id", "=", id).findRow{ row ->
                val msg = MessageEntity()
                msg.fromMap(row)
                msg
            }
        }
    }

    /**
     * RunTime: 36273.38 ms
     * TPS: 36273.38

     * RunTime: 39243.38 ms
     * TPS: 39243.38
     *
     * 通过jprofile得知, 内存跟纯sql差不多
     * 性能在 纯sql 与 orm 之间
     */
    @Test
    fun testQueryReuse(){
        val query = DbQueryBuilder()
        test{id ->
            query.clear()
            query.table("message").where("id", "=", id).findRow{ row ->
                val msg = MessageEntity()
                msg.fromMap(row)
                msg
            }
        }
    }

    /**
     * RunTime: 23175.54 ms
     * TPS: 23175.54
     *
     */
    @Test
    fun testQueryCompiled(){
        val csql = DbQueryBuilder().table("message").where("id", "=", DbExpr.question).compileSelectOne()
        test{ id ->
            csql.findRow(listOf(id)){ row ->
                val msg = MessageEntity()
                msg.fromMap(row)
                msg
            }
        }
    }


    fun test(action: (Int)->MessageEntity?){
        val start = System.nanoTime()
        for(i in 0..num) {
            val id = randomInt(10) + 1
            val msg = action.invoke(i)
        }
        val runTime = System.nanoTime() - start
        val runMs = runTime.toDouble() / 1000000L
        println(MessageFormat.format("RunTime: {0,number,#.##} ms \nTPS: {0,number,#.##}", runMs, num.toDouble() / runMs * 1000))
    }

}



