package com.jkmvc.db

import com.jkmvc.common.Config
import com.jkmvc.common.camel2Underline
import com.jkmvc.common.format
import com.jkmvc.common.underline2Camel
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.util.*

/**
 * 封装db操作
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
class Db(protected val conn: Connection /* 数据库连接 */, public val name:String = "default" /* 标识 */):IDb{

    companion object {

        /**
         * orm配置
         */
        public val ormConfig: Config = Config.instance("orm")

        /**
         * 是否调试
         */
        public val debug:Boolean = ormConfig.getBoolean("debug", false)!!;

        /**
         * 数据源工厂
         */
        public val dataSourceFactory:IDataSourceFactory by lazy{
            val clazz:String = ormConfig["dataSourceFactory"]!!
            Class.forName(clazz).newInstance() as IDataSourceFactory
        }

        /**
         * 线程安全的db缓存
         */
        protected val dbs:ThreadLocal<MutableMap<String, Db>> = ThreadLocal.withInitial {
            HashMap<String, Db>();
        }

        /**
         * 获得db
         */
        public fun getDb(name:String = "default"):Db{
            return dbs.get().getOrPut(name){
                //获得数据源
                val dataSource  = dataSourceFactory.getDataSource(name);
                // 新建db
                Db(dataSource.connection, name);
            }
        }

        /**
         * 关闭db
         */
        public fun closeDb(db:Db){
            db.conn.close()
            dbs.get().remove(db.name);
        }

        /**
         * 关闭所有db
         */
        public fun closeAllDb(){
            for((name, db) in dbs.get()){
                db.conn.close()
            }
            dbs.get().clear();
        }

        /**
         * 转化为Long
         * @param value
         * @return
         */
        public fun toLong(value:Any?):Long{
            // oracle 是 BigDecimal
            if(value is BigDecimal)
                return value.toLong()

            // mysql
            return value as Long;
        }

        /**
         * 转化为Int
         * @param value
         * @return
         */
        public fun toInt(value:Any?):Int{
            // oracle 是 BigDecimal
            if(value is BigDecimal)
                return value.toInt()

            // mysql
            return value as Int;
        }
    }

    /**
     * 数据库配置
     */
    val dbConfig: Config = Config.instance("database.$name", "yaml")

    /**
     * 获得数据库类型
     *   根据driverClass来获得
     */
    public override val dbType:DbType by lazy{
        //通过driverName是否包含关键字判断
        val driver: String = conn.metaData.driverName
        var result: DbType? = null
        for(type in DbType.values()){
            if (driver.contains(type.toString(), true))
                result = type
        }
        if(result == null)
            throw RuntimeException("未知数据库类型")
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
        conn.metaData.identifierQuoteString
    }

    /**
     * 表的字段
     */
    protected val tableColumns: Map<String, List<String>> by lazy {
        val tables = HashMap<String, MutableList<String>>()
        // 查询所有表的所有列
        /**
         * fix bug:
         * mysql中查询，conn.catalog = 数据库名
         * oracle中查询，conn.catalog = null，必须指定 schema 来过滤表，否则查出来多个库的表，会出现同名表，查出来的表字段有误
         */
        val rs = conn.metaData.getColumns(conn.catalog, schema, null, null)
        while (rs.next()) { // 逐个处理每一列
            val table = rs.getString("TABLE_NAME")!! // 表名
            val column = rs.getString("COLUMN_NAME")!! // 列名
            // 添加表的列
            tables.getOrPut(table){
                LinkedList<String>()
            }.add(column);
        }
        tables
    }

    /**
     * 属性名到字段名的映射
     */
    protected val prop2ColumnMapping: MutableMap<String, String> = HashMap()

    /**
     * 字段名到属性名的映射
     */
    protected val column2PropMapping: MutableMap<String, String> = HashMap()

    /**
     * schema
     *    oracle的概念，代表一组数据库对象
     *    在 Db.tableColumns 中延迟加载表字段时，用来过滤 DYPT 库的表
     *    可省略，默认值=username
     */
    public val schema:String?
        get(){
            if(dbType == DbType.Oracle)
                return dbConfig.getString("schema", dbConfig["username"])

            if(dbType == DbType.Mysql){
                val m = "jdbc:mysql://[^/]+/([^\\?]+)".toRegex().find(dbConfig["url"]!!)
                if(m != null)
                    return m.groupValues[1]
            }
            return null
        }

    /**
     * 当前事务的嵌套层级
     */
    protected var transDepth:Int = 0;

    /**
     * 标记当前事务是否回滚
     */
    protected var rollbacked = false;

    /**
     * 执行事务
     * @param statement db操作过程
     * @return
     */
    public override fun <T> transaction(statement: () -> T):T{
        try{
            begin(); // 开启事务
            val result:T = statement(); // 执行sql
            commit(); // 提交事务
            return result; // 返回结果
        }catch(e:Exception){
            rollback(); // 回顾
            throw e;
        }finally{
            close() // 关闭连接
        }
    }

