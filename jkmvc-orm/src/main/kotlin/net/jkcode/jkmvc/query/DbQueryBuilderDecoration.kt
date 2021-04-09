package net.jkcode.jkmvc.query

import net.jkcode.jkmvc.db.DbException
import net.jkcode.jkmvc.db.IDb
import net.jkcode.jkutil.common.*

/**
 * sql构建器 -- 修饰子句: 由修饰词where/group by/order by/limit来构建的子句
 *
 * @author shijianhang
 * @date 2016-10-12
 */
abstract class DbQueryBuilderDecoration : DbQueryBuilderAction (){

    companion object{

        /**
         * 空字段, 仅用于适配where第一个参数, 生成sql时不输出
         */
        val emptyColumn = DbExpr("", false)
    }

    /**
     * where/group by/having/order by/limit子句的数组
     *   where/group by/having/ordery by/limit 按顺序编译sql，否则sql无效
     *   因此不能用map, 而用array中有序，下标=DecorationPartType枚举的序号
     */
    protected val parts: Array<DbQueryPart<*>?> = arrayOfNulls(5);

    /**
     * where子句
     *   条件数组, 每个条件 = 字段名 + 运算符 + 字段值
     */
    protected val whereClause: DbQueryPart<*>
        get(){
            return parts.getOrPut(DecorationPartType.WHERE.ordinal){
                DbQueryPartGroup("WHERE", arrayOf(DbQueryBuilderDecoration::quoteWhereColumn, null, DbQueryBuilderDecoration::quote));
            } as DbQueryPart<*>
        }

    /**
     * group by子句
     *   字段数组
     */
    protected val groupByClause: DbQueryPart<*>
        get(){
            return parts.getOrPut(DecorationPartType.GROUP_BY.ordinal){
                DbQueryPartSimple("GROUP BY", arrayOf(DbQueryBuilderDecoration::quoteColumn));
            } as DbQueryPart<*>
        }

    /**
     * having子句
     *   条件数组, 每个条件 = 字段名 + 运算符 + 字段值
     */
    protected val havingClause: DbQueryPart<*>
        get(){
            return parts.getOrPut(DecorationPartType.HAVING.ordinal){
                DbQueryPartGroup("HAVING", arrayOf(DbQueryBuilderDecoration::quoteColumn, null, DbQueryBuilderDecoration::quote));
            } as DbQueryPart<*>
        }

    /**
     * order by子句
     *   排序数组, 每个排序 = 字段+方向
     */
    protected val orderByClause: DbQueryPart<*>
        get(){
            return parts.getOrPut(DecorationPartType.ORDER_BY.ordinal){
                DbQueryPartSimple("ORDER BY", arrayOf(DbQueryBuilderDecoration::quoteColumn, DbQueryBuilderDecoration::quoteOrderDirection));
            } as DbQueryPart<*>
        }

    /**
     * limit参数: limit + offset
     *    为了兼容不同db的特殊的limit语法，不使用 DbQueryPartSimple("LIMIT", arrayOf(null));
     *    直接硬编码
     */
    protected var limitParams: DbLimit? = null

    /**
     * select语句中, 控制查询加锁
     */
    protected var forUpdate: Boolean = false;

    /**
     * 设置查询加锁
     *
     * @param value
     * @return
     */
    public override fun forUpdate(value: Boolean): IDbQueryBuilder {
        forUpdate = value;
        return this;
    }

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
     * 编译修饰子句
     * @param db 数据库连接
     * @param sql 保存编译的sql
     * @return
     */
    public override fun compileDecoration(db: IDb, sql: StringBuilder): IDbQueryBuilder {
        sql.append(' ');
        // 逐个编译修饰表达式
        for(part in parts){
            if(part != null){
                part.compile(this, db, sql);
                sql.append(' ');
            }
        }

        // 单独编译limit表达式
        compileLimit(db, sql)

        // 单独编译for update
        if(action == SqlAction.SELECT && forUpdate)
            sql.append(" FOR UPDATE")

        return this;
    }

