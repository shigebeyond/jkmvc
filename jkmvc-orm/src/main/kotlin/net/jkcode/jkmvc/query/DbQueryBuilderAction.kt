package net.jkcode.jkmvc.query

import net.jkcode.jkutil.common.*
import net.jkcode.jkmvc.db.DbException
import net.jkcode.jkmvc.db.DbType
import net.jkcode.jkmvc.db.IDb
import net.jkcode.jkmvc.orm.DbKeyNames
import java.util.*
import kotlin.collections.ArrayList

/**
 * sql构建器 -- 动作子句: 由动态select/insert/update/delete来构建的子句
 *   通过字符串模板来实现
 *
 * @author shijianhang
 * @date 2016-10-12
 */
abstract class DbQueryBuilderAction : DbQueryBuilderQuoter() {

    companion object{
        /**
         * 操作符的正则
         *   空格 操作符 空格 结尾
         */
        val opRegx = " *([<>!=]+| IS( NOT)?|( NOT)? (EXISTS|BETWEEN|LIKE|IN)) *$".toRegex(RegexOption.IGNORE_CASE)

        /**
         * 分割操作符
         * @param   column  column name or DbExpr, also support column+operator: "age >=" / "name like"
         * @param   value   column value
         * @return
         */
        @JvmStatic
        protected fun splitOperator(column: String, value: Any?): Pair<String, String> {
            var op = "="
            var col = column

            // 1 有操作符, 如 "age >=" 或 "name like"
            val r = opRegx.find(column)
            if(r != null && col.notContainsQuotationMarks()){
                op = r.value
                val iOp = col.length - op.length
                val iFunc = col.lastIndexOf(')') // 函数)的位置
                // 无函数 或 函数在空格前
                if(iFunc == -1 || iFunc < iOp) {
                    col = col.substring(0, iOp)
                    return col to op
                }
            }

            // 2 无操作符
            if (value == null)
                op = "IS"

            if (value.isArrayOrCollection())
                op = "IN"

            return col to op
        }
    }

    /**
     * 动作
     */
    protected var action: SqlAction? = null;

    /**
     * 表名/子查询
     */
    protected var table: DbExpr = DbExpr.empty

    /**
     * join子句
     *   联表数组，每个联表join = 表名 + 联表方式 | 每个联表条件on = 字段 + 运算符 + 字段
     */
    protected val joinParts: LinkedList<DbQueryPart<*>> = LinkedList()

    /**
     * join的表名/子查询
     */
    protected val joinTables: LinkedList<CharSequence> = LinkedList()

    /**
     * 表别名, 如果没有别名, 则表名
     */
    override val tableAlias: String
        get() = table.alias ?: table.toString()

    /**
     * 要操作的数据：增改查的数据（没有删）
     *   操作数据的动作顺序 = SqlType中定义的动作顺序
     */
    protected val manipulatedData:Array<Any?> = arrayOfNulls(3)

    /**
     * 要查询的字段名
     */
    protected val selectColumns: HashSet<CharSequence>
        get(){
            return manipulatedData.getOrPut(SqlAction.SELECT.ordinal){
                HashSet<CharSequence>();
            } as HashSet<CharSequence>
        }

    /**
     * 要插入的多行: columns + values
     */
    protected val insertRows: InsertData
        get(){
            return manipulatedData.getOrPut(SqlAction.INSERT.ordinal){
                InsertData()
            } as InsertData
        }

    /**
     * 要更新字段值: <column to value>
     */
    protected val updateRow: MutableMap<String, Any?>
        get(){
            return manipulatedData.getOrPut(SqlAction.UPDATE.ordinal){
                HashMap<String, Any?>();
            } as HashMap<String, Any?>
        }

    /**
     * select语句中, 控制查询结果是否去重唯一
     */
    protected var distinct: Boolean = false;

    /**
     * 设置表名
     * @param table 表名
     * @return
     */
    public override fun from(table: DbExpr): IDbQueryBuilder {
        this.table = table
        return this
    }

