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

    /****************************** 编译sql ********************************/
    /**
     * 编译sql
     * @param action sql动作：select/insert/update/delete
     * @return 编译好的sql
     */
    public abstract fun compile(action:SqlType): CompiledSql;

    /**
     * 编译select语句
     * @return 编译好的sql
     */
    public abstract fun compileSelect(): CompiledSql

    /**
     * 编译select ... limit 1语句
     * @return 编译好的sql
     */
    public abstract fun compileSelectOne(): CompiledSql

    /**
     * 编译select count() 语句
     * @return 编译好的sql
     */
    public abstract fun compileCount(): CompiledSql

    /**
     * 编译insert语句
     * @return 编译好的sql
     */
    public abstract fun compileInsert(): CompiledSql

    /**
     * 编译update语句
     * @return 编译好的sql
     */
    public abstract fun compileUpdate(): CompiledSql

    /**
     * 编译delete语句
     * @return 编译好的sql
     */
    public abstract fun compileDelete(): CompiledSql

    /****************************** 执行sql ********************************/
    /**
     * 统计行数： count语句
     *
     * @param params 动态参数
     * @param db 数据库连接
     * @return
     */
    public abstract fun count(vararg params: Any?, db: IDb):Int;

    /**
     * 批量插入
     *
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @param db 数据库连接
     * @return
     */
    public abstract fun batchInsert(paramses: List<Any?>, paramSize:Int, db: IDb): IntArray;

    /**
     * 插入：insert语句
     *
     * @param generatedColumn 返回的自动生成的主键名
     * @param params 动态参数
     * @param db 数据库连接
     * @return 影响行数|新增的id
     */
    public abstract fun insert(generatedColumn:String? = null, vararg params: Any?, db: IDb):Int;

    /**
     * 更新：update语句
     *
     * @param params 动态参数
     * @param db 数据库连接
     * @return
     */
    public abstract fun update(vararg params: Any?, db: IDb):Boolean;

    /**
     * 删除
     *
     * @param params 动态参数
     * @param db 数据库连接
     * @return
     */
    public abstract fun delete(vararg params: Any?, db: IDb):Boolean;

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
}