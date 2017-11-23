package com.jkmvc.db

import com.jkmvc.common.deleteSuffix
import com.jkmvc.common.forceClone
import com.jkmvc.common.getOrPut
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KFunction1

/**
 * 要插入的数据
 */
class InsertData: Cloneable{
    /**
     * 要插入的字段
     */
    public var columns:List<String>? = null;

    /**
     * 要插入的多行数据，但是只有一维，需要按columns的大小，来拆分成多行
     */
    public var rows: ArrayList<Any?> = ArrayList<Any?>();

    /**
     * 检查行的大小
     * @param rowSize
     * @return
     */
    protected fun checkRowSize(rowSize:Int){
        if(isSubQuery())
            throw DbException("已插入子查询，不能再插入值");

        if(columns == null || columns!!.isEmpty())
            throw DbException("请先调用insertColumn()来设置插入的字段名");

        // 字段值数，是字段名数的整数倍
        val columnSize = columns!!.size
        if(rowSize % columnSize != 0)
            throw IllegalArgumentException("请插入的字段值数[$rowSize]与字段名数[$columnSize]不匹配");
    }

    /**
     * 是否插入子查询
     * @return
     */
    public fun isSubQuery(): Boolean {
        return rows.first() is DbQueryBuilder
    }

    /**
     * 添加一行/多行
     * @param row
     * @return
     */
    public fun add(row: Array<out Any?>): InsertData {
        checkRowSize(row.size)
        rows.addAll(row)
        return this;
    }

    /**
     * 添加子查询
     * @param subquery
     * @return
     */
    public fun add(subquery: DbQueryBuilder): InsertData {
        if(rows.isNotEmpty())
            throw DbException("已插入其他值，不能再插入子查询");

        rows.add(subquery)
        return this;
    }

    /**
     * 添加一行/多行
     */
    public fun add(row: Collection<Any?>): InsertData {
        checkRowSize(row.size)
        rows.addAll(row)
        return this;
    }

    /**
     * 清空数据
     */
    public fun clear() {
        columns = null;
        rows.clear();
    }

    /**
     * 克隆对象
     * @return o
     */
    public override fun clone(): Any {
        val o = super.clone() as InsertData
        // columns是List类型，没实现Cloneable接口
        //o.columns = columns?.clone() as List<String>?
        if(columns != null)
            o.columns = ArrayList(columns)
        o.rows = rows.clone() as ArrayList<Any?>
        return o;
    }
}

/**
 * 空表
 */
public val emptyTable = Pair<String, String?>("", null)

/**
 * sql构建器 -- 动作子句: 由动态select/insert/update/delete来构建的子句
 *   通过字符串模板来实现
 *
 * @author shijianhang
 * @date 2016-10-12
 */
abstract class DbQueryBuilderAction(override val db: IDb/* 数据库连接 */, var table: Pair<String, String?> /*表名*/) : IDbQueryBuilder() {

