package com.jkmvc.db

import java.sql.ResultSet

/**
 * 封装db操作
 */
interface IDb{

    /**
     * 获得表的所有列
     */
    fun listColumns(table:String): List<String>;

    /**
     * 执行事务
     */
    fun <T> transaction(statement: Db.() -> T):T;

    /**
     * 执行更新
     */
    fun execute(sql: String, paras: List<Any?>? = null): Int;

    /**
     * 查询多行
     */
    fun <T> queryResult(sql: String, paras: List<Any?>? = null, action:(ResultSet) -> T): T ;

    /**
     * 查询多行
     */
    fun <T> queryRows(sql: String, paras: List<Any?>? = null, transform:(MutableMap<String, Any?>) -> T): List<T>;

    /**
     * 查询一行(多列)
     */
    fun <T> queryRow(sql: String, paras: List<Any?>? = null, transform:(MutableMap<String, Any?>) -> T): T? ;

    /**
     * 查询一行一列
     */
    fun queryCell(sql: String, paras: List<Any?>? = null): Pair<Boolean, Any?>;

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
     * @return string
     */
    fun quoteTables(tables:Collection<Any>, with_brackets:Boolean = false):String;

    /**
     * 转义多个字段名
     *
     * @param Collection<Any> columns 表名集合，其元素可以是String, 也可以是Pair<字段名, 别名>
     * @param bool with_brackets 当拼接数组时, 是否用()包裹
     * @return string
     */
    fun quoteColumns(columns:Collection<Any>, with_brackets:Boolean = false):String;

    /**
     * 转义表名
     *
     * @param string table
     * @return string
     */
    fun quoteTable(table:String, alias:String? = null):String;

    /**
     * 转义多个字段名
     *
     * @param Collection<Any> columns 表名集合，其元素可以是String, 也可以是Pair<字段名, 别名>
     * @param bool with_brackets 当拼接数组时, 是否用()包裹
     * @return string
     */

    /**
     * 转义字段名
     *
     * @param string|array column 字段名, 可以是字段数组
     * @param string alias 字段别名
     * @param bool with_brackets 当拼接数组时, 是否用()包裹
     * @return string
     */
    fun quoteColumn(column:String, alias:String? = null, with_brackets:Boolean = false):String;

    /**
     * 转义值
     *
     * @param string|array value 字段值, 可以是值数组
     * @return string
     */
    fun quote(values:Collection<Any?>):String;

    /**
     * 转义值
     *
     * @param string|array value 字段值, 可以是值数组
     * @return Any
     */
    fun quote(value:Any?):Any?;
}