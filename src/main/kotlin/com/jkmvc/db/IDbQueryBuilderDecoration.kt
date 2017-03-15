package com.jkmvc.db

/**
 * sql构建器 -- 修饰子句: 由修饰词where/group by/order by/limit来构建的子句
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-12
 *
 */
interface IDbQueryBuilderDecoration
{
    /**
     * 编译修饰子句
     * @return string
     */
    fun compileDecoration():String;

    /**
     * 改写转义值的方法，搜集sql参数
     *
     * @param mixed value
     * @return string
     */
    fun quote(value:Any?):String;

    /**
     * 多个where条件
     * @param conditions:Map<String, Any?>
     * @return DbQueryBuilderDecoration
     */
    fun wheres(conditions:Map<String, Any?>):IDbQueryBuilder;

    /**
     * 多个on条件
     * @param conditions:Map<String, String>
     * @return DbQueryBuilderDecoration
     */
    fun ons(conditions:Map<String, String>):IDbQueryBuilder;

    /**
     * 多个having条件
     * @param conditions:Map<String, Any?>
     * @return DbQueryBuilderDecoration
     */
    fun havings(conditions:Map<String, Any?>):IDbQueryBuilder;

    /**
     * Alias of andWhere()
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    fun where(column:String, op:String, value:Any?):IDbQueryBuilder;

    /**
     * Alias of andWhere()
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    fun where(column:String, value:Any?):IDbQueryBuilder{
        return where(column, "=", value);
    }

    /**
     * Creates a new "AND WHERE" condition for the query.
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    fun andWhere(column:String, op:String, value:Any?):IDbQueryBuilder;

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    fun orWhere(column:String, op:String, value:Any?):IDbQueryBuilder;

    /**
     * Alias of andWhereOpen()
     *
     * @return DbQueryBuilder
     */
    fun whereOpen():IDbQueryBuilder;

    /**
     * Opens a new "AND WHERE (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    fun andWhereOpen():IDbQueryBuilder;

    /**
     * Opens a new "OR WHERE (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    fun orwhereOpen():IDbQueryBuilder;

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    fun whereclose():IDbQueryBuilder;

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    fun andWhereclose():IDbQueryBuilder;

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    fun orWhereclose():IDbQueryBuilder;

    /**
     * Creates a "GROUP BY ..." filter.
     *
     * @param   mixed   columns  column name or array(column, alias) or object
     * @return DbQueryBuilder
     */
    fun groupBy(columns:String):IDbQueryBuilder;

    /**
     * Alias of andHaving()
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    fun having(column:String, op:String, value:Any? = null):IDbQueryBuilder;

    /**
     * Creates a new "AND HAVING" condition for the query.
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    fun andHaving(column:String, op:String, value:Any?):IDbQueryBuilder;

    /**
     * Creates a new "OR HAVING" condition for the query.
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    fun orHaving(column:String, op:String, value:Any?):IDbQueryBuilder;

    /**
     * Alias of andHavingOpen()
     *
     * @return DbQueryBuilder
     */
    fun havingOpen():IDbQueryBuilder;

    /**
     * Opens a new "AND HAVING (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    fun andHavingOpen():IDbQueryBuilder;

    /**
     * Opens a new "OR HAVING (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    fun orHavingOpen():IDbQueryBuilder;

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    fun havingClose():IDbQueryBuilder;

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    fun andHavingClose():IDbQueryBuilder;

    /**
     * Closes an open "OR HAVING (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    fun orHavingClose():IDbQueryBuilder;

    /**
     * Applies sorting with "ORDER BY ..."
     *
     * @param   mixed   column     column name or array(column, alias) or object
     * @param   string  direction  direction of sorting
     * @return DbQueryBuilder
     */
    fun orderBy(column:String, direction:String? = null):IDbQueryBuilder;

    /**
     * Return up to "LIMIT ..." results
     *
     * @param   integer  limit
     * @param   integer  offset
     * @return DbQueryBuilder
     */
    fun limit(limit:Int, offset:Int = 0):IDbQueryBuilder;

    /**
     * Adds addition tables to "JOIN ...".
     *
     * @param   mixed   table  column name or array(column, alias) or object
     * @param   string  type   join type (LEFT, RIGHT, INNER, etc)
     * @return DbQueryBuilder
     */
    fun join(table:Any, type:String? = null):IDbQueryBuilder;

    /**
     * Adds "ON ..." conditions for the last created JOIN statement.
     *
     * @param   mixed   c1  column name or array(column, alias) or object
     * @param   string  op  logic operator
     * @param   mixed   c2  column name or array(column, alias) or object
     * @return DbQueryBuilder
     */
    fun on(c1:String, op:String, c2:String):IDbQueryBuilder;
}