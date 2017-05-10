package com.jkmvc.db

import com.jkmvc.common.deleteSuffix
import java.util.*
import kotlin.reflect.KFunction1

/**
 * sql构建器 -- 动作子句: 由动态select/insert/update/delete来构建的子句
 *   通过字符串模板来实现
 *
 * @author shijianhang
 * @date 2016-10-12
 */
abstract class DbQueryBuilderAction(override val db: IDb/* 数据库连接 */, var table: String = "" /*表名*/) : IDbQueryBuilder() {

    companion object {
        /**
         * 动作子句的sql模板
         */
        protected val SqlTemplates:Map<String, String> = mapOf(
                "select" to "SELECT :distinct :columns FROM :table",
                "insert" to "INSERT INTO :table (:columns) VALUES :values", // quoteColumn 默认不加(), quotevalue 默认加()
                "update" to "UPDATE :table SET :column = :value",
                "delete" to "DELETE FROM :table"
        );

        /**
         * 缓存字段填充方法
         */
        protected val fieldFillers: Map<String, KFunction1<DbQueryBuilderAction, String>> = mapOf(
                "table" to DbQueryBuilderAction::fillTable,
                "columns" to DbQueryBuilderAction::fillColumns,
                "values" to DbQueryBuilderAction::fillValues
        )
    }

    /**
     * 动作
     */
    protected var action: String = "";

    /**
     * 要插入的多行: [<column to value>]
     */
    protected val insertRows: MutableList<Map<String, Any?>> by lazy(LazyThreadSafetyMode.NONE) {
        LinkedList<Map<String, Any?>>();
    };

    /**
     * 要更新字段值: <column to value>
     */
    protected val updateRow: MutableMap<String, Any?> by lazy(LazyThreadSafetyMode.NONE) {
        LinkedHashMap<String, Any?>();
    }

    /**
     * 要查询的字段名: [alias to column]
     */
    protected val selectColumns: MutableSet<Any> by lazy(LazyThreadSafetyMode.NONE) {
        HashSet<Any>();
    }

    /**
     * select语句中, 控制查询结果是否去重唯一
     */
    protected var distinct: Boolean = false;

    /**
     * sql参数
     */
    protected val params: MutableList<Any?> by lazy {
        LinkedList<Any?>();
    }

    /**
     * 设置动作
     * 　　延时设置动作，此时可获得对应的数据库连接
     *
     * @param action sql动作：select/insert/update/delete
     * @return
     */
    public fun action(action: String): IDbQueryBuilder {
        if (action !in SqlTemplates)
            throw IllegalArgumentException("无效sql动作: $action");

        this.action = action;
        return this;
    }

    /**
     * 设置表名: 一般是单个表名
     * @param tables 表名数组: array(table1, table2, alias to table3),
     * 								  如 array("user", "contact", "addr" to "useraddress"), 其中 user 与 contact 表不带别名, 而 useraddress 表带别名 addr
     * @return
     */
    public override fun table(tables: String): IDbQueryBuilder {
        return this.tables(tables);
    }

    /**
     * 设置表名: 可能有多个表名
     * @param tables 表名数组: array(table1, table2, alias to table3),
     * 								  如 array("user", "contact", "addr" to "useraddress"), 其中 user 与 contact 表不带别名, 而 useraddress 表带别名 addr
     * @return
     */
    public override fun from(tables: String): IDbQueryBuilder {
        return tables(tables);
    }

    /**
     * 处理多个表名的设置
     * @param tables 表名数组: array(table1, table2, alias to table3),
     * 								  如 array("user", "contact", "addr" to "useraddress"), 其中 user 与 contact 表不带别名, 而 useraddress 表带别名 addr
     * @return
     */
    protected fun tables(tables: String): IDbQueryBuilder {
        table = tables;
        return this;
    }

    /**
     * 设置插入的单行, insert时用
     *
     * @param row 单行数据
     * @return
     */
    public override fun value(row: Map<String, Any?>): IDbQueryBuilder {
        insertRows.add(row);
        return this;
    }

