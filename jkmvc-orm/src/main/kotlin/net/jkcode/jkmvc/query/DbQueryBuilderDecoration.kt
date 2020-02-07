package net.jkcode.jkmvc.query

import net.jkcode.jkutil.common.cloneProperties
import net.jkcode.jkutil.common.getOrPut
import net.jkcode.jkutil.common.isArrayOrCollectionEmpty
import net.jkcode.jkmvc.db.DbException
import net.jkcode.jkmvc.db.DbType
import net.jkcode.jkmvc.db.IDb
import java.util.*
import kotlin.reflect.KFunction2

/**
 * sql构建器 -- 修饰子句: 由修饰词join/where/group by/order by/limit来构建的子句
 *
 * @author shijianhang
 * @date 2016-10-12
 */
abstract class DbQueryBuilderDecoration : DbQueryBuilderAction(){

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
    protected var limitParams:Pair<Int, Int>? = null

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
        return if (direction != null && "^(ASC|DESC)$".toRegex().matches(direction))
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
        if(limitParams == null)
            return

        val (limit, offset) = limitParams!!
        if(db.dbType == DbType.Oracle) { // oracle
            // select * from ( select t1_.*, rownum rownum_ from ( select * from USER ) t1_ where rownum <  $end ) t2_ where t2_.rownum_ >=  $limit
            sql.insert(0, "SELECT t1_.*, rownum rownum_ FROM ( ").append(") t1_ WHERE rownum <  ").append(limit + offset)
            if(offset > 0)
                sql.insert(0, "SELECT * FROM ( ").append(" ) t2_ WHERE t2_.rownum_ >=  ").append(offset)
            return
        }

        if(db.dbType == DbType.SqlServer) { // sqlserver
            val iSelect = "SELECT".length
            if(offset == 0) {
                //select top $limit * from user
                sql.insert(iSelect, " TOP $limit") // 在 select 之后插入 top
            }else{
                // 截取 order by 子句
                val iOrderBy = sql.indexOf("ORDER BY")
                var orderBy = "ORDER BY ID" // 由于排名函数 "ROW_NUMBER" 必须有 ORDER BY 子句, 因此默认给一个, 但是最好是开发者自己提供
                if(iOrderBy != -1){
                    orderBy = sql.substring(iOrderBy)
                    sql.delete(iOrderBy, sql.length)
                }
                // SELECT * FROM ( SELECT ROW_NUMBER() OVER (ORDER BY name) as rownum_, * FROM "user" ) a WHERE rownum_ >= $limit and rownum_ < $end;
                sql.insert(iSelect, "* FROM ( SELECT ROW_NUMBER() OVER ( $orderBy ) as rownum_, ").append(") t_ WHERE rownum_ >= ").append(limit).append(" AND rownum_ < ").append(offset + limit)
            }

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
        if(offset == 0)
            sql.append(" LIMIT ").append(limit)
        else
            sql.append(" LIMIT ").append(offset).append(", ").append(limit)
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
        val o = super.clone()
        // 复制子句
        o.cloneProperties(true,"clauses", "joinClause")
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
            throw DbException("查询条件where(\"$column\", \"IN\", ?)中的值为空数组/集合");

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
        limitParams = Pair(limit, offset)
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