    /**
     * 清空条件
     * @return
     */
    public override fun clear(): IDbQueryBuilder {
        // 逐个清空修饰表达式
        for(part in parts){
            part?.clear()
        }
        limitParams = null
        forUpdate = false;
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
        o.cloneProperties(true,"parts")
        return o
    }

    /**
     * Alias of andWhere()
     *
     * @param   column  column name or DbExpr, also support column+operator: "age >=" / "name like"
     * @param   value   column value
     * @return
     */
    override fun where(column: String, value: Any?): IDbQueryBuilder {
        val (col, op) = splitOperator(column, value)
        return where(col, op, value);
    }

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   column  column name or DbExpr, also support column+operator: "age >=" / "name like"
     * @param   value   column value
     * @return
     */
    override fun orWhere(column: String, value: Any?): IDbQueryBuilder {
        val (col, op) = splitOperator(column, value)
        return orWhere(col, op, value);
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
        return whav(column, op, value, true, true)
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
        return whav(column, op, value, true, false)
    }

    /**
     * 兼容 andWhere()/orWhere()/andHaving()/orHaving()
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @param   where   whether where/having
     * @param   and     whether and/or
     * @return
     */
    protected inline fun whav(column: String, op: String, value: Any?, where: Boolean, and: Boolean): IDbQueryBuilder {
        if(trySplitWhere(column, op, value, where, and))
            return this;

        val clause = if(where) whereClause else havingClause
        clause.addSubexp(arrayOf(column, prepareOperator(column, op, value), value), and);
        return this;
    }

    /**
     * 拆分in参数
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @param   where   whether where/having
     * @param   and     whether and/or
     * @return
     */
    protected inline fun trySplitWhere(column: String, op: String, value: Any?, where: Boolean, and: Boolean): Boolean {
        return trySplitColumn(column, op, value, true, where, and) // 尝试根据 & (与)来分割(字段)子条件
                || trySplitColumn(column, op, value, false, where, and) // 尝试根据 | (或)来分割(字段)子条件
                || trySplitInParams(column, op, value, where, and) // 尝试拆分in参数
    }
    /**
     * 尝试根据分隔符 |(或) 与 &(与) 来分割(字段)子条件
     *   多个字段使用相同查询条件
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @param   and     是否与(分隔符&), 否则或(分隔符|), 用于分割字段
     * @param   where   whether where/having
     * @param   outAnd  外部的and/or
     * @return
     */
    protected fun trySplitColumn(column: String, op: String, value: Any?, and: Boolean, where: Boolean, outAnd: Boolean): Boolean{
        val delimiter = if(and) '&' else '|'; // 分割符
        // 无分隔符
        if(!column.contains(delimiter))
            return false

        // 分割字段
        val cols = column.split(delimiter);
        whavOpen(where, outAnd)
        for (col in cols){
            // 拼接每个字段的条件
            whav(col, op, value, where, and)
        }
        whavClose(where)
        return true;
    }
    /**
     * 尝试拆分in参数
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @param   where   whether where/having
     * @param   and     whether and/or
     * @return
     */
    protected fun trySplitInParams(column: String, op: String, value: Any?, where: Boolean, and: Boolean): Boolean {
        // 只处理in参数
        if(!op.equals("IN", true))
            return false;

        // 参数个数超过1000才拆分
        val maxInParamNum = 1000
        if (value is List<*> && value.size > maxInParamNum) {
            whavOpen(where, and)
            var i = 0;
            while (i < value.size) {
                whav(column, op, value.subList(i, minOf(i + maxInParamNum - 1, value.size - 1)), where, and)
                i = i + maxInParamNum
            }
            whavClose(where)
            return true
        }
        return false
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

        whereClause.addSubexp(arrayOf<Any?>(DbCondition(condition, params)), true);
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

        whereClause.addSubexp(arrayOf<Any?>(DbCondition(condition, params)), false);
        return this;
    }

