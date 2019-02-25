package com.jkmvc.db

import com.jkmvc.common.Config
import com.jkmvc.common.camel2Underline
import com.jkmvc.common.format
import com.jkmvc.common.underline2Camel
import java.sql.Connection
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * db元数据
 *
 * 1 属性初始化
 *    由于 Db 类依赖 DbMeta 对象来代理实现 IDbMeta 接口, 因此 Db 对象创建时就需要 DbMeta 对象
 *    同时 DbMeta.anyConn 属性及相关属性 又依赖 Db 对象来获得连接, 因此 DbMeta 属性时需要 Db 对象
 *    如果 DbMeta.anyConn 属性在 DbMeta 对象创建时就初始化/调用, 则无疑会导致死循环依赖
 *    =>  DbMeta.anyConn 属性及相关属性, 递延初始化/调用
 *
 * 2 internal声明
 *    我不希望外部人使用该类, 减少可见性
 *    建议直接使用Db, Db也是代理该类
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2018-12-15 9:03 PM
 */
internal class DbMeta(public override val name:String /* 标识 */) : IDbMeta {

    companion object {

        /**
         * 缓存db元数据
         */
        protected val metas: ConcurrentHashMap<String, DbMeta> = ConcurrentHashMap();

        /**
         * 获得db元数据
         *    跨线程跨请求, 全局共有的数据源
         * @param name 数据源名
         * @return
         */
        public fun get(name: String): DbMeta {
            return metas.getOrPut(name){
                DbMeta(name)
            }
        }

    }

    /************************** 元数据 ***************************/
    /**
     * 主库配置
     */
    protected val masterConfig: Config = Config.instance("database.$name.master", "yaml")

    /**
     * 任意连接
     *   当需要查db时, 就随便要个连接
     */
    protected val anyConn: Connection
        get() = Db.instance(name).conn

    /**
     * 获得数据库类型
     *   根据driverClass来获得
     */
    public override val dbType:DbType by lazy{
        //通过driverName是否包含关键字判断
        var driver: String = anyConn.metaData.driverName
        // fix bug: sqlserver的driverName居然有空格, 如 Microsoft JDBC Driver 6.5 for SQL Server
        driver = driver.replace(" ", "")
        var result: DbType? = null
        for(type in DbType.values()){
            if (driver.contains(type.toString(), true)) {
                result = type
                break
            }
        }
        if(result == null)
            throw RuntimeException("Unknow database type")
        else
            result
    }

    /**
     * sql标示符（表/字段）的转义字符
     *   mysql为 `table`.`column`
     *   oracle为 "table"."column"
     *   sql server为 "table"."column" 或 [table].[column]
     */
    public override val identifierQuoteString:String by lazy(LazyThreadSafetyMode.NONE) {
        anyConn.metaData.identifierQuoteString
    }

    /**
     * 表的字段
     */
    public override val tableColumns: Map<String, List<String>> by lazy {
        val tables = HashMap<String, MutableList<String>>()
        // 查询所有表的所有列
        /**
         * fix bug:
         * mysql中查询，conn.catalog = 数据库名
         * oracle中查询，conn.catalog = null，必须指定 schema 来过滤表，否则查出来多个库的表，会出现同名表，查出来的表字段有误
         */
        val rs = anyConn.metaData.getColumns(anyConn.catalog, schema, null, null)
        while (rs.next()) { // 逐个处理每一列
            val table = rs.getString("TABLE_NAME")!! // 表名
            val column = rs.getString("COLUMN_NAME")!! // 列名
            // 添加表的列
            tables.getOrPut(table){
                LinkedList()
            }.add(column);
        }
        tables
    }

    /**
     * schema
     *    oracle的概念，代表一组数据库对象
     *    在 Db.tableColumns 中延迟加载表字段时，用来过滤 DYPT 库的表
     *    可省略，默认值=username
     */
    public override val schema:String? by lazy {
        if(dbType == DbType.Oracle) // oracle: 直接读db配置
            masterConfig.getString("schema", masterConfig["username"])
        else if(dbType == DbType.Mysql){ // mysql: 解析url
            val m = "jdbc:mysql://[^/]+/([^\\?]+)".toRegex().find(masterConfig["url"]!!)
            if(m != null)
                m.groupValues[1]
            else
                null
        }else
        null
    }

    /************************** 属性名与字段名互转 ***************************/
    /**
     * 属性名到字段名的映射 -- 缓存字段名
     */
    protected val prop2ColumnMapping: MutableMap<String, String> = HashMap()

    /**
     * 字段名到属性名的映射 -- 缓存属性名
     */
    protected val column2PropMapping: MutableMap<String, String> = HashMap()

    /**
     * 根据对象属性名，获得db字段名
     *    可根据实际需要在 model 类中重写
     *
     * @param prop 对象属性名
     * @return db字段名
     */
    public override fun prop2Column(prop:String): String {
        return prop2ColumnMapping.getOrPut(prop){
            // 处理关键字
            if(dbType == DbType.Oracle && prop == "rownum"){
                return prop
            }

            // 表+属性
            val tableAndProp = if(prop.contains('.')) prop.split('.') else null

            // 转属性
            var column = if(tableAndProp == null) prop else tableAndProp[1]
            if(masterConfig["columnUnderline"]!!) // 字段有下划线
                column = column.camel2Underline()
            if(masterConfig["columnUpperCase"]!!)// 字段全大写
                column = column.toUpperCase() // 转大写

            if(tableAndProp == null) column else tableAndProp[0] + '.' + column
        }
    }

    /**
     * 根据db字段名，获得对象属性名
     *    可根据实际需要在 model 类中重写
     *
     * @param column db字段名
     * @return 对象属性名
     */
    public override fun column2Prop(column:String): String {
        return column2PropMapping.getOrPut(column){
            var prop = column
            if(masterConfig["columnUpperCase"]!!)// 字段全大写
                prop = prop.toLowerCase() // 转小写
            if(masterConfig["columnUnderline"]!!) // 字段有下划线
                prop = prop.underline2Camel()
            prop
        }
    }

    /************************** 转义标识符 ***************************/
    /**
     * 缓存转义的标识符
     */
    protected val quotedIds: MutableMap<String, String> = HashMap()

    /**
     * 是否关键字
     * @param col 列
     * @return
     */
    public override fun isKeyword(col: String): Boolean{
        return dbType == DbType.Oracle && col == "rownum"
    }

    /**
     * 转义标识符(表名/字段名)
     * @param 表名或字段名或别名 DbExpr
     * @return
     */
    public override fun quoteIdentifier(id: String): String {
        return quotedIds.getOrPut(id) {
            "$identifierQuoteString$id$identifierQuoteString"
        }
    }

    /************************** 转义值 ***************************/
    /**
     * 转义单个值
     *
     * @param value 字段值
     * @return
     */
    public override fun quoteSingleValue(value: Any?): String {
        // null => "NULL"
        if (value == null)
            return "NULL";

        // bool => int
        if (value is Boolean)
            return if (value) "1" else "0";

        // int/float
        if (value is Number)
            return value.toString();

        // enum
        if(value is Enum<*>)
            return value.ordinal.toString()

        // string
        if (value is String)
            return "'$value'" // oracle字符串必须是''包含

        // date
        if (value is Date)
            return quoteDate(value)

        return value.toString()
    }

    /**
     * 转移日期值
     * @value value 参数
     * @return
     */
    protected fun quoteDate(value: Date): String {
        val value = "'${value.format()}'"
        return if(dbType == DbType.Oracle)
            "to_date($value,'yyyy-mm-dd hh24:mi:ss')"
        else
            value
    }

}