    /**
     * 是否在事务中
     * @return
     */
    public override fun isInTransaction(): Boolean {
        return transDepth > 0;
    }

    /**
     * 获得表的所有列
     *
     * @param table
     * @return
     */
    public override fun listColumns(table:String): List<String> {
        return tableColumns.get(table)!!;
    }

    /**
     * 执行更新
     * @param sql
     * @param params
     * @param generatedColumn 返回的自动生成的主键名
     * @return
     */
    public override fun execute(sql: String, params: List<Any?>?, generatedColumn:String?): Int {
        try{
            return conn.execute(sql, params, generatedColumn);
        }catch (e:Exception){
            dbLogger.error("出错[${e.message}] sql: " + previewSql(sql, params))
            throw  e
        }
    }

    /**
     * 批量更新：每次更新sql参数不一样
     *
     * @param sql
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @return
     */
    public override fun batchExecute(sql: String, paramses: List<Any?>, paramSize:Int): IntArray {
        try{
            return conn.batchExecute(sql, paramses, paramSize)
        }catch (e:Exception){
            dbLogger.error("出错[${e.message}], sql=$sql, params=$paramses ")
            throw  e
        }
    }

    /**
     * 查询多行
     * @param sql
     * @param params
     * @param action 转换结果的函数
     * @return
     */
    public override fun <T> queryResult(sql: String, params: List<Any?>?, action: (ResultSet) -> T): T {
        try{
            return conn.queryResult(sql, params, action)
        }catch (e:Exception){
            dbLogger.error("出错[${e.message}] sql: " + previewSql(sql, params))
            throw  e
        }
    }

    /**
     * 查询多行
     * @param sql
     * @param params
     * @param transform 转换结果的函数
     * @return
     */
    public override fun <T> queryRows(sql: String, params: List<Any?>?, transform: (MutableMap<String, Any?>) -> T): List<T> {
        try{
            return conn.queryRows(sql, params, transform);
        }catch (e:Exception){
            dbLogger.error("出错[${e.message}] sql: " + previewSql(sql, params))
            throw  e
        }
    }

    /**
     * 查询一行(多列)
     * @param sql
     * @param params
     * @param transform 转换结果的函数
     * @return
     */
    public override fun <T> queryRow(sql: String, params: List<Any?>?, transform: (MutableMap<String, Any?>) -> T): T? {
        try{
            return conn.queryRow(sql, params, transform);
        }catch (e:Exception){
            dbLogger.error("出错[${e.message}] sql: " + previewSql(sql, params))
            throw  e
        }
    }

    /**
     * 查询一列(多行)
     * @param sql
     * @param params
     * @param transform 转换结果的函数
     * @return
     */
    public override fun queryColumn(sql: String, params: List<Any?>?): List<Any?> {
        try{
            return conn.queryColumn(sql, params);
        }catch (e:Exception){
            dbLogger.error("出错[${e.message}] sql: " + previewSql(sql, params))
            throw  e
        }
    }

    /**
     * 查询一行一列
     * @param sql
     * @param params
     * @return
     */
    public override fun queryCell(sql: String, params: List<Any?>?): Pair<Boolean, Any?> {
        try{
            return conn.queryCell(sql, params);
        }catch (e:Exception){
            dbLogger.error("出错[${e.message}] sql: " + previewSql(sql, params))
            throw  e
        }
    }

    /**
     * 开启事务
     */
    public override fun begin():Unit{
        if(transDepth++ === 0)
            conn.autoCommit = false; // 禁止自动提交事务
    }

    /**
     * 提交事务
     */
    public override fun commit():Boolean{
        // 未开启事务
        if (transDepth <= 0)
        return false;

        // 无嵌套事务
        if (--transDepth === 0)
        {
            // 回滚 or 提交事务: 回滚的话,返回false
            if(rollbacked)
                conn.rollback();
            else
                conn.commit()
            val result = rollbacked;
            rollbacked = false; // 清空回滚标记
            return result;
        }

        // 有嵌套事务
        return true;
    }

    /**
     * 回滚事务
     */
    public override fun rollback():Boolean{
        // 未开启事务
        if (transDepth <= 0)
            return false;

        // 无嵌套事务
        if (--transDepth === 0)
        {
            rollbacked = false; // 清空回滚标记
            conn.rollback(); // 回滚事务
        }

        // 有嵌套事务
        rollbacked = true; // 标记回滚
        return true;
    }

    /**
     * 关闭
     */
    public override fun close():Unit{
        closeDb(this)
    }

