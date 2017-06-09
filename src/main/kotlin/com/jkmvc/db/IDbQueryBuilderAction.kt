package com.jkmvc.db

/**
 * sql构建器 -- 动作子句: 由动态select/insert/update/delete来构建的子句
 *   通过字符串模板来实现
 *
 * @author shijianhang
 * @date 2016-10-12
 */
interface IDbQueryBuilderAction
{
    /**
     * 数据库连接
     */
    val db:IDb;

    /**
     * 编译动作子句
     *
     * @return
     */
    fun compileAction(sql: StringBuilder): IDbQueryBuilder;

    /**
     * 设置表名: 一般是单个表名
     * @param tables 表名数组: array(table1, table2, alias to table3),
     * 								  如 array("user", "contact", "addr" to "useraddress"), 其中 user 与 contact 表不带别名, 而 useraddress 表带别名 addr
     * @return
     */
    fun table(tables:String):IDbQueryBuilder;

    /**
     * 设置表名: 可能有多个表名
     * @param tables 表名数组: array(table1, table2, alias to table3),
     * 								  如 array("user", "contact", "addr" to "useraddress"), 其中 user 与 contact 表不带别名, 而 useraddress 表带别名 addr
     * @return
     */
    fun from(tables:String):IDbQueryBuilder;

    /**
     * 设置插入的列, insert时用
     *
     * @param column
     * @return
     */
    fun insertColumns(vararg colums:String):IDbQueryBuilder;

    /**
     * 设置插入的单行, insert时用
     *
     * @param row
     * @return
     */
    fun value(vararg row:Any?):IDbQueryBuilder;

    /**
     * 设置插入的单行, insert时用
     *
     * @param row
     * @return
     */
    fun value(row:Map<String, Any?>):IDbQueryBuilder;

    /**
     * 设置更新的单个值, update时用
     *
     * @param column
     * @param value
     * @return
     */
    fun set(column:String, value:Any?):IDbQueryBuilder;

    /**
     * 设置更新的多个值, update时用
     *
     * @param row
     * @return
     */
    fun sets(row:Map<String, Any?>):IDbQueryBuilder;

    /**
     * 设置查询的字段, select时用
     *
     * @param columns 字段名数组: Array(column1, column2, alias to column3),
     * 													如 Array("name", "age", "birt" to "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
     * @return
     */
    fun select(vararg columns:Any):IDbQueryBuilder;

    /**
     * 设置查询结果是否去重唯一
     *
     * @param value
     * @returnAction
     */
    fun distinct(value:Boolean):IDbQueryBuilder;

    /**
     * 清空条件
     * @return
     */
    fun clear():IDbQueryBuilder;
}