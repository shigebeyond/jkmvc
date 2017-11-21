package com.jkmvc.db

import com.jkmvc.common.getOrPut
import java.util.*

/**
 * sql构建器 -- 修饰子句: 由修饰词join/where/group by/order by/limit来构建的子句
 *
 * @author shijianhang
 * @date 2016-10-12
 */
abstract class DbQueryBuilderDecoration(db: IDb, table: Pair<String, String?> /*表名*/) : DbQueryBuilderAction(db, table) {

    /**
     * 转义列
     */
    protected val columnQuoter: (Any?) -> String = { column: Any? ->
        db.quoteColumn(column!!);
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
    protected val tableQuoter: (Any?) -> String = { table: Any? ->
        db.quoteTable(table!!)
    }

    protected val orderDirection: (Any?) -> String = { value: Any? ->
        if (value != null && "^(ASC|DESC)$".toRegex().matches(value as String))
            value;
        else
            "";
    }

    /**
     * where/group by/having/order by/limit子句的数组
     */
    protected var clauses: Array<DbQueryBuilderDecorationClauses<*>?> = arrayOfNulls<DbQueryBuilderDecorationClauses<*>>(5);

    /**
     * join子句
     *   联表数组，每个联表join = 表名 + 联表方式 | 每个联表条件on = 字段 + 运算符 + 字段
     */
    protected var joinClause: LinkedList<DbQueryBuilderDecorationClausesGroup> = LinkedList<DbQueryBuilderDecorationClausesGroup>()

    /**
     * where子句
     *   条件数组, 每个条件 = 字段名 + 运算符 + 字段值
     */
    protected val whereClause: DbQueryBuilderDecorationClauses<*>
        get(){
            return clauses.getOrPut(ClauseType.WHERE.ordinal){
                DbQueryBuilderDecorationClausesGroup("WHERE", arrayOf<((Any?) -> String)?>(columnQuoter, null, valueQuoter));
            } as DbQueryBuilderDecorationClauses<*>
        }

    /**
     * group by子句
     *   字段数组
     */
    protected val groupByClause: DbQueryBuilderDecorationClauses<*>
        get(){
            return clauses.getOrPut(ClauseType.GROUP_BY.ordinal){
                DbQueryBuilderDecorationClausesSimple("GROUP BY", arrayOf<((Any?) -> String)?>(columnQuoter));
            } as DbQueryBuilderDecorationClauses<*>
        }

    /**
     * having子句
     *   条件数组, 每个条件 = 字段名 + 运算符 + 字段值
     */
    protected val havingClause: DbQueryBuilderDecorationClauses<*>
        get(){
            return clauses.getOrPut(ClauseType.HAVING.ordinal){
                DbQueryBuilderDecorationClausesGroup("HAVING", arrayOf<((Any?) -> String)?>(columnQuoter, null, valueQuoter));
            } as DbQueryBuilderDecorationClauses<*>
        }

    /**
     * order by子句
     *   排序数组, 每个排序 = 字段+方向
     */
    protected val orderByClause: DbQueryBuilderDecorationClauses<*>
        get(){
            return clauses.getOrPut(ClauseType.ORDER_BY.ordinal){
                DbQueryBuilderDecorationClausesSimple("ORDER BY", arrayOf<((Any?) -> String)?>(columnQuoter, orderDirection));
            } as DbQueryBuilderDecorationClauses<*>
        }

    /**
     * limit参数: limit + offset
     *    为了兼容不同db的特殊的limit语法，不使用 DbQueryBuilderDecorationClausesSimple("LIMIT", arrayOf<((Any?) -> String)?>(null));
     *    直接硬编码
     */
    protected var limitParams:Pair<Int, Int>? = null

    /**
     * 编译limit表达式
     *
     * @param sql 保存编译的sql
     */
    public fun compileLimit(sql: StringBuilder){
        if(limitParams == null)
            return

        val (limit, offset) = limitParams!!
        if(db.dbType == DbType.Oracle) { // oracle
            // select * from ( select t1_.*, rownum rownum_ from ( select * from USER ) t1_ where rownum <=  $end ) t2_ where t2_.rownum_ >=  $start
            sql.insert(0, "SELECT t1_.*, rownum rownum_ FROM ( ").append(") t1_ WHERE rownum <=  ").append(offset + limit)
            if(offset > 0)
                sql.insert(0, "SELECT * FROM ( ").append(" ) t2_ WHERE t2_.rownum_ >=  ").append(offset)
            return
        }

        if(db.dbType == DbType.Postgresql) { // psql
            // select * from user limit $limit  offset $offset;
            sql.append(" LIMIT ").append(limit)
            if(offset > 0)
                sql.append(" OFFSET ").append(offset)
            return
        }

        // 其他：mysql / sqlite
        // select * from user limit $offset, $limit;
        sql.append(" LIMIT ").append(offset)
        if(offset > 0)
            sql.append(", ").append(limit)
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

        // 单独编译limit表达式
        compileLimit(sql)

        return this;
    }

    /**
     * 遍历修饰子句
     * @param visitor 访问者函数，遍历时调用
     */
    protected fun travelDecorationClauses(visitor: (IDbQueryBuilderDecorationClauses<*>) -> Unit):Unit {
        // 逐个处理修饰词及其表达式
        // 1 joinClause
        for (j in joinClause)
            visitor(j);

        // 2 where/group by/having/ordery by/limit 按顺序编译sql，否则sql无效
        /*for ((k, v) in clauses) // map中无序
            visitor(v);*/
        for(v in clauses) // array中有序，下标就是序号
            if(v != null)
                visitor(v);
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

        joinClause.clear();

        return super.clear();
    }

    /**
     * 克隆对象
     * @return o
     */
    public override fun clone(): Any {
        val o = super.clone() as DbQueryBuilderDecoration
        //join子句
        o.joinClause = joinClause.clone() as LinkedList<DbQueryBuilderDecorationClausesGroup>
        //where/group by/having/order by/limit子句
        o.clauses = arrayOfNulls(clauses.size)
        for (i in 0..(clauses.size - 1))
            o.clauses[i] = clauses[i]?.clone() as DbQueryBuilderDecorationClauses<*>?
        return o
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
        whereClause.addSubexp(arrayOf<Any?>(column, prepareOperator(value, op), value), "AND");
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
        whereClause.addSubexp(arrayOf<Any?>(column, prepareOperator(value, op), value), "OR");
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
        whereClause.open("AND");
        return this;
    }

    /**
     * Opens a new "OR WHERE (...)" grouping.
     *
     * @return
     */
    public override fun orWhereOpen(): IDbQueryBuilder {
        whereClause.open("OR");
        return this;
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    public override fun whereClose(): IDbQueryBuilder {
        return andWhereClose();
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    public override fun andWhereClose(): IDbQueryBuilder {
        whereClause.close();
        return this;
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    public override fun orWhereClose(): IDbQueryBuilder {
        whereClause.close();
        return this;
    }

    /**
     * Creates a "GROUP BY ..." filter.
     *
     * @param   column  column name
     * @return
     */
    public override fun groupBy(column: String): IDbQueryBuilder {
        groupByClause.addSubexp(arrayOf(column));
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
        havingClause.addSubexp(arrayOf<Any?>(column, prepareOperator(value, op), value), "AND");
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
        havingClause.addSubexp(arrayOf<Any?>(column, prepareOperator(value, op), value), "OR");
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
        whereClause.open("AND");
        return this;
    }

    /**
     * Opens a new "OR HAVING (...)" grouping.
     *
     * @return
     */
    public override fun orHavingOpen(): IDbQueryBuilder {
        whereClause.open("OR");
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
        whereClause.close();
        return this;
    }

    /**
     * Closes an open "OR HAVING (...)" grouping.
     *
     * @return
     */
    public override fun orHavingClose(): IDbQueryBuilder {
        whereClause.close();
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
        orderByClause.addSubexp(arrayOf<Any?>(column, direction));
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
        limitParams = Pair(limit, offset)
        return this;
    }

    /**
     * Adds addition tables to "JOIN ...".
     *
     * @param   table  column name or array(column, alias) or object
     * @param   type   joinClause type (LEFT, RIGHT, INNER, etc)
     * @return
     */
    public override fun join(table: Any, type: String): IDbQueryBuilder {
        // joinClause　子句
        val j = DbQueryBuilderDecorationClausesGroup("$type JOIN", arrayOf<((Any?) -> String)?>(tableQuoter));
        j.addSubexp(arrayOf<Any?>(table));

        // on　子句
        val on = DbQueryBuilderDecorationClausesGroup("ON", arrayOf<((Any?) -> String)?>(columnQuoter, null, columnQuoter));

        joinClause.add(j);
        joinClause.add(on);

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
        joinClause.last().addSubexp(arrayOf(c1, op, c2), "AND");
        return this;
    }
}