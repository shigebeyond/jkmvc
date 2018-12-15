package com.jkmvc.db

/**
 * sql构建器
 *   依次继承 DbQueryBuilderAction 处理动作子句 + DbQueryBuilderDecoration 处理修饰子句
 *   提供select/where等类sql的方法, 但是调用方法时, 不直接拼接sql, 而是在compile()时才延迟拼接sql, 因为调用方法时元素可以无序, 但生成sql时元素必须有序
 *
 * @author shijianhang
 * @date 2016-10-13
 */
abstract class IDbQueryBuilder:IDbQueryBuilderAction, IDbQueryBuilderDecoration, Cloneable, CharSequence by "", IDbQuery() {

    /**
     * 克隆对象: 单纯用于改权限为public
     *
     * @return o
     */
    public override fun clone(): Any{
        return super.clone()
    }

    /**
     * 转义子查询
     *
     * @param subquery
     * @param alias
     * @return
     */
    public abstract fun quoteSubQuery(subquery: IDbQueryBuilder, alias: String? = null): String

    /**
     * 转义表名
     *
     * @param table
     * @return
     */
    public abstract fun quoteTable(table: CharSequence):String

    /****************************** 编译sql ********************************/
    /**
     * 编译sql
     * @param action sql动作：select/insert/update/delete
     * @param db 数据库连接
     * @return 编译好的sql
     */
    public abstract fun compile(action:SqlType, db: IDb = defaultDb): CompiledSql;

    /**
     * 编译select语句
     * @param db 数据库连接
     * @return 编译好的sql
     */
    public fun compileSelect(db: IDb = defaultDb): CompiledSql{
        return compile(SqlType.SELECT, db)
    }

    /**
     * 编译select ... limit 1语句
     * @param db 数据库连接
     * @return 编译好的sql
     */
    public fun compileSelectOne(db: IDb = defaultDb): CompiledSql{
        return limit(1).compile(SqlType.SELECT, db)
    }

    /**
     * 编译select count() 语句
     * @param db 数据库连接
     * @return 编译好的sql
     */
    public fun compileCount(db: IDb = defaultDb): CompiledSql{
        return select(DbExpr("count(1)", "NUM", false) /* oracle会自动转为全大写 */).compile(SqlType.SELECT, db)
    }

    /**
     * 编译insert语句
     * @param db 数据库连接
     * @return 编译好的sql
     */
    public fun compileInsert(db: IDb = defaultDb): CompiledSql{
        return compile(SqlType.INSERT, db)
    }

    /**
     * 编译update语句
     * @param db 数据库连接
     * @return 编译好的sql
     */
    public fun compileUpdate(db: IDb = defaultDb): CompiledSql{
        return compile(SqlType.UPDATE, db)
    }

    /**
     * 编译delete语句
     * @param db 数据库连接
     * @return 编译好的sql
     */
    public fun compileDelete(db: IDb = defaultDb): CompiledSql{
        return compile(SqlType.DELETE, db)
    }

    /****************************** 执行sql ********************************/
    /**
     * 统计行数： count语句
     *
     * @param params 动态参数
     * @param db 数据库连接
     * @return
     */
    public abstract fun count(params: List<Any?> = emptyList(), db: IDb = defaultDb):Int;

    /**
     * 编译 + 执行
     *
     * @param action sql动作：select/insert/update/delete
     * @param params 动态参数
     * @param generatedColumn 返回自动生成的主键名
     * @param db 数据库连接
     * @return 影响行数|新增id
     */
    public abstract fun execute(action:SqlType, params:List<Any?> = emptyList(), generatedColumn:String? = null, db: IDb = defaultDb):Int;

    /**
     * 插入：insert语句
     *
     *  @param generatedColumn 返回的自动生成的主键名
     *  @param params 动态参数
     *  @param db 数据库连接
     * @return 新增的id
     */
    public fun insert(generatedColumn:String? = null, params: List<Any?> = emptyList(), db: IDb = defaultDb):Int {
        return execute(SqlType.INSERT, params, generatedColumn, db);
    }

    /**
     * 更新：update语句
     *
     * @param params 动态参数
     * @param db 数据库连接
     * @return
     */
    public fun update(params: List<Any?> = emptyList(), db: IDb = defaultDb):Boolean {
        return execute(SqlType.UPDATE, params, null, db) > 0;
    }

    /**
     * 删除
     *
     * @param params 动态参数
     * @param db 数据库连接
     * @return
     */
    public fun delete(params: List<Any?> = emptyList(), db: IDb = defaultDb):Boolean {
        return execute(SqlType.DELETE, params, null, db) > 0;
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
    public abstract fun batchExecute(action:SqlType, paramses: List<Any?>, paramSize:Int, db: IDb = defaultDb): IntArray;

    /**
     * 批量插入
     *
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @param db 数据库连接
     * @return
     */
    public fun batchInsert(paramses: List<Any?>, paramSize:Int, db: IDb = defaultDb): IntArray {
        return batchExecute(SqlType.INSERT, paramses, paramSize, db)
    }

    /**
     * 批量更新
     *
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @param db 数据库连接
     * @return
     */
    public fun batchUpdate(paramses: List<Any?>, paramSize:Int, db: IDb = defaultDb): IntArray {
        return batchExecute(SqlType.UPDATE, paramses, paramSize, db)
    }

    /**
     * 批量插入
     *
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @param db 数据库连接
     * @return
     */
    public fun batchDelete(paramses: List<Any?>, paramSize:Int, db: IDb = defaultDb): IntArray {
        return batchExecute(SqlType.DELETE, paramses, paramSize, db)
    }
}