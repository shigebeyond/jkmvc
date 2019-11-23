package net.jkcode.jkmvc.query

import net.jkcode.jkutil.common.isArrayOrCollection
import net.jkcode.jkmvc.db.IDb

/**
 * sql构建器 -- 修饰子句: 由修饰词where/group by/order by/limit来构建的子句
 *
 * @author shijianhang
 * @date 2016-10-12
 */
interface IDbQueryBuilderDecoration{

    /**
     * 编译修饰子句
     * @param db 数据库连接
     * @param sql 保存编译sql
     * @return
     */
    fun compileDecoration(db: IDb, sql: StringBuilder): IDbQueryBuilder;

    /**
     * 多个on条件
     * @param conditions
     * @return
     */
    fun ons(conditions: Map<String, String>): IDbQueryBuilder {
        for ((column, value) in conditions)
            on(column, "=", value);

        return this as IDbQueryBuilder
    }

    /**
     * 多个having条件
     * @param conditions
     * @return
     */
    fun havings(conditions: Map<String, Any?>): IDbQueryBuilder {
        for ((column, value) in conditions)
            having(column, "=", value);

        return this as IDbQueryBuilder
    }

    /**
     * Alias of andWhere()
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    fun where(column: String, op: String, value: Any?): IDbQueryBuilder {
        return andWhere(column, op, value);
    }

    /**
     * Alias of andWhere()
     *
     * @param   column  column name or DbExpr
     * @param   value   column value
     * @return
     */
    fun where(column: String, value: Any?): IDbQueryBuilder {
        if (value == null)
            return where(column, "IS", value);

        if (value.isArrayOrCollection())
            return where(column, "IN", value)

        return where(column, "=", value);
    }

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   value   column value
     * @return
     */
    fun orWhere(column: String, value: Any?): IDbQueryBuilder {
        if (value.isArrayOrCollection())
            return orWhere(column, "IN", value)

        return orWhere(column, "=", value);
    }

    /**
     * Creates a new "WHERE BETWEEN" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   value   column value
     * @return
     */
    fun whereBetween(column: String, value: Pair<Any, Any>): IDbQueryBuilder {
        return where(column, "BETWEEN", value)
    }

    /**
     * Creates a new "OR WHERE BETWEEN" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   value   column value
     * @return
     */
    fun orWhereBetween(column: String, value: Pair<Any, Any>): IDbQueryBuilder {
        return orWhere(column, "BETWEEN", value)
    }

    /**
     * Multiple Where
     *
     * @param   conditions
     * @return
     */
    fun wheres(conditions: Map<String, Any?>): IDbQueryBuilder {
        for ((column, value) in conditions)
            where(column, value)
        return this as IDbQueryBuilder
    }

    /**
     * Multiple Where
     *
     * @param   conditions
     * @return
     */
    fun wheres(conditions: List<Triple<String, String, Any?>>): IDbQueryBuilder {
        for ((column, op, value) in conditions)
            where(column, op, value)
        return this as IDbQueryBuilder
    }

    /**
     * Creates a new "AND WHERE" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    fun andWhere(column: String, op: String, value: Any?): IDbQueryBuilder;

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    fun orWhere(column: String, op: String, value: Any?): IDbQueryBuilder;

        /**
     * Alias of andWhereOpen()
     *
     * @return
     */
    fun whereOpen(): IDbQueryBuilder {
        return andWhereOpen();
    }

    /**
     * Opens a new "AND WHERE (...)" grouping.
     *
     * @return
     */
    fun andWhereOpen(): IDbQueryBuilder;

    /**
     * Opens a new "OR WHERE (...)" grouping.
     *
     * @return
     */
    fun orWhereOpen(): IDbQueryBuilder;

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    fun whereClose(): IDbQueryBuilder {
        return andWhereClose();
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    fun andWhereClose(): IDbQueryBuilder;

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    fun orWhereClose(): IDbQueryBuilder;

    /**
     * Creates a "GROUP BY ..." filter.
     *
     * @param   column  column name
     * @return
     */
    fun groupBy(column: String): IDbQueryBuilder;

    /**
     * Creates a "GROUP BY ..." filter.
     *
     * @param   columns  column name
     * @return
     */
    fun groupBys(vararg columns: String): IDbQueryBuilder {
        for (col in columns)
            groupBy(col)
        return this as IDbQueryBuilder
    }

    /**
     * Alias of andHaving()
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    fun having(column: String, op: String, value: Any?): IDbQueryBuilder {
        return andHaving(column, op, value);
    }

    /**
     * Creates a new "AND HAVING" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    fun andHaving(column: String, op: String, value: Any?): IDbQueryBuilder;

    /**
     * Creates a new "OR HAVING" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    fun orHaving(column: String, op: String, value: Any?): IDbQueryBuilder;

    /**
     * Alias of andHavingOpen()
     *
     * @return
     */
    fun havingOpen(): IDbQueryBuilder {
        return andHavingOpen();
    }

    /**
     * Opens a new "AND HAVING (...)" grouping.
     *
     * @return
     */
    fun andHavingOpen(): IDbQueryBuilder;

    /**
     * Opens a new "OR HAVING (...)" grouping.
     *
     * @return
     */
    fun orHavingOpen(): IDbQueryBuilder;

	/**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return
     */
    fun havingClose(): IDbQueryBuilder {
        return andHavingClose();
    }

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return
     */
    fun andHavingClose(): IDbQueryBuilder;

    /**
     * Closes an open "OR HAVING (...)" grouping.
     *
     * @return
     */
    fun orHavingClose(): IDbQueryBuilder;

    /**
     * Applies sorting with "ORDER BY ..."
     *
     * @param   column     column name or DbExpr
     * @param   asc        whether asc direction
     * @return
     */
    fun orderBy(column: String, asc: Boolean): IDbQueryBuilder {
        return orderBy(column, if (asc) "ASC" else "DESC")
    }

    /**
     * Applies sorting with "ORDER BY ..."
     *
     * @param   column     column name or DbExpr
     * @param   direction  direction of sorting
     * @return
     */
    fun orderBy(column: String, direction: String? = null): IDbQueryBuilder;

    /**
     * Multiple OrderBy
     *
     * @param orders
     * @return
     */
    fun orderBys(orders: Map<String, String?>): IDbQueryBuilder {
        for ((column, direction) in orders)
            orderBy(column, direction)
        return this as IDbQueryBuilder
    }

    /**
     * Multiple OrderBy
     *
     * @param columns
     * @return
     */
    fun orderBys(vararg columns: String): IDbQueryBuilder {
        for (col in columns)
            orderBy(col)
        return this as IDbQueryBuilder
    }

    /**
     * Return up to "LIMIT ..." results
     *
     * @param  limit
     * @param  offset
     * @return
     */
    fun limit(limit: Int, offset: Int = 0): IDbQueryBuilder;

    /**
     * Adds addition tables to "JOIN ...".
     *
     * @param   table  table name | DbExpr | subquery
     * @param   type   joinClause type (LEFT, RIGHT, INNER, etc)
     * @return
     */
    fun join(table: CharSequence, type: String = "INNER"): IDbQueryBuilder

    /**
     * Adds "ON ..." conditions for the last created JOIN statement.
     *
     * @param   c1  column name or DbExpr
     * @param   op  logic operator
     * @param   c2  column name or DbExpr
     * @return
     */
    fun on(c1: String, op: String, c2: String): IDbQueryBuilder;
}