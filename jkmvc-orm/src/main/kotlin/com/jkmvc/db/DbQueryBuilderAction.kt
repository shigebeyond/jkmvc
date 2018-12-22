package com.jkmvc.db

import com.jkmvc.common.*
import java.util.*
import kotlin.reflect.KFunction2

/**
 * sql构建器 -- 动作子句: 由动态select/insert/update/delete来构建的子句
 *   通过字符串模板来实现
 *
 * @author shijianhang
 * @date 2016-10-12
 */
abstract class DbQueryBuilderAction : DbQueryBuilderQuoter() {

    companion object {

        /**
         * 动作子句的sql模板
         *   sql模板的动作顺序 = SqlType中定义的动作顺序
         */
        protected val SqlTemplates:Array<String> = arrayOf(
                "SELECT :distinct :columns FROM :table",
                "INSERT INTO :table (:columns) :values", // quoteColumn() 默认不加(), quote() 默认加()
                "UPDATE :table SET :column = :value",
                "DELETE FROM :table"
        );

        /**
         * 缓存字段填充方法
         */
        protected val fieldFillers: Map<String, KFunction2<DbQueryBuilderAction, IDb, String>> = mapOf(
                "table" to DbQueryBuilderAction::fillTable,
                "columns" to DbQueryBuilderAction::fillColumns,
                "values" to DbQueryBuilderAction::fillValues
        )
    }

    /**
     * 动作
     */
    protected var action: SqlType? = null;

    /**
     * 表名/子查询
     */
    protected var table: DbExpr = DbExpr.empty

    /**
     * 要操作的数据：增改查的数据（没有删）
     *   操作数据的动作顺序 = SqlType中定义的动作顺序
     */
    protected var manipulatedData:Array<Any?> = arrayOfNulls(3)

    /**
     * 要查询的字段名
     */
    public val selectColumns: HashSet<CharSequence>
        get(){
            return manipulatedData.getOrPut(SqlType.SELECT.ordinal){
                HashSet<CharSequence>();
            } as HashSet<CharSequence>
        }

    /**
     * 要插入的多行: columns + values
     */
    protected val insertRows: InsertData
        get(){
            return manipulatedData.getOrPut(SqlType.INSERT.ordinal){
                InsertData()
            } as InsertData
        }

