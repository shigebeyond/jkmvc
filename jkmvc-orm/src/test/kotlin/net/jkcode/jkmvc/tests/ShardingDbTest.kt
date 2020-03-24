package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.db.Db
import net.jkcode.jkmvc.db.sharding.ShardingDb
import org.junit.Test

/**
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-08-23 11:36 AM
 */
class ShardingDbTest {

    val db: Db = ShardingDb("shardorder")

    @Test
    fun testCreateTable() {
        db.execute("CREATE TABLE IF NOT EXISTS t_order (order_id BIGINT NOT NULL AUTO_INCREMENT, user_id INT NOT NULL, status VARCHAR(50), PRIMARY KEY (order_id))")
        db.execute( "CREATE TABLE IF NOT EXISTS t_order_item (order_item_id BIGINT NOT NULL AUTO_INCREMENT, order_id BIGINT NOT NULL, user_id INT NOT NULL, PRIMARY KEY (order_item_id))")
    }

    @Test
    fun testDropTable() {
        db.execute("DROP TABLE t_order_item")
        db.execute("DROP TABLE t_order")
    }

    @Test
    fun testInsert() {
        for (i in 1..9) {
            var orderId = db.execute("INSERT INTO t_order (user_id, status) VALUES (10, 'INIT')", emptyList<Any>(), "order_id") // 返回自增主键值
            db.execute("INSERT INTO t_order_item (order_id, user_id) VALUES (?, 10)", listOf(orderId))
            println("新增双数user_id的订单, 写入分库0")

            orderId = db.execute("INSERT INTO t_order (user_id, status) VALUES (11, 'INIT')", emptyList<Any>(), "order_id")
            db.execute("INSERT INTO t_order_item (order_id, user_id) VALUES (?, 11)", listOf(orderId))
            println("新增单数user_id的订单, 写入分库1")
        }
    }

    @Test
    fun testFind(){
        val row = db.queryMap("select * from t_order limit 1" /*sql*/, emptyList<Any>() /*参数*/) // 返回 Map 类型的一行数据
        println("查询user表：" + row)
    }

    @Test
    fun testFindAll(){
        val rows = db.queryMaps("SELECT i.* FROM t_order o JOIN t_order_item i ON o.order_id=i.order_id WHERE o.user_id in (?, ?)" /*sql*/, listOf(10, 11) /*参数*/) // 返回 Map 类型的多行数据
        println("查询订单表：" + rows)
    }

    @Test
    fun testTransaction(){
        db.transaction {
            var orderId = db.execute("INSERT INTO t_order (user_id, status) VALUES (10, 'INIT')", emptyList<Any>(), "order_id") // 返回自增主键值
            db.execute("INSERT INTO t_order_item (order_id, user_id) VALUES (?, 10)", listOf(orderId))

            orderId = db.execute("INSERT INTO t_order (user_id, status) VALUES (11, 'INIT')", emptyList<Any>(), "order_id")
            db.execute("INSERT INTO t_order_item (order_id, user_id) VALUES (?, 11)", listOf(orderId))
        }
    }
}