    /**
     * 设置插入的列, insert时用
     *
     * @param column
     * @return
     */
    public override fun insertColumns(vararg colums:String): IDbQueryBuilder {
        insertRows.columns = colums as Array<String>
        return this;
    }

    /**
     * 设置插入的单行值, insert时用
     *   插入的值的数目必须登录插入的列的数目
     *
     * @param row 单行数据
     * @return
     */
    public override fun value(vararg row:Any?): IDbQueryBuilder {
        insertRows.add(row);
        return this;
    }

    /**
     * 设置插入的子查询, insert时用
     *
     * @param row 单行数据
     * @return
     */
    public override fun values(subquery: IDbQueryBuilder): IDbQueryBuilder {
        insertRows.add(subquery);
        return this;
    }

    /**
     * 设置插入的单行, insert时用
     *
     * @param row
     * @return
     */
    public override fun value(row: Map<String, Any?>): IDbQueryBuilder {
        insertRows.columns = row.keys.toTypedArray()
        insertRows.add(row.values)
        return this;
    }

    /**
     * 设置更新的单个值, update时用
     *
     * @param column 字段名
     * @param value 字段值
     * @return
     */
    public override fun set(column: String, value: Any?): IDbQueryBuilder {
        updateRow.put(column, value);
        return this;
    }

    /**
     * 设置更新的单个值, update时用
     *
     * @param column
     * @param value
     * @param isExpr 是否db表达式
     * @return
     */
    public override fun set(column:String, value:String, isExpr: Boolean): IDbQueryBuilder {
        val realValue = if (isExpr) DbExpr(value, false) else value
        return this.set(column, realValue)
    }

    /**
     * 设置更新的多个值, update时用
     *
     * @param row 单行数据
     * @return
     */
    public override fun sets(row: Map<String, Any?>): IDbQueryBuilder {
        updateRow.putAll(row);
        return this;
    }

    /**
     * 设置查询的字段, select时用
     *
     * @param columns 字段名数组，其元素类型是 String 或 DbExpr
     *                select("id", "name") // 查询多个字段, 每个字段一个参数
     *                select("id,name") // 查询多个字段, 多个字段用逗号拼接为一个参数
     *                select(DbExpr("birthday", "birt")) // 字段带别名, 用DbExpr包装
     *                select("create_time created") // 字段带别名, 用空格分开
     * @return
     */
    public override fun select(vararg columns: CharSequence): IDbQueryBuilder {
        selectColumns.addAll(columns)
        return this;
    }

    /**
     * 设置查询的字段, select时用
     *
     * @param columns 字段名数组，其元素类型是 String 或 DbExpr
     *                selects(listOf("id", "name")) // 查询多个字段, 每个字段一个参数
     *                selects(listOf("id,name")) // 查询多个字段, 多个字段用逗号拼接为一个参数
     *                selects(listOf(DbExpr("birthday", "birt"))) // 字段带别名, 用DbExpr包装
     *                selects(listOf("create_time created")) // 字段带别名, 用空格分开
     * @return
     */
    public override fun selects(columns:List<CharSequence>): IDbQueryBuilder{
        selectColumns.addAll(columns)
        return this;
    }

    /**
     * 设置查询的字段, select时用
     * @param key 字段名
     * @return
     */
    override fun select(key: DbKeyNames): IDbQueryBuilder{
        return select(*key.columns)
    }

    /**
     * 设置查询结果是否去重唯一
     *
     * @param value
     * @return
     */
    public override fun distinct(value: Boolean): IDbQueryBuilder {
        distinct = value;
        return this;
    }

