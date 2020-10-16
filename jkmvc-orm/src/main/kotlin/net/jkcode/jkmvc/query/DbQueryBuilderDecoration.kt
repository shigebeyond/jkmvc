package net.jkcode.jkmvc.query

import net.jkcode.jkutil.common.cloneProperties
import net.jkcode.jkutil.common.getOrPut
import net.jkcode.jkutil.common.isArrayOrCollectionEmpty
import net.jkcode.jkmvc.db.DbException
import net.jkcode.jkmvc.db.DbType
import net.jkcode.jkmvc.db.IDb
import net.jkcode.jkutil.common.dbLogger
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KFunction2

/**
 * sql构建器 -- 修饰子句: 由修饰词join/where/group by/order by/limit来构建的子句
 *
 * @author shijianhang
 * @date 2016-10-12
 */
abstract class DbQueryBuilderDecoration : DbQueryBuilderAction (){

    /**
     * where/group by/having/order by/limit子句的数组
     */
    protected val clauses: Array<DbQueryBuilderDecorationClauses<*>?> = arrayOfNulls(5);

    /**
     * join子句
     *   联表数组，每个联表join = 表名 + 联表方式 | 每个联表条件on = 字段 + 运算符 + 字段
     */
    protected val joinClause: LinkedList<DbQueryBuilderDecorationClauses<*>> = LinkedList()

    /**
     * join的表名/子查询
     */
    protected val joinTables: LinkedList<CharSequence> = LinkedList()

    /**
     * where子句
     *   条件数组, 每个条件 = 字段名 + 运算符 + 字段值
     */
    protected val whereClause: DbQueryBuilderDecorationClauses<*>
        get(){
            return clauses.getOrPut(ClauseType.WHERE.ordinal){
                DbQueryBuilderDecorationClausesGroup("WHERE", arrayOf<KFunction2<IDb, *, String>?>(this::quoteWhereColumn, null, this::quote));
            } as DbQueryBuilderDecorationClauses<*>
        }

    /**
     * group by子句
     *   字段数组
     */
    protected val groupByClause: DbQueryBuilderDecorationClauses<*>
        get(){
            return clauses.getOrPut(ClauseType.GROUP_BY.ordinal){
                DbQueryBuilderDecorationClausesSimple("GROUP BY", arrayOf<KFunction2<IDb, *, String>?>(this::quoteColumn));
            } as DbQueryBuilderDecorationClauses<*>
        }

    /**
     * having子句
     *   条件数组, 每个条件 = 字段名 + 运算符 + 字段值
     */
    protected val havingClause: DbQueryBuilderDecorationClauses<*>
        get(){
            return clauses.getOrPut(ClauseType.HAVING.ordinal){
                DbQueryBuilderDecorationClausesGroup("HAVING", arrayOf<KFunction2<IDb, *, String>?>(this::quoteColumn, null, this::quote));
            } as DbQueryBuilderDecorationClauses<*>
        }

    /**
     * order by子句
     *   排序数组, 每个排序 = 字段+方向
     */
    protected val orderByClause: DbQueryBuilderDecorationClauses<*>
        get(){
            return clauses.getOrPut(ClauseType.ORDER_BY.ordinal){
                DbQueryBuilderDecorationClausesSimple("ORDER BY", arrayOf<KFunction2<IDb, *, String>?>(this::quoteColumn, this::quoteOrderDirection));
            } as DbQueryBuilderDecorationClauses<*>
        }

    /**
     * limit参数: limit + offset
     *    为了兼容不同db的特殊的limit语法，不使用 DbQueryBuilderDecorationClausesSimple("LIMIT", arrayOf<KFunction2 <IDb, *, String>?>(null));
     *    直接硬编码
     */
    protected var limitParams: DbLimit? = null

    /**
     * 转义where字段名, 存在以下2种情况
     *   1 普通字段(String|DbExpr): 转义
     *   2 条件表达式(DbCondition): 不转义
     *
     * @param db
     * @param table
     * @return
     */
    internal fun quoteWhereColumn(db: IDb, column: CharSequence): String{
        // 1 条件表达式(DbCondition): 不转义
        if(column is DbCondition) {
            // 记录参数
            if(column.params.isNotEmpty())
                compiledSql.staticParams.addAll(column.params);

            return column.exp
        }

        // 2 普通字段(String|DbExpr): 转义
        return db.quoteColumn(column)
    }

