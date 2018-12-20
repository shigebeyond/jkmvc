package com.jkmvc.db

/**
 * sql构建器 -- 动作子句: 由动态select/insert/update/delete来构建的子句
 *   通过字符串模板来实现
 *
 * @author shijianhang
 * @date 2016-10-12
 */
interface IDbQueryBuilderAction {

    /**
     * 编译动作子句
     *
     * @param db 数据库连接
     * @param sql 保存编译的sql
     * @return
     */
    fun compileAction(db: IDb, sql: StringBuilder): IDbQueryBuilder;

    /**
     * 设置表名
     *
     * @param table 表名
     * @param alias 别名
     * @return
     */
    fun table(table:String, alias:String? = null):IDbQueryBuilder{
        return from(table, alias)
    }

    /**
     * 设置表名
     *
     * @param table 表名
     * @return
     */
    fun from(table: DbExpr): IDbQueryBuilder

    /**
     * 设置表名
     *
     * @param table 表名
     * @param alias 别名
     * @return
     */
    fun from(table:String, alias:String? = null):IDbQueryBuilder{
        return from(DbExpr(table, alias))
    }

    /**
     * 设置表名
     *
     * @param subquery 子查询
     * @param alias 别名
     * @return
     */
    fun from(subquery: IDbQueryBuilder, alias:String): IDbQueryBuilder{
        return from(DbExpr(subquery, alias))
    }

    /**
     * 设置插入的列, insert时用
     *
     * @param column
     * @return
     */
    fun insertColumns(vararg colums:String):IDbQueryBuilder;

    /**
     * 设置插入的单行, insert时用
     *
     * @param row
     * @return
     */
    fun value(vararg row:Any?):IDbQueryBuilder;

    /**
     * 设置插入的子查询, insert时用
     *
     * @param row 单行数据
     * @return
     */
    fun values(subquery: IDbQueryBuilder): IDbQueryBuilder

    /**
     * 设置插入的单行, insert时用
     *
     * @param row
     * @return
     */
    fun value(row:Row):IDbQueryBuilder;

    /**
     * 设置更新的单个值, update时用
     *
     * @param column
     * @param value
     * @return
     */
    fun set(column:String, value:Any?):IDbQueryBuilder;

    /**
     * 设置更新的单个值, update时用
     *
     * @param column
     * @param value
     * @param isExpr 是否db表达式
     * @return
     */
    fun set(column:String, value:String, isExpr: Boolean = false):IDbQueryBuilder;

    /**
     * 设置更新的多个值, update时用
     *
     * @param row
     * @return
     */
    fun sets(row:Row):IDbQueryBuilder;

    /**
     * 设置查询的字段, select时用
     *
     * @param columns 字段名数组，其元素类型是 String 或 DbExpr
     *                如 arrayOf(column1, column2, DbExpr(column3, alias)),
     * 				  如 arrayOf("name", "age", DbExpr("birthday", "birt"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
     * @return
     */
    fun select(vararg columns:CharSequence):IDbQueryBuilder;

    /**
     * 设置查询结果是否去重唯一
     *
     * @param value
     * @returnAction
     */
    fun distinct(value:Boolean = true):IDbQueryBuilder;

    /**
     * 清空条件
     * @return
     */
    fun clear():IDbQueryBuilder;
}