    /**
     * 设置插入的多行, insert时用
     *
     * @param rows 多行数据
     * @return
     */
    public override fun values(rows: List<Map<String, Any?>>): IDbQueryBuilder {
        insertRows.addAll(rows);
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
     * @param columns 字段名数组: array(column1, column2, alias to column3),
     * 													如 array("name", "age", "birt" to "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
     * @return
     */
    public override fun select(vararg columns: Any): IDbQueryBuilder {
        if (!columns.isEmpty()){
            for(column in columns){
                when(column){
                    is Array<*> -> selectColumns.addAll(column as Array<Any>)
                    is Collection<*> -> selectColumns.addAll(columns);
                    else -> selectColumns.add(column);
                }
            }
        }

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
        action = "";
        table = "";
        distinct = false;
        params.clear();
        when (action) {
            "select" -> selectColumns.clear();
            "insert" -> insertRows.clear();
            "update" -> updateRow.clear();
        }
        return this;
    }

    /**
     * 编译动作子句
     *
     * @param sb 记录编译后的sql
     * @return
     */
    public override fun compileAction(sb: StringBuilder): IDbQueryBuilder {
        if (action !in SqlTemplates)
            throw DbException("未设置sql动作: $action");

        // 清空sql参数
        params.clear();

        // 实际上是填充子句的参数，如将行参表名替换为真实表名
        var sql: String = SqlTemplates[action]!!;

        // 1 填充表名/多个字段名/多个字段值
        // 针对 select :columns from :table / insert into :table :columns values :values / update :table
        sql = ":(table|columns|values)".toRegex().replace(sql) { result: MatchResult ->
            // 调用对应的方法: fillTable() / fillColumns() / fillValues()
            val method = fieldFillers[result.groupValues[1]];
            method?.call(this).toString();
        };

        // 2 填充字段谓句
        // 针对 update :table set :column = :value
        if("update" == action)
            sql = ":column(.+):value".toRegex().replace(sql) { result: MatchResult ->
                fillColumnPredicate(result.groupValues[1]);
            };
        // 3 填充distinct
        else if("select" == action)
            sql = sql.replace(":distinct", if (distinct) "distinct" else "");

        sb.append(sql);
        return this;
    }

    /**
     * 编译表名: 转义
     * @return
     */
    public fun fillTable(): String {
        return db.quoteTable(table);
    }

    /**
     * 编译多个字段名: 转义
     *     select/insert时用
     *
     * @return
     */
    public fun fillColumns(): String {
        // 1 select子句:  data是要查询的字段名, [alias to column]
        if (action == "select") {
            if (selectColumns.isEmpty())
                return "*";

            return db.quoteColumns(selectColumns);
        }

        // 2 insert子句:  data是要插入的多行: [<column to value>]
        if (insertRows.isEmpty())
            return "";

        // 取得第一行的keys
        val columns = insertRows[0].keys
        return db.quoteColumns(columns);
    }

    /**
     * 编译多个字段值: 转义
     *     insert时用
     *
     *  @return
     */
    public fun fillValues(): String {
        // insert子句:  data是要插入的多行: [<column to value>]
        if (insertRows.isEmpty())
            return "";

        //对每行每值执行db.quote(value);
        val sql = StringBuilder("(");
        for (row in insertRows){
            for((k, v) in row){
                sql.append(quote(v)).append(", ")
            }
        }
        return sql.deleteSuffix(", ").append(")").toString();
    }

    /**
     * 编译字段谓句: 转义 + 拼接谓句
     *    update时用
     *
     * @param operator 谓语
     * @param delimiter 拼接谓句的连接符
     * @return
     */
    public fun fillColumnPredicate(operator: String, delimiter: String = ", "): String {
        // update子句:  data是要更新字段值: <column to value>
        if (updateRow.isEmpty())
            return "";

        var sql: StringBuilder = StringBuilder();
        for ((column, value) in updateRow) {
            // column = value,
            sql.append(db.quoteColumn(column)).append(" ").append(operator).append(" ").append(quote(value)).append(delimiter);
        }

        return sql.deleteSuffix(delimiter).toString();
    }
}