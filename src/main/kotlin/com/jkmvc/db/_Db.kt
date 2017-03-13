package com.jkmvc.db

import com.alibaba.druid.pool.DruidDataSource
import com.jkmvc.common.Config
import java.io.InputStream
import java.io.Reader
import java.sql.*
import java.util.*

/**
 * StringBuilder扩展
 *  删除最后的一段子字符串
 */
public fun StringBuilder.delete(str:String):StringBuilder {
    val end = length - 1;
    val start = end - str.length;
    return delete(start, end);
}

/**
 * 获得数据源
 */
public fun getDruidDataSource(name:String = "database"): DruidDataSource {
    val config: Config = Config.instance("$name.properties")!!;
    val ds:DruidDataSource = DruidDataSource()

    // 基本属性 url、user、password
    ds.setUrl(config["url"])
    ds.setUsername(config["username"])
    ds.setPassword(config["password"])
    if (config["driverClass"] != null) //  若为 null 让 druid 自动探测 driverClass 值
        ds.setDriverClassName(config["driverClass"])

    ds.setInitialSize(config.getInt("initialSize", 10)!!) // 初始连接池大小
    ds.setMinIdle(config.getInt("minIdle", 10)!!) // 最小空闲连接数
    ds.setMaxActive(config.getInt("maxActive", 100)!!) // 最大活跃连接数
    ds.setMaxWait(config.getLong("maxWait", DruidDataSource.DEFAULT_MAX_WAIT.toLong())!!) // 配置获取连接等待超时的时间
    ds.setTimeBetweenConnectErrorMillis(config.getLong("timeBetweenConnectErrorMillis", DruidDataSource.DEFAULT_TIME_BETWEEN_CONNECT_ERROR_MILLIS)!!) // 配置发生错误时多久重连
    ds.setTimeBetweenEvictionRunsMillis(config.getLong("timeBetweenEvictionRunsMillis", DruidDataSource.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS)!!) // 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
    ds.setMinEvictableIdleTimeMillis(config.getLong("minEvictableIdleTimeMillis", DruidDataSource.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS)!!) // 配置连接在池中最小生存的时间

    /**
     * hsqldb - "select 1 from INFORMATION_SCHEMA.SYSTEM_USERS"
     * Oracle - "select 1 from dual"
     * DB2 - "select 1 from sysibm.sysdummy1"
     * mysql - "select 1"
     */
    ds.setValidationQuery(config.get("validationQuery", "select 1"))
    ds.setTestWhileIdle(config.getBoolean("testWhileIdle", true)!!)
    ds.setTestOnBorrow(config.getBoolean("testOnBorrow", true)!!)
    ds.setTestOnReturn(config.getBoolean("testOnReturn", true)!!)


    ds.setRemoveAbandoned(config.getBoolean("removeAbandoned", false)!!) // 是否打开连接泄露自动检测
    ds.setRemoveAbandonedTimeoutMillis(config.getLong("removeAbandonedTimeoutMillis", 300 * 1000)!!) // 连接长时间没有使用，被认为发生泄露时长
    ds.setLogAbandoned(config.getBoolean("logAbandoned", false)!!)  // 发生泄露时是否需要输出 log，建议在开启连接泄露检测时开启，方便排错

    //只要maxPoolPreparedStatementPerConnectionSize>0,poolPreparedStatements就会被自动设定为true，参照druid的源码
    ds.setMaxPoolPreparedStatementPerConnectionSize(config.getInt("maxPoolPreparedStatementPerConnectionSize", -1)!!)

    // 配置监控统计拦截的filters
    val filters: String? = config["filters"]    // 监控统计："stat"    防SQL注入："wall"     组合使用： "stat,wall"
    if (!filters.isNullOrBlank())
        ds.setFilters(filters)

    return ds;
}

/**
 * Connection扩展
 * 执行更新
 */
public fun Connection.execute(sql: String, paras: List<Any?>?): Int {
    var pst: PreparedStatement? = null
    var rs: ResultSet? = null;
    try{
        // 准备sql语句
        pst = prepareStatement(sql)
        // 设置参数
        if(paras != null)
            for (i in paras.indices)
                pst.setObject(i + 1, paras[i])
        // 执行
        val rows:Int = pst.executeUpdate()
        // 如果是insert语句，则返回新增id
        if("INSERT INTO".toRegex(RegexOption.IGNORE_CASE).matches(sql)){
            rs = pst.getGeneratedKeys(); //获取新增id
            rs.next();
            return rs.getInt(1); //返回新增id
        }
        // 非insert语句返回行数
        return rows;
    }finally{
        rs?.close()
        pst?.close()
    }
}

/**
 * 查询多行
 */