    /**
     * 转义多个表名
     *
     * @param tables 表名集合，其元素可以是String, 也可以是Pair<表名, 别名>
     * @param with_brackets 当拼接数组时, 是否用()包裹
     * @return
     */
    public override fun quoteTables(tables:Collection<Any>, with_brackets:Boolean):String
    {
        // 遍历多个表转义
        return tables.joinToString(", ", if(with_brackets) "(" else "", if(with_brackets) ")" else ""){
            // 单个表转义
            var table:String;
            var alias:String?;
            if(it is Pair<*, *>){ // 有别名
                table = it.component1() as String;
                alias = it .component2() as String;
            }else{ // 无别名
                table = it as String;
                alias = null;
            }
            // 单个表转义
            quoteTable(table, alias)
        }
    }

    /**
     * 转义表名
     *   mysql为`table`
     *   oracle为"table"
     *   sql server为"table" [table]
     *
     * @param table
     * @param alias 表别名
     * @return
     */
    public override fun quoteTable(table:String, alias:String?):String
    {
        return if(alias == null)
                    "$identifierQuoteString$table$identifierQuoteString";
                else // 表与别名之间不加 as，虽然mysql可识别，但oracle不能识别
                    "$identifierQuoteString$table$identifierQuoteString $identifierQuoteString$alias$identifierQuoteString"
    }

    /**
     * 转义多个字段名
     *
     * @param columns 表名集合，其元素可以是String, 也可以是Pair<字段名, 别名>
     * @param with_brackets 当拼接数组时, 是否用()包裹
     * @return
     */
    public override fun quoteColumns(columns:Collection<Any>, with_brackets:Boolean):String
    {
        // 遍历多个字段转义
        return columns.joinToString(", ", if(with_brackets) "(" else "", if(with_brackets) ")" else "") {
            // 单个字段转义
            quoteColumn(it)
        }
    }

    /**
     * 转义字段名
     *   mysql为`column`
     *   oracle为"column"
     *   sql server为"column" [column]
     *
     * @param column 字段名, 可以是字段数组
     * @param alias 字段别名
     * @param with_brackets 当拼接数组时, 是否用()包裹
     * @return
     */
    public override fun quoteColumn(column:String, alias:String?, with_brackets:Boolean):String
    {
        var table = "";
        var col = column;

        // 非函数表达式
        if ("^\\w[\\w\\d_\\.\\*]*".toRegex().matches(column))
        {
            // 表名
            if(column.contains('.')){
                var arr = column.split('.');
                table = "$identifierQuoteString${arr[0]}$identifierQuoteString.";
                col = arr[1]
            }

            // 字段名
            if(col == "*" || (dbType == DbType.Oracle && col == "rownum")) { // * 或 oracle的rownum 字段不转义
                //...
            }else{ // 其他字段转义
                col = "$identifierQuoteString$col$identifierQuoteString";
            }
        }

        // 字段别名
        if(alias == null)
            return "$table$col";

        return "$table$col AS $identifierQuoteString$alias$identifierQuoteString"; // 转义
    }

    /**
     * 转义单个值
     *
     * @param value 字段值, 可以是值数组
     * @return
     */
    public override fun quoteSingleValue(value: Any?): String {
        // null => "null"
        if (value == null)
            return "null";

        // bool => int
        if (value is Boolean)
            return if (value) "1" else "0";

        // int/float
        if (value is Number)
            return value.toString();

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
    public fun quoteDate(value: Date): String {
        val value = "'${value.format()}'"
        return if(dbType == DbType.Oracle)
                    "to_date($value,'yyyy-mm-dd hh24:mi:ss')"
                else
                    value
    }

    /**
     * 根据对象属性名，获得db字段名
     *    可根据实际需要在 model 类中重写
     *
     * @param prop 对象属性名
     * @return db字段名
     */
    public override fun prop2Column(prop:String): String {
        return prop2ColumnMapping.getOrPut(prop){
            var column = prop
            if(dbConfig["columnUnderline"]!!) // 字段有下划线
                column = column.camel2Underline()
            if(dbConfig["columnUpperCase"]!!)// 字段全大写
                column = column.toUpperCase() // 转大写
            column
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
            if(dbConfig["columnUpperCase"]!!)// 字段全大写
                prop = prop.toLowerCase() // 转小写
            if(dbConfig["columnUnderline"]!!) // 字段有下划线
                prop = prop.underline2Camel()
            prop
        }
    }

    /**
     * 预览sql
     * @param sql
     * @param params sql参数
     * @return
     */
    public override fun previewSql(sql: String, params: List<Any?>?): String {
        // 1 无参数
        if(params == null || params.isEmpty())
            return sql

        // 2 有参数：替换参数
        var i = 0 // 迭代索引
        return sql.replace("\\?".toRegex()) { matches: MatchResult ->
            quote(params[i++]) // 转义参数值
        }
    }
}