    /**
     * Adds addition tables to "JOIN ...".
     *
     * @param   table  table name | DbExpr | subquery
     * @param   type   joinParts type (LEFT, RIGHT, INNER, etc)
     * @return
     */
    public override fun join(table: CharSequence, type: String): IDbQueryBuilder {
        joinTables.add(table)

        // join　子句
        val j = DbQueryPartSimple("$type JOIN", arrayOf(DbQueryBuilderDecoration::quoteTable));
        j.addSubexp(arrayOf(table));

        // on　子句 -- on总是追随最近的一个join
        val on = DbQueryPartGroup("ON", arrayOf(DbQueryBuilderDecoration::quoteColumn, null, DbQueryBuilderDecoration::quoteColumnOrValue));

        joinParts.add(j);
        joinParts.add(on);

        return this;
    }

    /**
     * Adds "ON ..." conditions for the last created JOIN statement.
     *    on总是追随最近的一个join
     *
     * @param   c1  column name or DbExpr
     * @param   op  logic operator
     * @param   c2  column name or DbExpr or value
     * @param   isCol whether is column name, or value
     * @return
     */
    public override fun on(c1: String, op: String, c2: Any?, isCol: Boolean): IDbQueryBuilder {
        joinParts.last().addSubexp(arrayOf<Any?>(c1, op, Pair(c2, isCol)), "AND");
        return this;
    }


    /**
     * Adds "ON ..." conditions for the last created JOIN statement.
     *    on总是追随最近的一个join
     *
     * @param   c1  column name or DbExpr
     * @param   c2  column name or DbExpr or value
     * @param   isCol whether is column name, or value
     * @return
     */
    public override fun on(c1: String, c2: Any?, isCol: Boolean): IDbQueryBuilder{
        // 字段
        if(isCol)
            return on(c1, "=", c2, isCol)

        // 值
        val (col, op) = splitOperator(c1, c2)
        return on(col, op, c2, isCol)
    }

    /**
     * 清空条件
     * @return
     */
    public override fun clear(): IDbQueryBuilder {
        when (action) {
            SqlAction.SELECT -> selectColumns.clear();
            SqlAction.INSERT -> insertRows.clear();
            SqlAction.UPDATE -> updateRow.clear();
        }
        action = null;
        table = DbExpr.empty;
        distinct = false;
        joinParts.clear();
        joinTables.clear()
        for (part in joinParts)
            part.clear()
        return this;
    }

    /**
     * 克隆对象
     * @return o
     */
    public override fun clone(): Any {
        val o = super.clone() as DbQueryBuilderAction
        // action参数不复制
        o.action = null
        // 复制复杂属性: 要操作的数据
        o.cloneProperties(true, "table", "joinParts", "joinTables", "manipulatedData")
        return o;
    }

    /**
     * 克隆对象, 同clone(), 只转换下返回类型为 IDbQueryBuilder
     *
     * @param clearSelect 清空select参数
     * @return o
     */
    public override fun copy(clearSelect: Boolean): IDbQueryBuilder{
        val ret = clone() as DbQueryBuilderAction
        if(clearSelect)
            ret.selectColumns.clear()
        return ret
    }

    /**
     * 编译动作子句
     * @param db
     * @param sql 记录编译后的sql
     * @return
     */
    public override fun compileAction(db: IDb, sql: StringBuilder): DbQueryBuilderAction {
        if (action == null)
            throw DbException("Not set sql action");

        // 编译sql模板: 替换参数
        action!!.template.compile(this as DbQueryBuilderDecoration, db, sql)
        return this;
    }

    /**
     * 编译表名: 转义
     * @param db
     * @param sql
     */
    internal fun fillTables(db: IDb, sql: StringBuilder){
        // 填充本表
        val tb = if(action == SqlAction.INSERT)
                    quoteTable(db, table.exp)
                else
                    quoteTable(db, table)
        sql.append(tb).append(' ')

        // 填充联查的表
        for (part in joinParts)
            part.compile(this as DbQueryBuilderDecoration, db, sql)
    }

