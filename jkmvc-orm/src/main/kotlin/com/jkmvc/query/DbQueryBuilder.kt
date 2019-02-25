package com.jkmvc.query

import com.jkmvc.db.*
import kotlin.reflect.KClass

/**
 * sql构建器
 *   依次继承 DbQueryBuilderAction 处理动作子句 + DbQueryBuilderDecoration 处理修饰子句
 *  提供select/where等类sql的方法, 但是调用方法时, 不直接拼接sql, 而是在compile()时才延迟拼接sql, 因为调用方法时元素可以无序, 但生成sql时元素必须有序
 *
 * @author shijianhang
 * @date 2016-10-13
 */
open class DbQueryBuilder(public override val defaultDb: IDb = Db.instance()) : DbQueryBuilderDecoration() {

    /****************************** 编译sql ********************************/
    /**
     * 编译sql
     * @param action sql动作：select/insert/update/delete
     * @param db 数据库连接
     * @return 编译好的sql
     */
    public override fun compile(action: SqlType, db: IDb): CompiledSql {
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

        return compiledSql
    }

    /****************************** 执行sql ********************************/
    /**
     * 查找多个： select 语句
     *
     * @param params 参数
     * @param db 数据库连接
     * @param transform 转换函数
     * @return 列表
     */
    public override fun <T:Any> findAll(params: List<Any?>, db: IDb, transform: (Row) -> T): List<T>{
        // 编译 + 执行
        return compile(SqlType.SELECT, db).findAll(params, db, transform)
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param params 参数
     * @param db 数据库连接
     * @param transform 转换函数
     * @return 单个数据
     */
    public override fun <T:Any> find(params: List<Any?>, db: IDb, transform: (Row) -> T): T?{
        // 编译 + 执行
        return compileSelectOne(db).find(params, db, transform)
    }

    /**
     * 查询一列（多行）
     *
     * @param params 参数
     * @param clazz 值类型
     * @param db 数据库连接
     * @return
     */
    public override fun <T:Any> findColumn(params: List<Any?>, clazz: KClass<T>?, db: IDb): List<T?>{
        // 编译 + 执行
        return compile(SqlType.SELECT, db).findColumn(params, clazz, db)
    }

    /**
     * 查询一行一列
     *
     * @param params 参数
     * @param clazz 值类型
     * @param db 数据库连接
     * @return
     */
    public override fun <T:Any> findCell(params: List<Any?>, clazz: KClass<T>?, db: IDb): Cell<T> {
        // 编译 + 执行
        return compile(SqlType.SELECT, db).findCell(params, clazz, db)
    }

    /**
     * 统计行数： count语句
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    public override fun count(params: List<Any?>, db: IDb):Int {
        // 1 编译
        selectColumns.clear() // 清空多余的select
        val csql = select(DbExpr("count(1)", "NUM", false) /* oracle会自动转为全大写 */).compile(SqlType.SELECT, db);

        // 2 执行 select
        return csql.findCell<Int>(params, db).get()!!
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
    public override fun execute(action: SqlType, params:List<Any?>, generatedColumn:String?, db: IDb):Int {
        // 编译 + 执行
        return compile(action, db).execute(params, generatedColumn, db)
    }

    /**
     * 批量更新有参数的sql
     *
     * @param action sql动作：select/insert/update/delete
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @param db 数据库连接
     * @return
     */
    public override fun batchExecute(action: SqlType, paramses: List<Any?>, paramSize:Int, db: IDb): IntArray {
        // 编译 + 执行
        return compile(action, db).batchExecute(paramses, paramSize, db)
    }
}