    /**
     * Creates a new "OR WHERE EXISTS" condition for the query.
     *
     * @param   subquery 子查询
     * @return
     */
    public override fun whereExists(subquery: IDbQueryBuilder): IDbQueryBuilder {
        // 不能直接调用 orWhere(), 因为字段emptyColumn不是String
        whereClause.addSubexp(arrayOf<Any?>(emptyColumn, "EXISTS", subquery), false);
        return this
    }

    /**
     * Creates a new "WHERE EXISTS" condition for the query.
     *
     * @param   subquery 子查询
     * @return
     */
    public override fun orWhereExists(subquery: IDbQueryBuilder): IDbQueryBuilder {
        // 不能直接调用 where(), 因为字段emptyColumn不是String
        whereClause.addSubexp(arrayOf<Any?>(emptyColumn, "EXISTS", subquery), true);
        return this
    }

    /**
     * Opens a new "AND WHERE (...)" grouping.
     *
     * @return
     */
    public override fun andWhereOpen(): IDbQueryBuilder {
        whavOpen(true,true);
        return this;
    }

    /**
     * Opens a new "OR WHERE (...)" grouping.
     *
     * @return
     */
    public override fun orWhereOpen(): IDbQueryBuilder {
        whavOpen(true,false);
        return this;
    }

    /**
     * 兼容 andWhereOpen()/orWhereOpen()/andHavingOpen()/orHavingOpen()
     * @param   where   whether where/having
     * @param   and     whether and/or
     * @return
     */
    protected inline fun whavOpen(where: Boolean, and: Boolean): IDbQueryBuilder {
        val clause = if(where) whereClause else havingClause
        clause.open(and);
        return this;
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    public override fun andWhereClose(): IDbQueryBuilder {
        return whavClose(true)
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    public override fun orWhereClose(): IDbQueryBuilder {
        return whavClose(true)
    }

    /**
     * 兼容 andWhereClose()/orWhereClose()/andHavingClose()/orHavingClose()
     * @param   where   whether where/having
     * @return
     */
    protected inline fun whavClose(where: Boolean): IDbQueryBuilder {
        val clause = if(where) whereClause else havingClause
        clause.close();
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
     * @param   column  column name or DbExpr, also support column+operator: "age >=" / "name like"
     * @param   value   column value
     * @return
     */
    override fun having(column: String, value: Any?): IDbQueryBuilder{
        val (col, op) = splitOperator(column, value)
        return having(col, op, value);
    }

    /**
     * Alias of andHaving()
     *
     * @param   column  column name or DbExpr, also support column+operator: "age >=" / "name like"
     * @param   value   column value
     * @return
     */
    override fun orHaving(column: String, value: Any?): IDbQueryBuilder{
        val (col, op) = splitOperator(column, value)
        return orHaving(col, op, value);
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
        return whav(column, op, value, false, true)
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
        return whav(column, op, value, false, false)
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

        havingClause.addSubexp(arrayOf<Any?>(DbCondition(condition, params)), true);
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

        havingClause.addSubexp(arrayOf<Any?>(DbCondition(condition, params)), false);
        return this;
    }

    /**
     * Opens a new "AND HAVING (...)" grouping.
     *
     * @return
     */
    public override fun andHavingOpen(): IDbQueryBuilder {
        return whavOpen(false,true);
    }

    /**
     * Opens a new "OR HAVING (...)" grouping.
     *
     * @return
     */
    public override fun orHavingOpen(): IDbQueryBuilder {
        return whavOpen(false,false);
    }

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return
     */
    public override fun andHavingClose(): IDbQueryBuilder {
        return whavClose(false)
    }

    /**
     * Closes an open "OR HAVING (...)" grouping.
     *
     * @return
     */
    public override fun orHavingClose(): IDbQueryBuilder {
        return whavClose(false)
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

}