    /**
     * 编译多个字段名: 转义
     *     select/insert时用
     * @param db
     * @param sql
     */
    internal fun fillColumns(db: IDb, sql: StringBuilder){
        var cols: Iterator<CharSequence>

        if (action == SqlAction.SELECT) { // 1 select子句:  data是要查询的字段名
            if (selectColumns.isEmpty()){
                sql.append("*")
                return
            }

            cols = selectColumns.iterator()
        } else // 2 insert子句:  data是要插入的多行: columns + values
            cols = insertRows.columns.iterator()

        cols.joinTo(sql, ", ") {
            // 单个字段转义
            quoteColumn(db, it)
        }
    }

    /**
     * 转义select字段名, 存在以下2种情况
     *
     * @param db
     * @param table
     * @return
     */
    internal fun quoteSelectColumn(db: IDb, column: CharSequence): String{
        if(column is String && column.notContainsQuotationMarks()){
            // 1 逗号分割多个字段
            if(column.contains(',')){
                val cols = column.splitOutsideFunc(',')
                return cols.joinToString{
                    quoteSelectColumn(db, it) // 递归调用
                }
            }
        }

        // 2 单个字段
        return db.quoteColumn(column)
    }

    /**
     * 编译多个字段值: 转义
     *     insert时用
     * @param db
     * @param sql
     */
    internal fun fillValues(db: IDb, sql: StringBuilder){
        // 1 insert...select..字句
        if(insertRows.isSubQuery()) { // 子查询
            sql.append(quote(db, insertRows.getSubQuery()))
            return
        }

        // 2 insert子句:  data是要插入的多行: columns + values
        sql.append("VALUES ");
        //对每行构建()
        var i = 0
        val valueSize = insertRows.rows.size
        while(i < valueSize){ //insertRows.rows是多行数据，但是只有一维，需要按columns的大小，来拆分成多行
            sql.append("(")
            //对每值执行db.quote(value);
            val columnSize = insertRows.columns.size
            for (j in 0..(columnSize - 1)){
                val v = insertRows.rows[i++]
                sql.append(quote(db, v)).append(", ")
            }
            sql.deleteSuffix(", ").append("), ")
        }
        sql.deleteSuffix(", ")
    }

    /**
     * 编译distinct
     *     select时用
     * @param db
     * @param sql
     */
    internal fun fillDistinct(db: IDb, sql: StringBuilder){
        val part = if (distinct) "distinct" else ""
        sql.append(part)
    }

    /**
     * 编译多个字段名等于字段值的表达式: 转义 + 连接
     *    update时用, 即 field1 = value1, field2 = value2
     * @param db
     * @param sql
     */
    internal fun fillColumnValues(db: IDb, sql: StringBuilder){
        // update子句:  data是要更新字段值: <column to value>
        if (updateRow.isEmpty())
            return;

        for ((column, value) in updateRow) {
            // column = value,
            sql.append(quoteColumn(db, column)).append(" = ").append(quote(db, value)).append(",");
        }
        sql.deleteSuffix(",").toString();
    }

    /**
     * 填充删除的多表
     * @param db 数据库连接
     * @param sql 保存编译的sql
     */
    public fun fillDelTables(db: IDb, sql: StringBuilder){
        // 仅处理多表删除, 但mysql删除单表也是要处理的
        if(action != SqlAction.DELETE || joinTables.isEmpty() && db.dbType != DbType.Mysql)
            return

        val tables = ArrayList<CharSequence>(joinTables.size + 1)
//        if(joinTables.isEmpty() && db.dbType == DbType.Mysql && table is DbExpr && table.alias == null){
//            // mysql删除单表, 且无表别名, 就不输出了
//        }else
            tables.add(table)
        for(table in joinTables) {
            // 子查询不能删除
            if(table is IDbQueryBuilder
                    || table is DbExpr && table.exp is IDbQueryBuilder)
                continue

            tables.add(table)
        }

        tables.joinTo(sql, ", ", " ", " ") { table ->
            db.quoteTableAlias(table)
        }

        tables.clear()
    }
}