    /**
     * 转义排序方向
     *
     * @param db
     * @param direction
     * @return
     */
    internal fun quoteOrderDirection(db: IDb, direction: String?): String{
        return if (direction != null
                && ("ASC".equals(direction, true) || "DESC".equals(direction, true)))
            direction;
        else
            "";
    }

    /**
     * 编译limit表达式
     * @param db 数据库连接
     * @param sql 保存编译的sql
     */
    public fun compileLimit(db: IDb, sql: StringBuilder){
        limitParams?.compile(db, sql)
    }

    /**
     * 编译多表删除表达式
     * @param db 数据库连接
     * @param sql 保存编译的sql
     */
    public fun compileDeleteMultiTable(db: IDb, sql: StringBuilder){
        // 仅处理多表删除
        if(action != SqlType.DELETE || joinTables.isEmpty())
            return

        val tables = ArrayList<CharSequence>(joinTables.size + 1)
        tables.add(table)
        for(table in joinTables) {
            // 子查询不能删除
            if(table is IDbQueryBuilder
                    || table is DbExpr && table.exp is IDbQueryBuilder)
                continue

            tables.add(table)
        }

        if (db.dbType == DbType.Mysql) { // mysql
            val tablesPart = tables.joinToString(", ", " ", " ") { table ->
                db.quoteTableAlias(table)
            }

            val iSelect = "DELETE".length
            //delete t1, t2 from t1 left join t2 on ...
            sql.insert(iSelect, tablesPart) // 在 delete 之后插入多表
            return
        }

        // todo: oracle/sql server
    }

    /**
     * 编译修饰子句
     * @param db 数据库连接
     * @param sql 保存编译的sql
     * @return
     */
    public override fun compileDecoration(db: IDb, sql: StringBuilder): IDbQueryBuilder {
        sql.append(' ');
        // 逐个编译修饰表达式
        travelDecorationClauses { clause: IDbQueryBuilderDecorationClauses<*> ->
            clause.compile(db, sql);
            sql.append(' ');
        }

        // 单独编译limit表达式
        compileLimit(db, sql)

        // 单独编译多表删除表达式
        compileDeleteMultiTable(db, sql)

        return this;
    }

    /**
     * 遍历修饰子句
     * @param visitor 访问者函数，遍历时调用
     */
    protected fun travelDecorationClauses(visitor: (IDbQueryBuilderDecorationClauses<*>) -> Unit) {
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
        joinTables.clear()
        limitParams = null
        return super.clear();
    }

