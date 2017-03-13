package com.jkmvc.db

import java.util.*
import kotlin.jvm.internal.FunctionImpl

/**
 * sql构建器 -- 修饰子句: 由修饰词where/group by/order by/limit来构建的子句
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-12
 *
 */
abstract class DbQueryBuilderDecoration(db: IDb/* 数据库连接 */, table: String = "" /*表名*/) : DbQueryBuilderAction(db, table) {
    /**
     * 转义列
     */
    protected val columnQuoter: (String) -> String = { value: String ->
        db.quoteColumn(value);
    }

    /**
     * 转义值
     */
    protected val valueQuoter: (Any?) -> String = { value: Any? ->
        quote(value);
    }

    /**
     * 转义表
     */
    protected val tableQuoter: (String) -> String = { value: String ->
        db.quoteTable(value);
    }

    protected val orderDirection: (String?) -> String = { value: String? ->
        if (value != null && "^(ASC|DESC)$".toRegex().matches(value))
            value;
        else
            "";
    }

    /**
     * 条件数组, 每个条件 = 字段名 + 运算符 + 字段值
     * @var DbQueryBuilderDecorationClausesGroup
     */
    protected val where = DbQueryBuilderDecorationClausesGroup("WHERE", arrayOf<FunctionImpl?>(columnQuoter as FunctionImpl, null, valueQuoter as FunctionImpl));

    /**
     * 字段数组
     * @var DbQueryBuilderDecorationClausesSimple
     */
    protected val groupBy = DbQueryBuilderDecorationClausesSimple("GROUP BY", arrayOf<FunctionImpl?>(columnQuoter as FunctionImpl));

    /**
     * 条件数组, 每个条件 = 字段名 + 运算符 + 字段值
     * @var DbQueryBuilderDecorationClausesGroup
     */
    protected val having = DbQueryBuilderDecorationClausesGroup("HAVING", arrayOf<FunctionImpl?>(columnQuoter as FunctionImpl, null, valueQuoter as FunctionImpl));

    /**
     * 排序数组, 每个排序 = 字段+方向
     * @var DbQueryBuilderDecorationClausesSimple
     */
    protected val orderBy = DbQueryBuilderDecorationClausesSimple("ORDER BY", arrayOf<FunctionImpl?>(columnQuoter as FunctionImpl, orderDirection as FunctionImpl));

    /**
     * 行限数组 limit, offset
     * @var DbQueryBuilderDecorationClausesSimple
     */
    protected val limit = DbQueryBuilderDecorationClausesSimple("LIMIT", arrayOf<FunctionImpl?>(null));

    /**
     * 联表数组，每个联表join = 表名 + 联表方式 | 每个联表条件on = 字段 + 运算符 + 字段, 都写死在DbQueryBuilderDecorationClausesJoin类
     * @var array
     */
    protected val join: MutableList<DbQueryBuilderDecorationClausesGroup> by lazy {
        LinkedList<DbQueryBuilderDecorationClausesGroup>()
    };

    /**
     * 修饰子句
     */
    public fun visitDecorationClauses(visitor: (IDbQueryBuilderDecorationClauses<*>) -> Unit) {
        // 逐个处理修饰词及其表达式
        for (j in join)
            visitor(j);

        visitor(where);
        visitor(groupBy);
        visitor(having);
        visitor(orderBy);
        visitor(limit);
    }

    /**
     * 编译修饰子句
     * @return string
     */
    public override fun compileDecoration(): String {
        val sql: StringBuilder = StringBuilder();
        // 逐个编译修饰表达式
        visitDecorationClauses { clause: IDbQueryBuilderDecorationClauses<*> ->
            sql.append(clause.compile()).append(' ');
        }
        return sql.toString();
    }

    /**
     * 清空条件
     * @return DbQueryBuilder
     */
    public override fun clear(): IDbQueryBuilder {
        // 逐个清空修饰表达式
        visitDecorationClauses { clause: IDbQueryBuilderDecorationClauses<*> ->
            clause.clear();
        }

        join.clear();

        return super.clear();
    }

    /**
     * 改写转义值的方法，搜集sql参数
     *
     * @param mixed value
     * @return string
     */
    public override fun quote(value: Any?): String {
        // 1 将参数值直接拼接到sql
        //return db.quote(value);

        // 2 sql参数化: 将参数名拼接到sql, 独立出参数值, 以便执行时绑定参数值
        params.add(value);
        return "?";
    }

    /**
     * 检查是否是sql操作符
     *
     * @param    string
     * @return    bool
     */
    public fun isOperator(str: String): Boolean {
        return "(\\s|<|>|!|=|is|is not)".toRegex(RegexOption.IGNORE_CASE).matches(str);
    }

    /**
     * 多个where条件
     * @param conditions:Map<String, Any?>
     * @return DbQueryBuilder
     */
    public override fun wheres(conditions: Map<String, Any?>): IDbQueryBuilder {
        for ((column, value) in conditions)
            where(column, "=", value);

        return this;
    }

    /**
     * 多个on条件
     * @param conditions:Map<String, String>
     * @return DbQueryBuilder
     */
    public override fun ons(conditions: Map<String, String>): IDbQueryBuilder {
        for ((column, value) in conditions)
            on(column, "=", value);

        return this;
    }

    /**
     * 多个having条件
     * @param conditions:Map<String, Any?>
     * @return DbQueryBuilder
     */
    public override fun havings(conditions: Map<String, Any?>): IDbQueryBuilder {
        for ((column, value) in conditions)
            having(column, "=", value);

        return this;
    }

