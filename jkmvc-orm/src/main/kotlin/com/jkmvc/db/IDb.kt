package com.jkmvc.db

import java.sql.ResultSet

/**
 * 封装db操作
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
interface IDb{

    /**
     * 获得数据库类型
     *   根据driverClass来获得
     */
    val dbType:DbType

    /**
     * 执行事务
     * @param statement db操作过程
     * @return
     */
    fun <T> transaction(statement: Db.() -> T):T;

    /**
     * 获得表的所有列
     *
     * @param table
     * @return
     */
    fun listColumns(table:String): List<String>;

    /**
     * 执行更新
     * @param sql
     * @param params
     * @param returnGeneratedKey
     * @return
     */
    fun execute(sql: String, params: List<Any?>? = null, returnGeneratedKey:Boolean = false): Int;

    /**
     * 批量更新: 每次更新sql参数不一样
     *
     * @param sql
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @return
     */
    fun batchExecute(sql: String, paramses: List<Any?>, paramSize:Int): IntArray;

    /**
     * 查询多行
     * @param sql
     * @param params
     * @param action 处理结果的函数
     * @return
     */
    fun <T> queryResult(sql: String, params: List<Any?>? = null, action: (ResultSet) -> T): T;

    /**
     * 查询多行
     * @param sql
     * @param params
     * @param transform 处理结果的函数
     * @return
     */
    fun <T> queryRows(sql: String, params: List<Any?>? = null, transform: (MutableMap<String, Any?>) -> T): List<T>;

    /**
     * 查询一行(多列)
     * @param sql
     * @param params
     * @param transform 处理结果的函数
     * @return
     */
    fun <T> queryRow(sql: String, params: List<Any?>? = null, transform: (MutableMap<String, Any?>) -> T): T?;

    /**
     * 查询一列(多行)
     * @param sql
     * @param params
     * @param transform 处理结果的函数
     * @return
     */
    fun queryColumn(sql: String, params: List<Any?>?): List<Any?>

    /**
     * 查询一行一列
     * @param sql
     * @param params
     * @return
     */
    fun queryCell(sql: String, params: List<Any?>? = null): Pair<Boolean, Any?>;
    
    /**
     * 开启事务
     */
    fun begin():Unit;


    /**
     * 提交
     */
    fun commit():Boolean;

    /**
     * 回滚
     */
    fun rollback():Boolean;

    /**
     * 关闭
     */
    fun close():Unit;

    /**
     * 转义多个表名
     *
     * @param Collection<Any> tables 表名集合，其元素可以是String, 也可以是Pair<表名, 别名>
     * @return
     */
    fun quoteTables(tables:Collection<Any>, with_brackets:Boolean = false):String;

    /**
     * 转义多个字段名
     *
     * @param Collection<Any> columns 表名集合，其元素可以是String, 也可以是Pair<字段名, 别名>
     * @param bool with_brackets 当拼接数组时, 是否用()包裹
     * @return
     */
    fun quoteColumns(columns:Collection<Any>, with_brackets:Boolean = false):String;

    /**
     * 转义表名
     *   mysql为`table`
     *   oracle为"table"
     *   sql server为"table" [table]
     *
     * @param table
     * @param alias
     * @return
     */
    fun quoteTable(table:String, alias:String? = null):String;

    /**
     * 转义表名
     *   mysql为`table`
     *   oracle为"table"
     *   sql server为"table" [table]
     *
     * @param table
     * @return
     */
    fun quoteTable(table:Pair<String, String?>):String{
        return quoteTable(table.component1(), table.component2())
    }

    /**
     * 转义表名
     *
     * @param table
     * @return
     */
    fun quoteTable(table:Any):String{
        return if(table is Pair<*, *>)
                    quoteTable(table as Pair<String, String?>);
                else
                    quoteTable(table as String);
    }

    /**
     * 转义字段名
     *   mysql为`column`
     *   oracle为"column"
     *   sql server为"column" [column]
     *
     * @param column 字段名 + 别名
     * @param with_brackets 当拼接数组时, 是否用()包裹
     * @return
     */
    fun quoteColumn(column:Pair<String, String>, with_brackets:Boolean = false):String{
        return quoteColumn(column.component1(), column.component2(), with_brackets)
    }

    /**
     * 转义字段名
     *
     * @param column 字段名, 可以是字段数组
     * @param alias 字段别名
     * @param bool with_brackets 当拼接数组时, 是否用()包裹
     * @return
     */
    fun quoteColumn(column:String, alias:String? = null, with_brackets:Boolean = false):String;

    /**
     * 转义字段名
     *
     * @param column 字段名, 可以是字段数组
     * @return
     */
    fun quoteColumn(column:Any):String{
        return if(column is Pair<*, *>)
                    quoteColumn(column as Pair<String, String>);
                else
                    quoteColumn(column as String);
    }

    /**
     * 转义值
     *
     * @param value 字段值, 可以是值数组
     * @return
     */
    fun quote(value:Any?):String;

    /**
     * 根据对象属性名，获得db字段名
     *    可根据实际需要在 model 类中重写
     *
     * @param prop 对象属性名
     * @return db字段名
     */
    fun prop2Column(prop:String): String

    /**
     * 根据db字段名，获得对象属性名
     *    可根据实际需要在 model 类中重写
     *
     * @param column db字段名
     * @return 对象属性名
     */
    fun column2Prop(column:String): String

    /**
     * 预览sql
     * @param sql
     * @param params sql参数
     * @return
     */
    fun previewSql(sql: String, params: List<Any?>): String
}