    /**
     * 要更新字段值: <column to value>
     */
    protected val updateRow: MutableRow
        get(){
            return manipulatedData.getOrPut(SqlType.UPDATE.ordinal){
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
    public override fun insertColumns(vararg colums:String):IDbQueryBuilder{
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
    public override fun value(row:Row):IDbQueryBuilder{
        insertRows.columns = row.keys.mapToArray {
            it
        }
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
    public override fun set(column:String, value:String, isExpr: Boolean):IDbQueryBuilder{
        val realValue = if (isExpr) DbExpr(value, false) else value
        return this.set(column, realValue)
    }

    /**
     * 设置更新的多个值, update时用
     *
     * @param row 单行数据
     * @return
     */
    public override fun sets(row: Row): IDbQueryBuilder {
        updateRow.putAll(row);
        return this;
    }

    /**
     * 设置查询的字段, select时用
     *
     * @param columns 字段名数组，其元素类型是 String 或 DbExpr
     *                如 arrayOf(column1, column2, DbExpr(column3, alias)),
     * 				  如 arrayOf("name", "age", DbExpr("birthday", "birt"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
     * @return
     */
    public override fun select(vararg columns: CharSequence): IDbQueryBuilder {
        selectColumns.addAll(columns)
        return this;
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
     * 清空条件
     * @return
     */
    public override fun clear(): IDbQueryBuilder {
        when (action) {
            SqlType.SELECT -> selectColumns.clear();
            SqlType.INSERT -> insertRows.clear();
            SqlType.UPDATE -> updateRow.clear();
        }
        action = null;
        table = DbExpr.empty;
        distinct = false;
        return this;
    }

    /**
     * 克隆对象
     * @return o
     */
    public override fun clone(): Any {
        val o = super.clone() as DbQueryBuilderAction
        // 复制要操作的数据
        o.manipulatedData = arrayOfNulls(manipulatedData.size)
        for (i in 0..(manipulatedData.size - 1))
            o.manipulatedData[i] = manipulatedData[i]?.forceClone()
        return o;
    }

    /**
     * 编译动作子句
     * @param db
     * @param buffer 记录编译后的sql
     * @return
     */
    public override fun compileAction(db: IDb, buffer: StringBuilder): IDbQueryBuilder {
        if (action == null)
            throw DbException("未设置sql动作");

        // 实际上是填充子句的参数，如将行参表名替换为真实表名
        var sql: String = SqlTemplates[action!!.ordinal];

        // 1 填充表名/多个字段名/多个字段值
        // 针对 select :columns from :table / insert into :table :columns values :values / update :table
        sql = ":(table|columns|values)".toRegex().replace(sql) { result: MatchResult ->
            // 调用对应的方法: fillTable() / fillColumns() / fillValues()
            val method = fieldFillers[result.groupValues[1]];
            method?.call(this, db).toString();
        };

        // 2 填充字段谓句
        // 针对 update :table set :column = :value
        if(action == SqlType.UPDATE)
            sql = ":column(.+):value".toRegex().replace(sql) { result: MatchResult ->
                fillColumnValueExpr(db, result.groupValues[1]);
            };
        // 3 填充distinct
        else if(action == SqlType.SELECT)
            sql = sql.replace(":distinct", if (distinct) "distinct" else "");

        buffer.append(sql);
        return this;
    }

    /**
     * 编译表名: 转义
     * @param db
     * @return
     */
    public fun fillTable(db: IDb): String {
        if(action == SqlType.INSERT || action == SqlType.DELETE) // mysql的insert/delete语句, 不支持表带别名
            return quoteTable(db, table.exp)

        return quoteTable(db, table);
    }

    /**
     * 编译多个字段名: 转义
     *     select/insert时用
     * @param db
     * @return
     */
    public fun fillColumns(db: IDb): String {
        var cols: Iterator<CharSequence>

        if (action == SqlType.SELECT) { // 1 select子句:  data是要查询的字段名
            if (selectColumns.isEmpty())
                return "*";

            cols = selectColumns.iterator()
        } else // 2 insert子句:  data是要插入的多行: columns + values
            cols = insertRows.columns.iterator()

        return cols.joinToString(", ") {
            // 单个字段转义
            quoteColumn(db, it)
        }
    }

    /**
     * 编译多个字段值: 转义
     *     insert时用
     * @param db
     * @return
     */
    public fun fillValues(db: IDb): String {
        // 1 insert...select..字句
        if(insertRows.isSubQuery()) // 子查询
            return quote(db, insertRows.getSubQuery())

        // 2 insert子句:  data是要插入的多行: columns + values
        val sql = StringBuilder("VALUES ");
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
        return sql.deleteSuffix(", ").toString();
    }

    /**
     * 编译字段名与字段值的表达式: 转义 + 构建表达式 + 连接表达式
     *    update时用
     * @param db
     * @param operator 一对字段名与字段值之间的操作符, 组成一个表达式
     * @param delimiter 表达式之间的连接符
     * @return
     */
    public fun fillColumnValueExpr(db: IDb, operator: String, delimiter: String = ", "): String {
        // update子句:  data是要更新字段值: <column to value>
        if (updateRow.isEmpty())
            return "";

        var sql: StringBuilder = StringBuilder();
        for ((column, value) in updateRow) {
            // column = value,
            sql.append(quoteColumn(db, column)).append(" ").append(operator).append(" ").append(quote(db, value)).append(delimiter);
        }

        return sql.deleteSuffix(delimiter).toString();
    }
}