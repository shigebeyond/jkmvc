package com.jkmvc.db

/**
 * 修饰子句的类型
 * @author shijianhang
 * @date 2016-10-10
 */
enum class ClauseType {
    WHERE,
    GROUP_BY,
    HAVING,
    ORDER_BY,
    LIMIT
}

/**
 * sql构建器 -- 修饰子句: 由修饰词where/group by/order by/limit来构建的子句
 *
 * @author shijianhang
 * @date 2016-10-12
 */
interface IDbQueryBuilderDecoration
{
    /**
     * 编译修饰子句
     *
     * @param sql 保存编译sql
     * @return
     */
    fun compileDecoration(sql: StringBuilder): IDbQueryBuilder;

    /**
     * 改写转义值的方法，搜集sql参数
     *
     * @param value
     * @return
     */
    fun quote(value:Any?):String;

    /**
     * 多个where条件
     * @param conditions
     * @return
     */
    fun wheres(conditions:Map<String, Any?>):IDbQueryBuilder;

    /**
     * 多个on条件
     * @param conditions:Map<String, String>
     * @return
     */
    fun ons(conditions:Map<String, String>):IDbQueryBuilder;

    /**
     * 多个having条件
     * @param conditions
     * @return
     */
    fun havings(conditions:Map<String, Any?>):IDbQueryBuilder;

    /**
     * Alias of andWhere()
     *
     * @param   column  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    fun where(column:String, op:String, value:Any?):IDbQueryBuilder;

    /**
     * Alias of andWhere()
     *
     * @param   column  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    fun where(column:String, value:Any?):IDbQueryBuilder{
        return where(column, "=", value);
    }

    /**
     * Creates a new "AND WHERE" condition for the query.
     *
     * @param   column  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    fun andWhere(column:String, op:String, value:Any?):IDbQueryBuilder;

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   column  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    fun orWhere(column:String, op:String, value:Any?):IDbQueryBuilder;

    /**
     * Alias of andWhereOpen()
     *
     * @return
     */
    fun whereOpen():IDbQueryBuilder;

    /**
     * Opens a new "AND WHERE (...)" grouping.
     *
     * @return
     */
    fun andWhereOpen():IDbQueryBuilder;

    /**
     * Opens a new "OR WHERE (...)" grouping.
     *
     * @return
     */
    fun orwhereOpen():IDbQueryBuilder;

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    fun whereclose():IDbQueryBuilder;

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    fun andWhereclose():IDbQueryBuilder;

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    fun orWhereclose():IDbQueryBuilder;

    /**
     * Creates a "GROUP BY ..." filter.
     *
     * @param   columns  column name or array(column, alias) or object
     * @return
     */
    fun groupBy(columns:String):IDbQueryBuilder;

    /**
     * Alias of andHaving()
     *
     * @param   column  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    fun having(column:String, op:String, value:Any? = null):IDbQueryBuilder;

    /**
     * Creates a new "AND HAVING" condition for the query.
     *
     * @param   column  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    fun andHaving(column:String, op:String, value:Any?):IDbQueryBuilder;

    /**
     * Creates a new "OR HAVING" condition for the query.
     *
     * @param   column  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    fun orHaving(column:String, op:String, value:Any?):IDbQueryBuilder;

    /**
     * Alias of andHavingOpen()
     *
     * @return
     */
    fun havingOpen():IDbQueryBuilder;

    /**
     * Opens a new "AND HAVING (...)" grouping.
     *
     * @return
     */
    fun andHavingOpen():IDbQueryBuilder;

    /**
     * Opens a new "OR HAVING (...)" grouping.
     *
     * @return
     */
    fun orHavingOpen():IDbQueryBuilder;

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return
     */
    fun havingClose():IDbQueryBuilder;

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return
     */
    fun andHavingClose():IDbQueryBuilder;

    /**
     * Closes an open "OR HAVING (...)" grouping.
     *
     * @return
     */
    fun orHavingClose():IDbQueryBuilder;

    /**
     * Applies sorting with "ORDER BY ..."
     *
     * @param   column     column name or array(column, alias) or object
     * @param   direction  direction of sorting
     * @return
     */
    fun orderBy(column:String, direction:String? = null):IDbQueryBuilder;

    /**
     * Return up to "LIMIT ..." results
     *
     * @param  limit
     * @param  offset
     * @return
     */
    fun limit(limit:Int, offset:Int = 0):IDbQueryBuilder;

    /**
     * Adds addition tables to "JOIN ...".
     *
     * @param   table  column name or array(column, alias) or object
     * @param   type   joinClause type (LEFT, RIGHT, INNER, etc)
     * @return
     */
    fun join(table:Any, type:String? = null):IDbQueryBuilder;

    /**
     * Adds "ON ..." conditions for the last created JOIN statement.
     *
     * @param   c1  column name or array(column, alias) or object
     * @param   op  logic operator
     * @param   c2  column name or array(column, alias) or object
     * @return
     */
    fun on(c1:String, op:String, c2:String):IDbQueryBuilder;
}