    /**
     * 克隆对象
     * @return o
     */
    public override fun clone(): Any {
        val o = super.clone() as DbQueryBuilderDecoration
        // limit参数不复制
        o.limitParams = null
        // 复制复杂属性: 子句
        o.cloneProperties(true,"clauses", "joinClause", "joinTables")
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
     * Creates a new "AND WHERE" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun andWhere(column: String, op: String, value: Any?): IDbQueryBuilder {
        if(op == "IN" && trySplitInParams(column, op, value, true))
            return this;

        whereClause.addSubexp(arrayOf(column, prepareOperator(column, op, value), value), "AND");
        return this;
    }

    /**
     * 拆分in参数
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @param   and    and / or
     * @return
     */
    protected fun trySplitInParams(column: String, op: String, value: Any?, and: Boolean): Boolean {
        val maxInParamNum = 1000
        if (value is List<*> && value.size > maxInParamNum) {
            if(and) andWhereOpen() else orWhereOpen()
            var i = 0;
            while (i < value.size) {
                orWhere(column, op, value.subList(i, minOf(i + maxInParamNum - 1, value.size - 1)))
                i = i + maxInParamNum
            }
            if(and) andWhereClose() else orWhereClose()
            return true
        }
        return false
    }

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun orWhere(column: String, op: String, value: Any?): IDbQueryBuilder {
        if(op == "IN" && trySplitInParams(column, op, value, false))
            return this;

        whereClause.addSubexp(arrayOf(column, prepareOperator(column, op, value), value), "OR");
        return this;
    }

    /**
     * Prepare operator
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    protected fun prepareOperator(column: String, op: String, value: Any?): String {
        if (value == null && op == "=") // IS null
            return "IS";

        if(op.equals("IN", true) && (value == null || value.isArrayOrCollectionEmpty())) // IN 空数组/集合
            throw DbException("Invalid call `where(\"$column\", \"IN\", ?)`: 3th parameter is empty array of collection");

        return op;
    }

    /**
     * Creates a new "AND WHERE" condition for the query.
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    public override fun andWhereCondition(condition: String, params: List<*>): IDbQueryBuilder {
        if(condition.isBlank())
            return this

        whereClause.addSubexp(arrayOf(DbCondition(condition, params)), "AND");
        return this;
    }

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    public override fun orWhereCondition(condition: String, params: List<*>): IDbQueryBuilder {
        if(condition.isBlank())
            return this

        whereClause.addSubexp(arrayOf(DbCondition(condition, params)), "OR");
        return this;
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
     * Creates a new "AND HAVING" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun andHaving(column: String, op: String, value: Any?): IDbQueryBuilder {
        if(op == "IN" && trySplitInParams(column, op, value, true))
            return this;

        havingClause.addSubexp(arrayOf(column, prepareOperator(column, op, value), value), "AND");
        return this;
    }

    /**
     * Creates a new "OR HAVING" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun orHaving(column: String, op: String, value: Any?): IDbQueryBuilder {
        if(op == "IN" && trySplitInParams(column, op, value, false))
            return this;

        havingClause.addSubexp(arrayOf(column, prepareOperator(column, op, value), value), "OR");
        return this;
    }

    /**
     * Creates a new "AND HAVING" condition for the query.
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    public override fun andHavingCondition(condition: String, params: List<*>): IDbQueryBuilder{
        if(condition.isBlank())
            return this

        havingClause.addSubexp(arrayOf(DbCondition(condition, params)), "AND");
        return this;
    }

    /**
     * Creates a new "OR HAVING" condition for the query.
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    public override fun orHavingCondition(condition: String, params: List<*>): IDbQueryBuilder{
        if(condition.isBlank())
            return this

        havingClause.addSubexp(arrayOf(DbCondition(condition, params)), "OR");
        return this;
    }

    /**
     * Opens a new "AND HAVING (...)" grouping.
     *
     * @return
     */
    public override fun andHavingOpen(): IDbQueryBuilder {
        havingClause.open("AND");
        return this;
    }

    /**
     * Opens a new "OR HAVING (...)" grouping.
     *
     * @return
     */
    public override fun orHavingOpen(): IDbQueryBuilder {
        havingClause.open("OR");
        return this;
    }

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return
     */
    public override fun andHavingClose(): IDbQueryBuilder {
        havingClause.close();
        return this;
    }

    /**
     * Closes an open "OR HAVING (...)" grouping.
     *
     * @return
     */
    public override fun orHavingClose(): IDbQueryBuilder {
        havingClause.close();
        return this;
    }

    /**
     * Applies sorting with "ORDER BY ..."
     *
     * @param   column     column name or DbExpr
     * @param   direction  direction of sorting
     * @return
     */
    public override fun orderBy(column: String, direction: String?): IDbQueryBuilder {
        orderByClause.addSubexp(arrayOf(column, direction));
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
        if(limitParams != null)
            dbLogger.warn("Duplate setting limit, only use the last setting")
        limitParams = DbLimit(limit, offset)
        return this;
    }

    /**
     * Adds addition tables to "JOIN ...".
     *
     * @param   table  table name | DbExpr | subquery
     * @param   type   joinClause type (LEFT, RIGHT, INNER, etc)
     * @return
     */
    public override fun join(table: CharSequence, type: String): IDbQueryBuilder {
        joinTables.add(table)

        // join　子句
        val j = DbQueryBuilderDecorationClausesSimple("$type JOIN", arrayOf<KFunction2<IDb, *, String>?>(this::quoteTable));
        j.addSubexp(arrayOf(table));

        // on　子句 -- on总是追随最近的一个join
        val on = DbQueryBuilderDecorationClausesGroup("ON", arrayOf<KFunction2<IDb, *, String>?>(this::quoteColumn, null, this::quoteColumn));

        joinClause.add(j);
        joinClause.add(on);

        return this;
    }

    /**
     * Adds "ON ..." conditions for the last created JOIN statement.
     *    on总是追随最近的一个join
     *
     * @param   c1  column name or DbExpr
     * @param   op  logic operator
     * @param   c2  column name or DbExpr
     * @return
     */
    public override fun on(c1: String, op: String, c2: String): IDbQueryBuilder {
        joinClause.last().addSubexp(arrayOf(c1, op, c2), "AND");
        return this;
    }
}