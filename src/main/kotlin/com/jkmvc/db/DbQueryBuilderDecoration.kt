package com.jkmvc.db

import com.jkmvc.common.getOrPut
import java.util.*

/**
 * sql构建器 -- 修饰子句: 由修饰词where/group by/order by/limit来构建的子句
 *
 * @author shijianhang
 * @date 2016-10-12
 */
abstract class DbQueryBuilderDecoration(db: IDb, table: String = "" /*表名*/) : DbQueryBuilderAction(db, table) {

    /**
     * 转义列
     */
    protected val columnQuoter: (Any?) -> String = { value: Any? ->
        db.quoteColumn(value as String);
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
    protected val tableQuoter: (Any?) -> String = { value: Any? ->
        db.quoteTable(value as String);
    }

    protected val orderDirection: (Any?) -> String = { value: Any? ->
        if (value != null && "^(ASC|DESC)$".toRegex().matches(value as String))
            value;
        else
            "";
    }

    /**
     * where/group by/having/order by/limit
     */
    protected val clauses: Array<DbQueryBuilderDecorationClauses<*>?> = arrayOfNulls<DbQueryBuilderDecorationClauses<*>>(5);

    /**
     * 联表数组，每个联表join = 表名 + 联表方式 | 每个联表条件on = 字段 + 运算符 + 字段, 都写死在DbQueryBuilderDecorationClausesJoin类
     */
    protected val join: MutableList<DbQueryBuilderDecorationClausesGroup> by lazy(LazyThreadSafetyMode.NONE) {
        LinkedList<DbQueryBuilderDecorationClausesGroup>()
    };

    /**
     * 获得修饰子句
     *   主要用于首次使用的初始化，避免创建时就初始化
     *   原来用map，但考虑到每个QueryBuild都要创建map影响性能，因此改用array
     */
    protected fun getClause(type:ClauseType):DbQueryBuilderDecorationClauses<*>{
        return clauses.getOrPut(type.ordinal){
            buildClause(type)
        }!!
    }

    /**
     * 构建修饰子句
     */
    protected fun buildClause(type:ClauseType):DbQueryBuilderDecorationClauses<*>{
        return when(type) {
            //条件数组, 每个条件 = 字段名 + 运算符 + 字段值
            ClauseType.WHERE -> DbQueryBuilderDecorationClausesGroup("WHERE", arrayOf<((Any?) -> String)?>(columnQuoter, null, valueQuoter));
            //字段数组
            ClauseType.GROUP_BY -> DbQueryBuilderDecorationClausesSimple("GROUP BY", arrayOf<((Any?) -> String)?>(columnQuoter));
            //条件数组, 每个条件 = 字段名 + 运算符 + 字段值
            ClauseType.HAVING -> DbQueryBuilderDecorationClausesGroup("HAVING", arrayOf<((Any?) -> String)?>(columnQuoter, null, valueQuoter));
            //排序数组, 每个排序 = 字段+方向
            ClauseType.ORDER_BY -> DbQueryBuilderDecorationClausesSimple("ORDER BY", arrayOf<((Any?) -> String)?>(columnQuoter, orderDirection));
            //行限数组 limit, offset
            ClauseType.LIMIT -> DbQueryBuilderDecorationClausesSimple("LIMIT", arrayOf<((Any?) -> String)?>(null));
            else -> throw DbException("未知修饰词[$type]");
        }
    }

    /**
     * 遍历修饰子句
     * @param visitor 访问者函数，遍历时调用
     */
    protected fun travelDecorationClauses(visitor: (IDbQueryBuilderDecorationClauses<*>) -> Unit):Unit {
        // 逐个处理修饰词及其表达式
        // 1 join
        for (j in join)
            visitor(j);

        // 2 where/group by/having/ordery by/limit 按顺序编译sql，否则sql无效
        /*for ((k, v) in clauses) // map中无序
            visitor(v);*/
        for(v in clauses) // array中有序，下标就是序号
            if(v != null)
                visitor(v);
    }

    /**
     * 编译修饰子句
     *
     * @param sql 保存编译的sql
     * @return
     */
    public override fun compileDecoration(sql: StringBuilder): IDbQueryBuilder{
        sql.append(' ');
        // 逐个编译修饰表达式
        travelDecorationClauses { clause: IDbQueryBuilderDecorationClauses<*> ->
            clause.compile(sql);
            sql.append(' ');
        }
        return this;
    }

    /**
     * 清空条件
     * @return
     */
    public override fun clear(): IDbQueryBuilder {
        // 逐个清空修饰表达式
        travelDecorationClauses { clause: IDbQueryBuilderDecorationClauses<*> ->
            clause.clear();
        }

        join.clear();

        return super.clear();
    }

    /**
     * 改写转义值的方法，搜集sql参数
     *
     * @param value
     * @return
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
     * @param str
     * @return
     */
    public fun isOperator(str: String): Boolean {
        return "(\\s|<|>|!|=|is|is not)".toRegex(RegexOption.IGNORE_CASE).matches(str);
    }

    /**
     * 多个where条件
     * @param conditions
     * @return
     */
    public override fun wheres(conditions: Map<String, Any?>): IDbQueryBuilder {
        for ((column, value) in conditions)
            where(column, "=", value);

        return this;
    }

    /**
     * 多个on条件
     * @param conditions
     * @return
     */
    public override fun ons(conditions: Map<String, String>): IDbQueryBuilder {
        for ((column, value) in conditions)
            on(column, "=", value);

        return this;
    }

    /**
     * 多个having条件
     * @param conditions
     * @return
     */
    public override fun havings(conditions: Map<String, Any?>): IDbQueryBuilder {
        for ((column, value) in conditions)
            having(column, "=", value);

        return this;
    }

    /**
     * Alias of andWhere()
     *
     * @param   column  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun where(column: String, op: String, value: Any?): IDbQueryBuilder {
        return andWhere (column, op, value);
    }

    /**
     * Creates a new "AND WHERE" condition for the query.
     *
     * @param   column  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun andWhere(column: String, op: String, value: Any?): IDbQueryBuilder {
        getClause(ClauseType.WHERE).addSubexp(arrayOf<Any?>(column, prepareOperator(value, op), value), "AND");
        return this;
    }

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   column  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun orWhere(column: String, op: String, value: Any?): IDbQueryBuilder {
        getClause(ClauseType.WHERE).addSubexp(arrayOf<Any?>(column, prepareOperator(value, op), value), "OR");
        return this;

    }

    /**
     * Prepare operator
     *
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    protected fun prepareOperator(value: Any?, op: String = "="): String {
        if (value == null && op == "=") // IS null
            return "IS";

        return op;
    }


    /**
     * Alias of andWhereOpen()
     *
     * @return
     */
    public override fun whereOpen(): IDbQueryBuilder {
        return andWhereOpen();
    }

    /**
     * Opens a new "AND WHERE (...)" grouping.
     *
     * @return
     */
    public override fun andWhereOpen(): IDbQueryBuilder {
        getClause(ClauseType.WHERE).open("AND");
        return this;
    }

    /**
     * Opens a new "OR WHERE (...)" grouping.
     *
     * @return
     */
    public override fun orwhereOpen(): IDbQueryBuilder {
        getClause(ClauseType.WHERE).open("OR");
        return this;
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    public override fun whereclose(): IDbQueryBuilder {
        return andWhereclose();
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    public override fun andWhereclose(): IDbQueryBuilder {
        getClause(ClauseType.WHERE).close();
        return this;
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    public override fun orWhereclose(): IDbQueryBuilder {
        getClause(ClauseType.WHERE).close();
        return this;
    }

    /**
     * Creates a "GROUP BY ..." filter.
     *
     * @param   columns  column name or array(column, alias) or object
     * @return
     */
    public override fun groupBy(column: String): IDbQueryBuilder {
        getClause(ClauseType.GROUP_BY).addSubexp(arrayOf(column));
        return this;
    }

    /**
     * Alias of andHaving()
     *
     * @param   column  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun having(column: String, op: String, value: Any?): IDbQueryBuilder {
        return andHaving(column, op, value);
    }

    /**
     * Creates a new "AND HAVING" condition for the query.
     *
     * @param   column  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun andHaving(column: String, op: String, value: Any?): IDbQueryBuilder {
        getClause(ClauseType.HAVING).addSubexp(arrayOf<Any?>(column, prepareOperator(value, op), value), "AND");
        return this;
    }

    /**
     * Creates a new "OR HAVING" condition for the query.
     *
     * @param   column  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun orHaving(column: String, op: String, value: Any?): IDbQueryBuilder {
        getClause(ClauseType.HAVING).addSubexp(arrayOf<Any?>(column, prepareOperator(value, op), value), "OR");
        return this;
    }

    /**
     * Alias of andHavingOpen()
     *
     * @return
     */
    public override fun havingOpen(): IDbQueryBuilder {
        return andHavingOpen();
    }

    /**
     * Opens a new "AND HAVING (...)" grouping.
     *
     * @return
     */
    public override fun andHavingOpen(): IDbQueryBuilder {
        getClause(ClauseType.WHERE).open("AND");
        return this;
    }

    /**
     * Opens a new "OR HAVING (...)" grouping.
     *
     * @return
     */
    public override fun orHavingOpen(): IDbQueryBuilder {
        getClause(ClauseType.WHERE).open("OR");
        return this;
    }

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return
     */
    public override fun havingClose(): IDbQueryBuilder {
        return andHavingClose();
    }

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return
     */
    public override fun andHavingClose(): IDbQueryBuilder {
        getClause(ClauseType.WHERE).close();
        return this;
    }

    /**
     * Closes an open "OR HAVING (...)" grouping.
     *
     * @return
     */
    public override fun orHavingClose(): IDbQueryBuilder {
        getClause(ClauseType.WHERE).close();
        return this;
    }

    /**
     * Applies sorting with "ORDER BY ..."
     *
     * @param   column     column name or array(column, alias) or object
     * @param   direction  direction of sorting
     * @return
     */
    public override fun orderBy(column: String, direction: String?): IDbQueryBuilder {
        getClause(ClauseType.ORDER_BY).addSubexp(arrayOf<Any?>(column, direction));
        return this;
    }

    /**
     * Return up to "LIMIT ..." results
     *
     * @param   limit
     * @param   offset
     * @return
     */
    public override fun limit(limit: Int, offset: Int): IDbQueryBuilder {
        if (offset === 0)
            getClause(ClauseType.LIMIT).addSubexp(arrayOf<Any?>(limit));
        else
            getClause(ClauseType.LIMIT).addSubexp(arrayOf<Any?>(offset)).addSubexp(arrayOf<Any?>(limit));

        return this;
    }

    /**
     * Adds addition tables to "JOIN ...".
     *
     * @param   table  column name or array(column, alias) or object
     * @param   type   join type (LEFT, RIGHT, INNER, etc)
     * @return
     */
    public override fun join(table: Any, type: String?): IDbQueryBuilder {
        // join　子句
        val j = DbQueryBuilderDecorationClausesGroup("$type JOIN", arrayOf<((Any?) -> String)?>(tableQuoter));
        j.addSubexp(arrayOf<Any?>(table));

        // on　子句
        val on = DbQueryBuilderDecorationClausesGroup("ON", arrayOf<((Any?) -> String)?>(columnQuoter, null, columnQuoter));

        join.add(j);
        join.add(on);

        return this;
    }

    /**
     * Adds "ON ..." conditions for the last created JOIN statement.
     *
     * @param   c1  column name or array(column, alias) or object
     * @param   op  logic operator
     * @param   c2  column name or array(column, alias) or object
     * @return
     */
    public override fun on(c1: String, op: String, c2: String): IDbQueryBuilder {
        join.last().addSubexp(arrayOf(c1, op, c2), "AND");
        return this;
    }
}