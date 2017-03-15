package com.jkmvc.db

import java.sql.Connection
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.memberFunctions

/**
 * sql构建器
 *   依次继承 DbQueryBuilderAction 处理动作子句 + DbQueryBuilderDecoration 处理修饰子句
 *  提供select/where等类sql的方法, 但是调用方法时, 不直接拼接sql, 而是在compile()时才延迟拼接sql, 因为调用方法时元素可以无序, 但生成sql时元素必须有序
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-13
 *
 */
class DbQueryBuilder(db:Db = Db.getDb(), table:String = "" /*表名*/) :DbQueryBuilderDecoration(db, table)
{
    public constructor(dbName:String /* db名 */, table:String = "" /*表名*/):this(Db.getDb(dbName), table){
    }

    /**
     * 编译sql
     *
     * @param string action sql动作：select/insert/update/delete
     * @return array(sql, 参数)
     */
    public override fun compile(action:String):Pair<String, List<Any?>>
    {
        params.clear();

        // 动作子句 + 修饰子句
        val actionSql:String = this.action(action).compileAction();
        val decorationSql:String = compileDecoration();
        println(actionSql + decorationSql)
        println(params)
        return Pair(actionSql + decorationSql, params);
    }

    /**
     * 编译 + 执行
     *
     * @param string action sql动作：select/insert/update/delete
     * @return int 影响行数|新增id
     */
    protected fun execute(action:String):Int
    {
        // 1 编译
        val (sql, params) = compile(action);

        // 2 执行 insert/update/delete
        return db.execute(sql, params);
    }

    /**
     * 查找多个： select 语句
     *
     * @return array
     */
    public override fun findAll(): List<DbRecord> {
        // 1 编译
        val (sql, params) = compile("select");

        // 2 执行 select
        return db.queryRows<DbRecord>(sql, params){ row:MutableMap<String, Any?> ->
            DbRecord(row)
        }
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @return object
     */
    public override fun find(): DbRecord? {
        // 1 编译
        val (sql, params) = compile("select");

        // 2 执行 select
        return db.queryRow<DbRecord>(sql, params){ row:MutableMap<String, Any?> ->
            DbRecord(row)
        }
    }

    /**
     * 统计行数： count语句
     * @return int
     */
    public override fun count():Int
    {
        // 1 编译
        val (sql, params) = select(Pair("count(1)", "num")).compile("select");

        // 2 执行 select
        val (hasNext, count) = db.queryCell(sql, params);
        return if(hasNext)
                    0
                else
                    count as Int;
    }

    /**
     * 插入：insert语句
     * @return int 新增的id
     */
    public override fun insert():Int
    {
        return execute("insert");
    }

    /**
     *	更新：update语句
     *	@return	bool
     */
    public override fun update():Boolean
    {
        return execute("update") > 0;
    }

    /**
     *	删除
     *	@return	bool
     */
    public override fun delete():Boolean
    {
        return execute("delete") > 0;
    }
}