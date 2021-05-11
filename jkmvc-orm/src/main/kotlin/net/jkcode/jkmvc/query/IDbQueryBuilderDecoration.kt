package net.jkcode.jkmvc.query

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
     * @param   column  column name or DbExpr, also support column+operator: "age >=" / "name like"
     * @param   value   column value
     * @return
     */
    fun where(column: String, value: Any?): IDbQueryBuilder

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   column  column name or DbExpr, also support column+operator: "age >=" / "name like"
     * @param   value   column value
     * @return
     */
    fun orWhere(column: String, value: Any?): IDbQueryBuilder

    /**
     * Creates a new "WHERE BETWEEN" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   from   column value
     * @param   to   column value
     * @return
     */
    fun whereBetween(column: String, from: Any, to: Any): IDbQueryBuilder {
        return where(column, "BETWEEN", Pair(from, to))
    }

    /**
     * Creates a new "OR WHERE BETWEEN" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   from   column value
     * @param   to   column value
     * @return
     */
    fun orWhereBetween(column: String, from: Any, to: Any): IDbQueryBuilder {
        return orWhere(column, "BETWEEN", Pair(from, to))
    }

    /**
     * Creates a new "WHERE LIKE" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   value   column value
     * @return
     */
    fun whereLike(column: String, value: String): IDbQueryBuilder {
        val exp = if (value.contains('%')) value else "%$value%"
        return where(column, "LIKE", exp)
    }

    /**
     * Creates a new "OR WHERE LIKE" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   value   column value
     * @return
     */
    fun orWhereLike(column: String, value: String): IDbQueryBuilder {
        val exp = if (value.contains('%')) value else "%$value%"
        return orWhere(column, "LIKE", exp)
    }

    /**
     * Creates a new "OR WHERE EXISTS" condition for the query.
     *
     * @param   subquery 子查询
     * @return
     */
    fun whereExists(subquery: IDbQueryBuilder): IDbQueryBuilder

    /**
     * Creates a new "WHERE EXISTS" condition for the query.
     *
     * @param   subquery 子查询
     * @return
     */
    fun orWhereExists(subquery: IDbQueryBuilder): IDbQueryBuilder

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
     * Alias of andWhereCondition()
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    fun whereCondition(condition: String, params: List<*> = emptyList<Any>()): IDbQueryBuilder {
        return andWhereCondition(condition, params)
    }

    /**
     * Creates a new "AND WHERE" condition for the query.
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    fun andWhereCondition(condition: String, params: List<*> = emptyList<Any>()): IDbQueryBuilder

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    fun orWhereCondition(condition: String, params: List<*> = emptyList<Any>()): IDbQueryBuilder

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
     * Wrap a new "AND WHERE (...)" grouping.
     *
     * @return
     */
    fun andWhereWrap(action: IDbQueryBuilder.() -> Unit): IDbQueryBuilder{
        andWhereOpen()
        (this as IDbQueryBuilder).action()
        andWhereClose()
        return this
    }

    /**
     * Wrap a new "AND WHERE (...)" grouping.
     *
     * @return
     */
    fun whereWrap(action: IDbQueryBuilder.() -> Unit): IDbQueryBuilder{
        return andWhereWrap(action)
    }

    /**
     * Opens a new "OR WHERE (...)" grouping.
     *
     * @return
     */
    fun orWhereOpen(): IDbQueryBuilder;

    /**
     * Wrap a new "Or WHERE (...)" grouping.
     *
     * @return
     */
    fun orWhereWrap(action: IDbQueryBuilder.() -> Unit): IDbQueryBuilder{
        orWhereOpen()
        (this as IDbQueryBuilder).action()
        orWhereClose()
        return this
    }

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
     * Creates a "GROUP BY ..." filter.
     *
     * @param   columns  column name
     * @return
     */
    fun groupBys(columns: List<String>): IDbQueryBuilder {
        for (col in columns)
            groupBy(col)
        return this as IDbQueryBuilder
    }

    /**
     * Alias of andHaving()
     *
     * @param   column  column name or DbExpr, also support column+operator: "age >=" / "name like"
     * @param   value   column value
     * @return
     */
    fun having(column: String, value: Any?): IDbQueryBuilder

    /**
     * Alias of andHaving()
     *
     * @param   column  column name or DbExpr, also support column+operator: "age >=" / "name like"
     * @param   value   column value
     * @return
     */
    fun orHaving(column: String, value: Any?): IDbQueryBuilder

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
     * Alias of andHavingCondition()
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    fun havingCondition(condition: String, params: List<*> = emptyList<Any>()): IDbQueryBuilder {
        return andHavingCondition(condition)
    }

    /**
     * Creates a new "AND HAVING" condition for the query.
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    fun andHavingCondition(condition: String, params: List<*> = emptyList<Any>()): IDbQueryBuilder

    /**
     * Creates a new "OR HAVING" condition for the query.
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    fun orHavingCondition(condition: String, params: List<*> = emptyList<Any>()): IDbQueryBuilder

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
     *
     * @param   column     column name or DbExpr
     * @param   desc       whether desc direction,
     * @return
     */
    fun orderBy(column: String, desc: Boolean?): IDbQueryBuilder {
        var direction: String? = null
        if(desc != null)
            direction = if (desc) "DESC" else "ASC"
        return orderBy(column, direction)
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
     * 设置查询加锁
     *
     * @param value
     * @return
     */
    fun forUpdate(value: Boolean = true): IDbQueryBuilder
}