    /**
     * Alias of andWhere()
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    public override fun where(column: String, op: String, value: Any?): IDbQueryBuilder {
        return andWhere (column, op, value);
    }

    /**
     * Creates a new "AND WHERE" condition for the query.
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    public override fun andWhere(column: String, op: String, value: Any?): IDbQueryBuilder {
        where.addSubexp(arrayOf<Any?>(column, prepareOperator(value, op), value), "AND");
        return this;
    }

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    public override fun orWhere(column: String, op: String, value: Any?): IDbQueryBuilder {
        where.addSubexp(arrayOf<Any?>(column, prepareOperator(value, op), value), "OR");
        return this;

    }

    /**
     * Prepare operator
     *
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilderDecoration
     */
    protected fun prepareOperator(value: Any?, op: String = "="): String {
        if (value == null && op == "=") // IS null
            return "IS";

        return op;
    }


    /**
     * Alias of andWhereOpen()
     *
     * @return DbQueryBuilder
     */
    public override fun whereOpen(): IDbQueryBuilder {
        return andWhereOpen();
    }

    /**
     * Opens a new "AND WHERE (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public override fun andWhereOpen(): IDbQueryBuilder {
        where.open("AND");
        return this;
    }

    /**
     * Opens a new "OR WHERE (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public override fun orwhereOpen(): IDbQueryBuilder {
        where.open("OR");
        return this;
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public override fun whereclose(): IDbQueryBuilder {
        return andWhereclose();
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public override fun andWhereclose(): IDbQueryBuilder {
        where.close();
        return this;
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public override fun orWhereclose(): IDbQueryBuilder {
        where.close();
        return this;
    }

    /**
     * Creates a "GROUP BY ..." filter.
     *
     * @param   mixed   columns  column name or array(column, alias) or object
     * @return DbQueryBuilder
     */
    public override fun groupBy(column: String): IDbQueryBuilder {
        groupBy.addSubexp(arrayOf(column));
        return this;
    }

    /**
     * Alias of andHaving()
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    public override fun having(column: String, op: String, value: Any?): IDbQueryBuilder {
        return andHaving(column, op, value);
    }

    /**
     * Creates a new "AND HAVING" condition for the query.
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    public override fun andHaving(column: String, op: String, value: Any?): IDbQueryBuilder {
        having.addSubexp(arrayOf<Any?>(column, prepareOperator(value, op), value), "AND");
        return this;
    }

    /**
     * Creates a new "OR HAVING" condition for the query.
     *
     * @param   mixed   column  column name or array(column, alias) or object
     * @param   string  op      logic operator
     * @param   mixed   value   column value
     * @return DbQueryBuilder
     */
    public override fun orHaving(column: String, op: String, value: Any?): IDbQueryBuilder {
        having.addSubexp(arrayOf<Any?>(column, prepareOperator(value, op), value), "OR");
        return this;
    }

    /**
     * Alias of andHavingOpen()
     *
     * @return DbQueryBuilder
     */
    public override fun havingOpen(): IDbQueryBuilder {
        return andHavingOpen();
    }

    /**
     * Opens a new "AND HAVING (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public override fun andHavingOpen(): IDbQueryBuilder {
        where.open("AND");
        return this;
    }

    /**
     * Opens a new "OR HAVING (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public override fun orHavingOpen(): IDbQueryBuilder {
        where.open("OR");
        return this;
    }

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public override fun havingClose(): IDbQueryBuilder {
        return andHavingClose();
    }

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public override fun andHavingClose(): IDbQueryBuilder {
        where.close();
        return this;
    }

    /**
     * Closes an open "OR HAVING (...)" grouping.
     *
     * @return DbQueryBuilder
     */
    public override fun orHavingClose(): IDbQueryBuilder {
        where.close();
        return this;
    }

    /**
     * Applies sorting with "ORDER BY ..."
     *
     * @param   mixed   column     column name or array(column, alias) or object
     * @param   string  direction  direction of sorting
     * @return DbQueryBuilder
     */
    public override fun orderBy(column: String, direction: String?): IDbQueryBuilder {
        orderBy.addSubexp(arrayOf<Any?>(column, direction));
        return this;
    }

    /**
     * Return up to "LIMIT ..." results
     *
     * @param   integer  limit
     * @param   integer  offset
     * @return DbQueryBuilder
     */
    public override fun limit(limit: Int, offset: Int): IDbQueryBuilder {
        if (offset === 0)
            this.limit.addSubexp(arrayOf<Any?>(limit));
        else
            this.limit.addSubexp(arrayOf<Any?>(offset, limit));

        return this;
    }

    /**
     * Adds addition tables to "JOIN ...".
     *
     * @param   mixed   table  column name or array(column, alias) or object
     * @param   string  type   join type (LEFT, RIGHT, INNER, etc)
     * @return DbQueryBuilder
     */
    public override fun join(table: String, type: String?): IDbQueryBuilder {
        // join　子句
        val j = DbQueryBuilderDecorationClausesGroup("$type JOIN", arrayOf<FunctionImpl?>(tableQuoter as FunctionImpl));
        j.addSubexp(arrayOf<Any?>(table));

        // on　子句
        val on = DbQueryBuilderDecorationClausesGroup("ON", arrayOf<FunctionImpl?>(columnQuoter as FunctionImpl, null, columnQuoter as FunctionImpl));

        join.add(j);
        join.add(on);

        return this;
    }

    /**
     * Adds "ON ..." conditions for the last created JOIN statement.
     *
     * @param   mixed   c1  column name or array(column, alias) or object
     * @param   string  op  logic operator
     * @param   mixed   c2  column name or array(column, alias) or object
     * @return DbQueryBuilder
     */
    public override fun on(c1: String, op: String, c2: String): IDbQueryBuilder {
        join.last().addSubexp(arrayOf(c1, op, c2), "AND");
        return this;
    }
}