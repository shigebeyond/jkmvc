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
    public fun compileDecoration():String;

    /**
     * 改写转义值的方法，搜集sql参数
     *
     * @param mixed value
     * @return string
     */
    public fun quote(value:Any?):String;

    /**
     * 多个where条件
     * @param conditions:Map<String, Any?>
     * @return DbQueryBuilderDecoration
     */
    public fun wheres(conditions:Map<String, Any?>):IDbQueryBuilder;

    /**
     * 多个on条件
     * @param conditions:Map<String, String>
     * @return DbQueryBuilderDecoration
     */
    public fun ons(conditions:Map<String, String>):IDbQueryBuilder;

    /**
     * 多个having条件
     * @param conditions:Map<String, Any?>
     * @return DbQueryBuilderDecoration
     */
    public fun havings(conditions:Map<String, Any?>):IDbQueryBuilder;

    /**
     * Alias of andWhere()
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    public fun where(column:String, op:String, value:Any?):IDbQueryBuilder;

    /**
     * Creates a new "AND WHERE" condition for the query.
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    public fun andWhere(column:String, op:String, value:Any?):IDbQueryBuilder;

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    public fun orWhere(column:String, op:String, value:Any?):IDbQueryBuilder;

    /**
     * Alias of andWhereOpen()
     *
     * @return DbQueryBuilder
     */
    public fun whereOpen():IDbQueryBuilder;

    /**
     * Opens a new "AND WHERE (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public fun andWhereOpen():IDbQueryBuilder;

    /**
     * Opens a new "OR WHERE (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public fun orwhereOpen():IDbQueryBuilder;

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public fun whereclose():IDbQueryBuilder;

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public fun andWhereclose():IDbQueryBuilder;

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public fun orWhereclose():IDbQueryBuilder;

    /**
     * Creates a "GROUP BY ..." filter.
     *
     * @param   mixed   columns  column name or array(column, alias) or object
     * @return DbQueryBuilder
     */
    public fun groupBy(columns:String):IDbQueryBuilder;

    /**
     * Alias of andHaving()
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    public fun having(column:String, op:String, value:Any? = null):IDbQueryBuilder;

    /**
     * Creates a new "AND HAVING" condition for the query.
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    public fun andHaving(column:String, op:String, value:Any?):IDbQueryBuilder;

    /**
     * Creates a new "OR HAVING" condition for the query.
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    public fun orHaving(column:String, op:String, value:Any?):IDbQueryBuilder;

    /**
     * Alias of andHavingOpen()
     *
     * @return DbQueryBuilder
     */
    public fun havingOpen():IDbQueryBuilder;

    /**
     * Opens a new "AND HAVING (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public fun andHavingOpen():IDbQueryBuilder;

    /**
     * Opens a new "OR HAVING (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public fun orHavingOpen():IDbQueryBuilder;

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public fun havingClose():IDbQueryBuilder;

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public fun andHavingClose():IDbQueryBuilder;

    /**
     * Closes an open "OR HAVING (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public fun orHavingClose():IDbQueryBuilder;

    /**
     * Applies sorting with "ORDER BY ..."
     *
     * @param   mixed   column     column name or array(column, alias) or object
     * @param   string  direction  direction of sorting
     * @return DbQueryBuilder
     */
    public fun orderBy(column:String, direction:String? = null):IDbQueryBuilder;

    /**
     * Return up to "LIMIT ..." results
     *
     * @param   integer  limit
     * @param   integer  offset
     * @return DbQueryBuilder
     */
    public fun limit(limit:Int, offset:Int = 0):IDbQueryBuilder;

    /**
     * Adds addition tables to "JOIN ...".
     *
     * @param   mixed   table  column name or array(column, alias) or object
     * @param   string  type   join type (LEFT, RIGHT, INNER, etc)
     * @return DbQueryBuilder
     */
    public fun join(table:String, type:String? = null):IDbQueryBuilder;

    /**
     * Adds "ON ..." conditions for the last created JOIN statement.
     *
     * @param   mixed   c1  column name or array(column, alias) or object
     * @param   string  op  logic operator
     * @param   mixed   c2  column name or array(column, alias) or object
     * @return DbQueryBuilder
     */
    public fun on(c1:String, op:String, c2:String):IDbQueryBuilder;
}