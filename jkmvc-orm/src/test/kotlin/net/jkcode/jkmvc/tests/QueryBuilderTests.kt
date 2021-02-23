package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.db.*
import net.jkcode.jkmvc.orm.DbKey
import net.jkcode.jkmvc.orm.DbKeyNames
import net.jkcode.jkmvc.orm.whereIn
import net.jkcode.jkmvc.query.DbExpr
import net.jkcode.jkmvc.query.DbQueryBuilder
import net.jkcode.jkmvc.query.IDbQueryBuilder
import net.jkcode.jkmvc.query.SqlAction
import org.junit.Test


class QueryBuilderTests{

    val db: Db = Db.instance()

    val id: Int by lazy {
        val minId = db.queryValue<Int>("select id from user order by id limit 1" /*sql*/)!!
        println("随便选个id: " + minId)
        minId
    }

    @Test
    fun testInsert(){
//       var id = DbQueryBuilder().table("user").value(mapOf("name" to "shi", "age" to 1)).insert("id);
        var id = DbQueryBuilder().table("user").insertColumns("name", "age").value("shi", 1).insert("id");
        println("插入user表：" + id)
    }

    @Test
    fun testBatchInsert(){
        val query = DbQueryBuilder().table("user").insertColumns("name", "age");
        val ids = ArrayList<Long>()
        for (i in id..(id+10)){
            query.value("shi-$i", i)
            val id = query.insert("id");
            ids.add(id)
        }
        println("批量插入user表, 起始id：${ids.first()}, 行数：10")
    }

    @Test
    fun testBatchInsert2(){
        // 初始化查询
        val initQuery:(DbQueryBuilder)-> DbQueryBuilder = { query: DbQueryBuilder ->
            query.clear().table("user").insertColumns("name", "age") as DbQueryBuilder;
        }
        val query = initQuery(DbQueryBuilder())
        for(i in 0..1){
            for (j in 1..10){
                query.value("shi-$j", j)
            }
            val id = query.insert("id");
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
        DbQueryBuilder().table("user").insertColumns("name", "age").value(DbExpr.question, DbExpr.question).batchInsert(params)
    }

    @Test
    fun testFind(){
        val row = DbQueryBuilder().table("user").where("id", "=", id).distinct().forUpdate().findMap()
        println("查询user表：" + row)
    }

    @Test
    fun testFindAll(){
        val rows = DbQueryBuilder().table("user").orderBy("id").limit(1).findMaps()
        println("查询user表：" + rows)
    }

    @Test
    fun testBetween(){
        val rows = DbQueryBuilder().table("user").where("id", "BETWEEN", Pair(1, 2)).findMaps()
        println("查询user表：" + rows)
    }

    @Test
    fun testCondition(){
        val rows = DbQueryBuilder().table("user").whereCondition("id BETWEEN 1 and 10").findMaps()
        println("查询user表：" + rows)
    }

    @Test
    fun testSplitWhere(){
        val rows = DbQueryBuilder().table("user")
                .where("id&age", ">", 0) // &是and
                //.where("name|avatar", "like", "%shi%") // |是or
                .whereLike("name|avatar", "shi") // |是or
                .findMaps()
        println("查询user表：" + rows)
    }

    @Test
    fun testFindPage(){
        val query: IDbQueryBuilder = DbQueryBuilder().table("user")
        val counter: IDbQueryBuilder = query.copy() // 克隆query builder
        val rows = query.orderBy("id").limit(10).findMaps() // 查分页数据
        val count = counter.count() // 查总数
        println("查询user表：总数: " + count + ", 分页数据：" + rows)
    }

    @Test
    fun testCount(){
        val count = DbQueryBuilder().table("user").count();
        println("统计user表：" + count)
    }

    @Test
    fun testUpdate(){
        val f = DbQueryBuilder().table("user").sets(mapOf("name" to "wang", "age" to 2)).where("id", "=", id).update();
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
        DbQueryBuilder().table("user")
                .set("name", DbExpr.question)
                .set("age", DbExpr.question)
                .where("id", "=", DbExpr.question)
                .batchExecute(SqlAction.UPDATE, params)
    }

    @Test
    fun testDelete(){
        val f = DbQueryBuilder().table("user").where("id", "=", id).delete();
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
        DbQueryBuilder().table("user").where("id", "=", DbExpr.question).batchDelete(params)
    }

    /**
     * 嵌套的 whereOpen 子句
     */
    @Test
    fun testNestedClauses(){
        val query = DbQueryBuilder().from("user")
                .whereOpen()
                    .where("id", "IN", arrayOf(1, 2, 3, 5))
                    .andWhereOpen()
                        .where("lastLogin", "<=", System.currentTimeMillis() / 1000)
                        .orWhere("lastLogin", "IS", null)
                    .andWhereClose()
                .whereClose()
                .andWhere("removed","IS", null);
        val csql = query.compileSelect()
        println(csql.previewSql())
    }

    @Test
    fun testHaving(){
        val query = DbQueryBuilder().select("username", DbExpr("COUNT(`id`)", "total_posts", false))
                .from("posts").groupBy("username").having("total_posts", ">=", 10);
        val csql = query.compileSelect()
        println(csql.previewSql())
    }

    /**
     * 测试join
     */
    @Test
    fun testJoin(){
        val query = DbQueryBuilder().select("profiles.*", "posts.total_posts").from("profiles")
                .join("posts", "INNER").on("profiles.username", "=", "posts.username");
        val csql = query.compileSelect()
        println(csql.previewSql())
    }

    /**
     * 测试DbKey
     */
    @Test
    fun testDbKey(){
        val query = DbQueryBuilder().whereIn(DbKeyNames("appId", "version"), DbKey(listOf(1,1,1), listOf(2,3,4))).from("app")
        val csql = query.compileSelect()
        println(csql.previewSql())
    }

    /**
     * 测试子查询
     */
    @Test
    fun testSubQuery(){
        /*// 子查询
        val sub = DbQueryBuilder().select("username", DbExpr("COUNT(`id`)", "total_posts"))
                .from("posts").groupBy("username").having("total_posts", ">=", 10);

        // join子查询： join select
        val query = DbQueryBuilder().select("profiles.*", "posts.total_posts").from("profiles")
                .join(DbExpr(sub, "posts"), "INNER").on("profiles.username", "=", "posts.username");
        val csql = query.compileSelect()

        // insert子查询： insert...select
        //val query = DbQueryBuilder().table("post_totals").insertColumns("username", "posts").values(sub)
        //val csql = query.compileInsert()
        */

        // 子查询
        val sub = DbQueryBuilder().select("username").from("posts");

        // select in 子查询
        val query = DbQueryBuilder().from("users").where("username", "IN", sub);
        val csql = query.compileSelect()

        println(csql.previewSql())
    }

    /**
     * db表达式
     */
    @Test
    fun testDbExpr(){
        val f = DbQueryBuilder().table("user")
                    .set("age", DbExpr("age + 1", false))
                    //.set("age", "age + 1", true)
                    .where("id", "=", 1).update();
        println("age++ : $f")

    }


    @Test
    fun testCompiledSql(){
        println("使用编译过的sql来重复查询")
        val csql = DbQueryBuilder().table("user").where("id", "=", DbExpr.question).compileSelectOne()
        for(i in 0..10){
            println(csql.findMap(listOf(i)))
        }
    }

}