    companion object {

        /**
         * 动作子句的sql模板
         *   sql模板的动作顺序 = ActionType中定义的动作顺序
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
        protected val fieldFillers: Map<String, KFunction1<DbQueryBuilderAction, String>> = mapOf(
                "table" to DbQueryBuilderAction::fillTable,
                "columns" to DbQueryBuilderAction::fillColumns,
                "values" to DbQueryBuilderAction::fillValues
        )
    }

    /**
     * 动作
     */
    protected var action: ActionType? = null;

    /**
     * 要操作的数据：增改查的数据（没有删）
     *   操作数据的动作顺序 = ActionType中定义的动作顺序
     */
    protected var manipulatedData:Array<Any?> = arrayOfNulls(3)

    /**
     * 要查询的字段名: [alias to column]
     */
    public val selectColumns: HashSet<Any>
        get(){
            return manipulatedData.getOrPut(ActionType.SELECT.ordinal){
                HashSet<Any>();
            } as HashSet<Any>
        }

    /**
     * 要插入的多行: columns + values
     */
    protected val insertRows: InsertData
        get(){
            return manipulatedData.getOrPut(ActionType.INSERT.ordinal){
                InsertData()
            } as InsertData
        }

    /**
     * 要更新字段值: <column to value>
     */
    protected val updateRow: HashMap<String, Any?>
        get(){
            return manipulatedData.getOrPut(ActionType.UPDATE.ordinal){
                HashMap<String, Any?>();
            } as HashMap<String, Any?>
        }

    /**
     * select语句中, 控制查询结果是否去重唯一
     */
    protected var distinct: Boolean = false;

    /**
     * 设置表名: 可能有多个表名
     * @param table 表名
     * @param alias 别名
     * @return
     */
    public override fun from(table: String, alias:String?): IDbQueryBuilder {
        this.table = Pair(table, alias)
        return this
    }

    /**
     * 设置插入的列, insert时用
     *
     * @param column
     * @return
     */
    public override fun insertColumns(vararg colums:String):IDbQueryBuilder{
        insertRows.columns = colums.asList()
        return this;
    }

    /**
     * 设置插入的单行, insert时用
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
    public override fun value(subquery: DbQueryBuilder): IDbQueryBuilder {
        insertRows.add(subquery);
        return this;
    }

    /**
     * 设置插入的单行, insert时用
     *
     * @param row
     * @return
     */
    public override fun value(row:Map<String, Any?>):IDbQueryBuilder{
        insertRows.columns = ArrayList(row.keys)
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
     * @param columns 字段名数组，其元素类型是 String 或 Pair<String, String>
     *                如 arrayOf(column1, column2, alias to column3),
     * 				  如 arrayOf("name", "age", "birt" to "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
     * @return
     */
    public override fun select(vararg columns: Any): IDbQueryBuilder {
        if (!columns.isEmpty()){
            for(column in columns){
                when(column){
                    is Array<*> -> selectColumns.addAll(column as Array<Any>)
                    is Collection<*> -> selectColumns.addAll(column as Collection<Any>);
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
        when (action) {
            ActionType.SELECT -> selectColumns.clear();
            ActionType.INSERT -> insertRows.clear();
            ActionType.UPDATE -> updateRow.clear();
        }
        action = null;
        table = emptyTable;
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
     *
     * @param buffer 记录编译后的sql
     * @return
     */
    public override fun compileAction(buffer: StringBuilder): IDbQueryBuilder {
        if (action == null)
            throw DbException("未设置sql动作");

        // 实际上是填充子句的参数，如将行参表名替换为真实表名
        var sql: String = SqlTemplates[action!!.ordinal];

        // 1 填充表名/多个字段名/多个字段值
        // 针对 select :columns from :table / insert into :table :columns values :values / update :table
        sql = ":(table|columns|values)".toRegex().replace(sql) { result: MatchResult ->
            // 调用对应的方法: fillTable() / fillColumns() / fillValues()
            val method = fieldFillers[result.groupValues[1]];
            method?.call(this).toString();
        };

        // 2 填充字段谓句
        // 针对 update :table set :column = :value
        if(action == ActionType.UPDATE)
            sql = ":column(.+):value".toRegex().replace(sql) { result: MatchResult ->
                fillColumnPredicate(result.groupValues[1]);
            };
        // 3 填充distinct
        else if(action == ActionType.SELECT)
            sql = sql.replace(":distinct", if (distinct) "distinct" else "");

        buffer.append(sql);
        return this;
    }

    /**
     * 编译表名: 转义
     * @return
     */
    public fun fillTable(): String {
        if(action == ActionType.INSERT || action == ActionType.DELETE) // mysql的insert/delete语句, 不支持表带别名
            return db.quoteTable(table.component1())

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
        if (action == ActionType.SELECT) {
            if (selectColumns.isEmpty())
                return "*";

            return db.quoteColumns(selectColumns);
        }

        // 2 insert子句:  data是要插入的多行: columns + values
        return db.quoteColumns(insertRows.columns!!);
    }

    /**
     * 编译多个字段值: 转义
     *     insert时用
     *
     *  @return
     */
    public fun fillValues(): String {
        // 1 insert...select..字句
        val firstValue = insertRows.rows.first()
        if(firstValue is DbQueryBuilder) // 子查询
            return quote(firstValue)

        // 2 insert子句:  data是要插入的多行: columns + values
        val sql = StringBuilder("VALUES ");
        //对每行构建()
        var i = 0
        val valueSize = insertRows.rows.size
        while(i < valueSize){ //insertRows.rows是多行数据，但是只有一维，需要按columns的大小，来拆分成多行
            sql.append("(")
            //对每值执行db.quote(value);
            val columnSize = insertRows.columns!!.size
            for (j in 0..(columnSize - 1)){
                val v = insertRows.rows[i++]
                sql.append(quote(v)).append(", ")
            }
            sql.deleteSuffix(", ").append("), ")
        }
        return sql.deleteSuffix(", ").toString();
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