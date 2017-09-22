package com.jkmvc.db

import com.jkmvc.common.Config
import com.jkmvc.common.deleteSuffix
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
         * 是否调试
         */
        public val debug:Boolean = Config.instance("jkmvc").getBoolean("debug.db")!!;

        /**
         * 数据源工厂
         */
        public var dataSourceFactory:IDataSourceFactory = DruidDataSourceFactory;

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
    }

    /**
     * 转移字符
     */
    protected val identityQuoteString by lazy(LazyThreadSafetyMode.NONE) {
        conn.metaData.identifierQuoteString
    }

    /**
     * 表的字段
     */
    protected val tableColumns: Map<String, List<String>> by lazy {
        val tables = HashMap<String, MutableList<String>>()
        // 查询所有表的所有列
        val rs = conn.metaData.getColumns(conn.catalog, null, null, null)
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
    public override fun <T> transaction(statement: Db.() -> T):T{
        try{
            begin(); // 开启事务
            val result:T = this .statement(); // 执行sql
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
     * @param returnGeneratedKey
     * @return
     */
    public override fun execute(sql: String, params: List<Any?>?, returnGeneratedKey:Boolean): Int {
        return conn.execute(sql, params, returnGeneratedKey);
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
        return conn.batchExecute(sql, paramses, paramSize)
    }

    /**
     * 查询多行
     * @param sql
     * @param params
     * @param action 处理结果的函数
     * @return
     */
    public override fun <T> queryResult(sql: String, params: List<Any?>?, action: (ResultSet) -> T): T {
        return conn.queryResult(sql, params, action)
    }

    /**
     * 查询多行
     * @param sql
     * @param params
     * @param transform 处理结果的函数
     * @return
     */
    public override fun <T> queryRows(sql: String, params: List<Any?>?, transform: (MutableMap<String, Any?>) -> T): List<T> {
        return conn.queryRows(sql, params, transform);
    }

    /**
     * 查询一行(多列)
     * @param sql
     * @param params
     * @param transform 处理结果的函数
     * @return
     */
    public override fun <T> queryRow(sql: String, params: List<Any?>?, transform: (MutableMap<String, Any?>) -> T): T? {
        return conn.queryRow(sql, params, transform);
    }

    /**
     * 查询一列(多行)
     * @param sql
     * @param params
     * @param transform 处理结果的函数
     * @return
     */
    public override fun queryColumn(sql: String, params: List<Any?>?): List<Any?> {
        return conn.queryColumn(sql, params);
    }

    /**
     * 查询一行一列
     * @param sql
     * @param params
     * @return
     */
    public override fun queryCell(sql: String, params: List<Any?>?): Pair<Boolean, Any?> {
        return conn.queryCell(sql, params);
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
        val str:StringBuilder = StringBuilder();
        for (item in tables)
        {
            var table:String;
            var alias:String?;
            if(item is Pair<*, *>){ // 有别名
                table = item.component1() as String;
                alias = item .component2() as String;
            }else{ // 无别名
                table = item as String;
                alias = null;
            }
            // 单个表转义
            str.append(quoteTable(table, alias)).append(", ");
        }
        str.deleteSuffix(", ");
        return if(with_brackets)  "($str)" else str.toString();
    }

    /**
     * 转义表名
     *
     * @param table
     * @param alias 表别名
     * @return
     */
    public override fun quoteTable(table:String, alias:String?):String
    {
        return if(alias == null)
            "`$table`";
        else
            "`$table`  AS `$alias`"
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
        val str:StringBuilder = StringBuilder();
        for (item in columns)
        {
            var column:String;
            var alias:String?;
            if(item is Pair<*, *>){ // 有别名
                column = item.component1() as String;
                alias = item .component2() as String;
            }else{ // 无别名
                column = item as String;
                alias = null;
            }

            // 单个字段转义
            str.append(quoteColumn(column, alias)).append(", ");
        }

        // 删最后逗号
        str.deleteSuffix(", ");

        // 加括号
        if(with_brackets)
            str.insert(0, '(').append(')');

        return str.toString();
    }

    /**
     * 转义字段名
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
        if ("^\\w[\\w\\d_\\.]*".toRegex().matches(column))
        {
            // 表名
            if(column.contains('.')){
                var arr = column.split('.');
                table = "`${arr[0]}`.";
                col = arr[1]
            }

            // 字段名
            if(column != "*") // 非*
                col = "`$col`"; // 转义
        }

        // 字段别名
        if(alias == null)
            return table + col;

        return table + col + " AS `$alias`"; // 转义
    }

    /**
     * 转义值
     *
     * @param value 字段值, 可以是值数组
     * @return
     */
    public override fun quote(values:Collection<Any?>):String
    {
        val str:StringBuffer = StringBuffer();
        return values.map {
            quote(it);
        }.joinToString(", ", "(", ")").toString() // 头部 + 连接符拼接多值 + 尾部
    }

    /**
     * 转义值
     *
     * @param value 字段值, 可以是值数组
     * @return
     */
    public override fun quote(value:Any?):Any?
    {
        // null => "null"
        if(value == null)
            return "null";

        // bool => int
        if(value is Boolean)
            return if(value) 1 else 0;

        // int/float
        if(value is Number)
            return value;

        // 非string => string
        return "$identityQuoteString$value$identityQuoteString";
    }
}