public fun <T> Connection.queryResult(sql: String, paras: List<Any?>?, action:(ResultSet) -> T): T {
    var pst: PreparedStatement? = null;
    var rs: ResultSet? = null;
    try {
        // 准备sql语句
        pst = prepareStatement(sql)
        // 设置参数
        if(paras != null)
            for (i in paras.indices)
                pst.setObject(i + 1, paras[i])
        // 查询
        rs = pst.executeQuery()
        // 处理查询结果
        return action(rs);
    } finally {
        rs?.close()
        pst?.close()
    }
}

/**
 * 查询多行
 */
public fun <T> Connection.queryRows(sql: String, paras: List<Any?>?, transform:(MutableMap<String, Any?>) -> T): List<T> {
    return queryResult<List<T>>(sql, paras){ rs: ResultSet ->
        // 处理查询结果
        val result = LinkedList<T>()
        rs.forEachRow { row:MutableMap<String, Any?> ->
            result.add(transform(row));// 转换一行数据
        }
        result;
    }
}

/**
 * 查询一行(多列)
 */
public fun <T> Connection.queryRow(sql: String, paras: List<Any?>?, transform:(MutableMap<String, Any?>) -> T): T? {
    return queryResult<T?>(sql, paras){ rs: ResultSet ->
        // 处理查询结果
        val row:MutableMap<String, Any?>? = rs.nextRow()
        if(row == null)
            null
        else
            transform(row);// 转换一行数据
    }
}

/**
 * 查询一行一列
 */
public fun Connection.queryCell(sql: String, paras: List<Any?>?): Pair<Boolean, Any?> {
    return queryResult<Pair<Boolean, Any?>>(sql, paras){ rs: ResultSet ->
        // 处理查询结果
        rs.nextCell(1)
    }
}

/**
 * 获得结果集的下一行
 */
public inline fun ResultSet.nextRow(): MutableMap<String, Any?>? {
    if(next()) {
        // 获得一行
        val row:MutableMap<String, Any?> = LinkedHashMap<String, Any?>();
        val rsmd = metaData
        for (i in 1..rsmd.columnCount) { // 多列
            val type: Int = rsmd.getColumnType(i); // 类型
            val label: String = rsmd.getColumnLabel(i); // 字段名
            val value: Any? // 字段值
            when (type) {
                Types.CLOB -> value = getClob(i).toString()
                Types.NCLOB -> value = getNClob(i).toString()
                Types.BLOB -> value = getBlob(i).toByteArray()
                else -> value = getObject(i)
            }
            row[label] = value;
        }
        return row
    }

    return null;
}
/**
 * 遍历结果集的每一行
 */
public inline fun ResultSet.forEachRow(action: (MutableMap<String, Any?>) -> Unit): Unit {
    while(true){
        // 获得一行
        val row = nextRow()

        // 处理一行
        if(row != null)
            action(row)
    }
}

/**
 * 访问结果集的下一行的某列
 */
public inline fun ResultSet.nextCell(i:Int): Pair<Boolean, Any?> {
    val hasNext = next()
    var value: Any? = null; // 字段值
    if(hasNext) {
        // 获得一行的某列
        val rsmd = metaData
        val type: Int = rsmd.getColumnType(i); // 类型
        when (type) { // 字段值
            Types.CLOB -> value = getClob(i).toString()
            Types.NCLOB -> value = getNClob(i).toString()
            Types.BLOB -> value = getBlob(i).toByteArray()
            else -> value = getObject(i)
        }
    }

    return Pair<Boolean, Any?>(hasNext, value);
}

/**
 * 遍历结果集的每一行的某列
 */
public inline fun ResultSet.forEachCell(i:Int, action: (Any?) -> Unit): Unit {
    while(true){
        // 获得一行某列
        val (hasNext, value) = nextCell(i)

        // 处理一行某列
        if(hasNext)
            action(value)
    }
}

/**
 * Blob转ByteArray
 */
public fun Blob?.toByteArray(): ByteArray? {
    if (this == null)
        return null

    var `is`: InputStream? = null
    try {
        `is` = this.binaryStream
        if (`is` == null)
            return null
        val data = ByteArray(this.length().toInt())        // byte[] data = new byte[is.available()];
        if (data.size == 0)
            return null
        `is`.read(data)
        return data
    } finally {
        `is`?.close()

    }
}

/**
 * Clob转String
 */
public fun Clob?.toString(): String? {
    if (this == null)
        return null

    var reader: Reader? = null
    try {
        reader = this.characterStream
        if (reader == null)
            return null
        val buffer = CharArray(this.length().toInt())
        if (buffer.size == 0)
            return null
        reader.read(buffer)
        return String(buffer)
    } finally {
        reader?.close()
    }
}