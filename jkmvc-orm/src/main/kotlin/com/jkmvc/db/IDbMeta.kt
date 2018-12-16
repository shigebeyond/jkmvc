package com.jkmvc.db

/**
 * db元数据
 *
 */
interface IDbMeta{

    /**
     * 标识名
     */
    val name:String

    /**
     * 获得数据库类型
     *   根据driverClass来获得
     */
    val dbType:DbType

    /**
     * sql标示符（表/字段）的转义字符
     *   mysql为 `table`.`column`
     *   oracle为 "table"."column"
     *   sql server为 "table"."column" 或 [table].[column]
     */
    val identifierQuoteString:String

    /**
     * schema
     *    oracle的概念，代表一组数据库对象
     *    在 Db.tableColumns 中延迟加载表字段时，用来过滤 DYPT 库的表
     *    可省略，默认值=username
     */
    val schema:String?

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
     * 表的字段
     */
    val tableColumns: Map<String, List<String>>

    /**
     * 获得表的所有列
     *
     * @param table
     * @return
     */
    fun listColumns(table:String): List<String> {
        return tableColumns.get(table)!!;
    }
}