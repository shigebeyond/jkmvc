package net.jkcode.jkmvc.db

/**
 * db元数据
 *
 */
interface IDbMeta: IDbIdentifierQuoter, IDbValueQuoter {

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
     * catalog
     *   一般是db名, 就是 show databases; 中某个名字
     */
    val catalog: String?

    /**
     * schema
     *    oracle的概念，代表一组数据库对象
     *    在 Db.tables 中延迟加载表字段时，用来过滤 DYPT 库的表
     *    可省略，默认值=username
     */
    val schema:String?

    /**
     * 表
     */
    val tables: Map<String, DbTable>

    /**
     * 获得表
     *
     * @param table
     * @return
     */
    fun getTable(table:String): DbTable?

    /**
     * 获得表的所有列
     *    有缓存
     *
     * @param table
     * @return
     */
    fun getColumnsByTable(table:String): Collection<DbColumn> {
        return getTable(table)?.columns?.values ?: throw DbException("表[$table]不存在")
    }

    /**
     * 查询表是否存在
     * @param table 表名
     * @return
     */
    fun queryTableExist(table: String): Boolean

    /**
     * 查询表的主键列
     * @param table 表名, 必须不为空, 且存在于db, 否则报错
     * @return
     */
    fun queryPrimaryKeysByTable(table: String): Collection<String>

    /**
     * 查询表的列
     *    每次都查最新的
     *
     * @param table 表名, 如果为null, 则查询所有表的列
     * @return
     */
    fun queryColumnsByTable(table: String?): List<DbColumn>

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

}