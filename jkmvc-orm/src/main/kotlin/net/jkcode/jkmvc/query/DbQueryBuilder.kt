package net.jkcode.jkmvc.query

import net.jkcode.jkmvc.db.Db
import net.jkcode.jkmvc.db.DbResultSet
import net.jkcode.jkmvc.db.IDb

/**
 * sql构建器
 *   依次继承 DbQueryBuilderAction 处理动作子句 + DbQueryBuilderDecoration 处理修饰子句
 *  提供select/where等类sql的方法, 但是调用方法时, 不直接拼接sql, 而是在compile()时才延迟拼接sql, 因为调用方法时元素可以无序, 但生成sql时元素必须有序
 *
 * @author shijianhang
 * @date 2016-10-13
 */
open class DbQueryBuilder(public override val defaultDb: IDb = Db.instance()) : DbQueryBuilderDecoration() {

    /**
     * 获得sql查询构建器
     *
     *
     * @param table
     * @param sort 排序字段
     * @param desc 是否降序
     * @param start 偏移
     * @param rows 查询行数
     * @param defaultDb
     * @return
     */
    public constructor(table: String, sort: String? = null, desc: Boolean? = null, start: Int? = null, rows: Int? = null, defaultDb: IDb = Db.instance()): this(defaultDb) {
        table(table)

        if (sort != null && sort != "")
            orderBy(sort, desc)

        if (rows != null && rows > 0)
            limit(rows, start ?: 0)
    }

    /**
     * 获得sql查询构建器
     *
     *
     * @param table
     * @param condition 条件
     * @param params 条件参数
     * @param sort 排序字段
     * @param desc 是否降序
     * @param start 偏移
     * @param rows 查询行数
     * @param defaultDb
     * @return
     */
    public constructor(table: String, condition: String, params: List<*> = emptyList<Any>(), sort: String? = null, desc: Boolean? = null, start: Int? = null, rows: Int? = null, defaultDb: IDb = Db.instance()): this(table, sort, desc, start, rows, defaultDb){
        whereCondition(condition, params)
    }

    /****************************** 编译sql ********************************/
    /**
     * 编译sql
     * @param action sql动作：select/insert/update/delete
     * @param db 数据库连接
     * @return 编译好的sql
     */
    public override fun compile(action: SqlAction, db: IDb): CompiledSql {
        // 清空编译结果
        compiledSql.clear();

        // 设置动作
        this.action = action;

        // 编译动作子句 + 修饰子句
        // 其中，sql收集编译好的语句，compiledSql.staticParams收集静态参数
        val sql = StringBuilder();
        this.compileAction(db, sql).compileDecoration(db, sql);

        // 收集编译好的sql
        compiledSql.sql = sql.toString()

        // 清空所有参数
        clear()

        return compiledSql
    }

    /****************************** 执行sql ********************************/
    /**
     * 查找结果： select 语句
     *
     * @param params
     * @param single 是否查一条
     * @param transform 结果转换函数
     * @return
     */
    public override fun <T> findResult(params: List<*>, single: Boolean, db: IDb, transform: (DbResultSet) -> T): T{
        if(single)
            limit(1)

        // 编译 + 执行
        return compile(SqlAction.SELECT, db).findResult(params, single, db, transform)
    }

    /**
     * 统计行数： count语句
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    public override fun count(params: List<*>, db: IDb):Int {
        // 1 编译
        selectColumns.clear() // 清空多余的select
        val csql = select(DbExpr("count(1)", "NUM", false) /* oracle会自动转为全大写 */).compile(SqlAction.SELECT, db);

        // 2 执行 select
        return csql.findValue<Int>(params, db)!!
    }

    /**
     * 加总列值： sum语句
     *
     * @param column 列
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    public override fun sum(column: String, params: List<*>, db: IDb):Int {
        // 1 编译
        selectColumns.clear() // 清空多余的select
        val csql = select(DbExpr("sum($column)", "NUM", false) /* oracle会自动转为全大写 */).compile(SqlAction.SELECT, db);

        // 2 执行 select
        return csql.findValue<Int>(params, db)!!
    }

    /**
     * 自增
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    public override fun incr(column: String, step: Int, params: List<*>, db: IDb): Boolean {
        // 1 编译
        set(column, DbExpr("$column + $step", false)) // Equals: set(column, "$column + $step", true)
        val csql = compile(SqlAction.UPDATE, db);

        // 2 执行 update
        return csql.execute(params, null, db) > 0
    }

    /**
     * 编译 + 执行
     *
     * @param action sql动作：select/insert/update/delete
     * @param params 参数
     * @param generatedColumn 返回的自动生成的主键名
     * @param db 数据库连接
     * @return 影响行数|新增id
     */
    public override fun execute(action: SqlAction, params:List<Any?>, generatedColumn:String?, db: IDb): Long {
        // 编译 + 执行
        return compile(action, db).execute(params, generatedColumn, db)
    }

    /**
     * 批量更新有参数的sql
     *
     * @param action sql动作：select/insert/update/delete
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param db 数据库连接
     * @return
     */
    public override fun batchExecute(action: SqlAction, paramses: List<Any?>, db: IDb): IntArray {
        // 编译 + 执行
        return compile(action, db).batchExecute(paramses, db)
    }
}