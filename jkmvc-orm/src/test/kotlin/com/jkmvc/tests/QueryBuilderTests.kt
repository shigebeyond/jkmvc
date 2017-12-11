package com.jkmvc.tests

import com.jkmvc.db.*
import org.junit.Test


class QueryBuilderTests{

    val db: Db = Db.instance()

    val id: Long by lazy {
        val (hasNext, minId) = db.queryCell("select id from user order by id limit 1" /*sql*/)
        println("随便选个id: " + minId)
        minId as Long;
    }

    @Test
    fun testInsert(){
//       var id = DbQueryBuilder(db).table("user").value(mapOf("name" to "shi", "age" to 1)).insert("id);
        var id = DbQueryBuilder(db).table("user").insertColumns("name", "age").value("shi", 1).insert("id");
        println("插入user表：" + id)
    }

    @Test
    fun testBatchInsert(){
        val query = DbQueryBuilder(db).table("user").insertColumns("name", "age");
        val ids = ArrayList<Int>()
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
        val initQuery:(DbQueryBuilder)->DbQueryBuilder = { query:DbQueryBuilder ->
            query.clear().table("user").insertColumns("name", "age") as DbQueryBuilder;
        }
        val query = initQuery(DbQueryBuilder(db))
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
        //DbQueryBuilder(db).table("user").insertColumns("name", "age").value("?", "?").batchExecute(ActionType.INSERT, params, 2)// 每次只处理2个参数
        DbQueryBuilder(db).table("user").insertColumns("name", "age").value("?", "?").batchInsert(params, 2)// 每次只处理2个参数
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

    /**
     * 嵌套的 whereOpen 子句
     */
    @Test
    fun testNestedClauses(){
        val query = DbQueryBuilder(db).from("user")
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
        val query = DbQueryBuilder(db).select("username", "COUNT(`id`)" to "total_posts")
                .from("posts").groupBy("username").having("total_posts", ">=", 10);
        val csql = query.compileSelect()
        println(csql.previewSql())
    }

    /**
     * 测试子查询
     */
    @Test
    fun testSubQuery(){
        // 子查询
        val sub = DbQueryBuilder(db).select("username", "COUNT(`id`)" to "total_posts")
                .from("posts").groupBy("username").having("total_posts", ">=", 10);

        // join子查询： join select
        val query = DbQueryBuilder(db).select("profiles.*", "posts.total_posts").from("profiles")
                .joins(sub to "posts", "INNER").on("profiles.username", "=", "posts.username");
        val csql = query.compileSelect()

        // insert子查询： insert...select
        //val query = DbQueryBuilder(db).table("post_totals").insertColumns("username", "posts").values(sub)
        //val csql = query.compileInsert()
        println(csql.previewSql())
    }

    /**
     * db表达式
     */
    @Test
    fun testDbExpression(){
        val f = DbQueryBuilder().table("user").set("age", DbExpression("age + 1")).where("id", "=", 1).update();
        println("age++ : $f")

    }


    @Test
    fun testCompiledSql(){
        println("使用编译过的sql来重复查询")
        val csql = DbQueryBuilder(db).table("user").where("id", "=", "?").compileSelectOne()
        for(i in 0..10){
            println(csql.find<Record>(i))
        }
    }

}