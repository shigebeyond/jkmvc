package com.jkmvc.db

/**
 * sql构建器 -- 动作子句: 由动态select/insert/update/delete来构建的子句
 *   通过字符串模板来实现
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-12
 *
 */
interface IDbQueryBuilderAction
{
    /**
     * 数据库连接
     */
    val db:IDb;

    /**
     * 编译动作子句
     * @return string
     */
    public fun compileAction():String;

    /**
     * 设置表名: 一般是单个表名
     * @param tables 表名数组: array(table1, table2, alias to table3),
     * 								  如 array("user", "contact", "addr" to "useraddress"), 其中 user 与 contact 表不带别名, 而 useraddress 表带别名 addr
     * @return DbQueryBuilder
     */
    public fun table(tables:String):IDbQueryBuilder;

    /**
     * 设置表名: 可能有多个表名
     * @param tables 表名数组: array(table1, table2, alias to table3),
     * 								  如 array("user", "contact", "addr" to "useraddress"), 其中 user 与 contact 表不带别名, 而 useraddress 表带别名 addr
     * @return DbQueryBuilder
     */
    public fun from(tables:String):IDbQueryBuilder;

    /**
     * 设置插入的单行, insert时用
     *
     * @param array row
     * @return DbQueryBuilder
     */
    public fun value(row:Map<String, Any?>):IDbQueryBuilder;

    /**
     * 设置插入的多行, insert时用
     *
     * @param array rows
     * @return DbQueryBuilder
     */
    public fun values(rows:List<Map<String, Any?>>):IDbQueryBuilder;

    /**
     * 设置更新的单个值, update时用
     *
     * @param string column
     * @param string value
     * @return DbQueryBuilder
     */
    public fun set(column:String, value:Any?):IDbQueryBuilder;

    /**
     * 设置更新的多个值, update时用
     *
     * @param array row
     * @return DbQueryBuilder
     */
    public fun sets(row:Map<String, Any?>):IDbQueryBuilder;

    /**
     * 设置查询的字段, select时用
     *
     * @param array columns 字段名数组: array(column1, column2, alias to column3),
     * 													如 array("name", "age", "birt" to "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
     * @return DbQueryBuilder
     */
    public fun select(vararg columns:Any):IDbQueryBuilder;

    /**
     * 设置查询结果是否去重唯一
     *
     * @param boolean value
     * @return DbQueryBuilderAction
     */
    public fun distinct(value:Boolean):IDbQueryBuilder;

    /**
     * 清空条件
     * @return DbQueryBuilder
     */
    public fun clear():IDbQueryBuilder;
}