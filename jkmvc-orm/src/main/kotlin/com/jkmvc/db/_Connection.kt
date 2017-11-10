package com.jkmvc.db

import com.jkmvc.common.findConstructor
import com.jkmvc.orm.IOrm
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.Reader
import java.sql.*
import java.util.*
import kotlin.reflect.KClass


// db的日志
val dbLogger = LoggerFactory.getLogger("com.jkmvc.db")

/****************************** Connection直接提供查询与更新数据的方法 *******************************/
/**
 * PreparedStatement扩展
 * 设置参数
 *
 * @param params 全部参数的列表
 * @param start 要设置的参数的开始序号
 * @param length 要设置的参数的个数
 * @param
 */
public fun PreparedStatement.setParameters(params: List<Any?>?, start:Int = 0, length:Int = if(params == null) 0 else params.size): PreparedStatement {
    if(params != null){
        if(length < 0 || length > params.size)
            throw ArrayIndexOutOfBoundsException("预编译sql中设置参数错误：需要的参数个数为 $length, 实际参数个数为 ${params.size}")

        // 设置参数
        for (i in 0..(length - 1)) {
            var value = params[start + i] /* 实际参数从start开始 */
            /**
             * fix bug: oracle执行sql报错： 无效的列类型
             * 原因：oracle的 DATE 类型字段，不能接受 java.util.Date 的值，只能接受 java.sql.Date 的值
             * 解决：转日期参数 java.util.Date -> java.sql.Date
             * 注意：我们还要兼顾oracle的2个类型 DATE|TIMESTAMP，mysql的4个类型 date|datetime|timestamp|time，为保证精度不丢失，统一使用 java.sql.Timestamp
             */
            if(value is java.util.Date && value !is java.sql.Date && value !is java.sql.Timestamp && value !is java.sql.Time)
                value =  java.sql.Timestamp(value.time)
            setObject(1 + i /* sql参数从1开始 */, value)
        }
    }
    return this
}

/**
 * Connection扩展
 * 执行更新
 *
 * @param sql
 * @param params 参数
 * @param returnGeneratedKey 是否返回自动生成的主键，注：只针对int的自增长主键，不能是其他类型的主键，否则报错
 * @return
 */
public fun Connection.execute(sql: String, params: List<Any?>? = null, returnGeneratedKey:Boolean = false): Int {
    var pst: PreparedStatement? = null
    var rs: ResultSet? = null;
    try{
        // 准备sql语句
        pst = prepareStatement(sql)
        // 设置参数
        pst.setParameters(params)
        // 执行
        val rows:Int = pst.executeUpdate()
        // insert语句，返回自动生成的主键
        if(returnGeneratedKey /*&& "INSERT.*".toRegex(RegexOption.IGNORE_CASE).matches(sql)*/){
            rs = pst.getGeneratedKeys(); //获取新增id
            rs.next();
            return rs.getInt(1); //返回新增id
        }
        // 非insert语句，返回行数
        return rows;
    }finally{
        rs?.close()
        pst?.close()
    }
}

/**
 * Connection扩展
 * 批量更新：每次更新sql参数不一样
 *
 * @param sql
 * @param params 多次处理的参数的汇总，一次处理取 lengthPerExec 个参数，必须保证他的大小是 lengthPerExec 的整数倍
 * @param lengthPerExec 一次处理的参数个数
 * @return
 */
public fun Connection.batchExecute(sql: String, params: List<Any?>, lengthPerExec:Int): IntArray {
    // 计算批处理的次数
    if(lengthPerExec <= 0 || params.size % lengthPerExec > 0)
        throw IllegalArgumentException("批处理sql中设置参数错误：一次处理需要的参数个数为$lengthPerExec, 全部参数个数为${params.size}, 后者必须是前者的整数倍");

    val batchNum:Int = params.size / lengthPerExec

    var pst: PreparedStatement? = null
    try{
        // 准备sql语句
        pst = prepareStatement(sql)
        // 逐次处理sql
        for(i in 0..(batchNum - 1)){
            // 设置参数
            pst.setParameters(params, i * lengthPerExec, lengthPerExec)
            // 添加sql
            pst.addBatch();
        }
        // 批量执行sql
        return pst.executeBatch()
    }finally{
        pst?.close()
    }
}

/**
 * 查询多行

 * @param sql
 * @param params 参数
 * @param action 结果转换函数
 * @return
 */
public fun <T> Connection.queryResult(sql: String, params: List<Any?>? = null, action:(ResultSet) -> T): T {
    var pst: PreparedStatement? = null;
    var rs: ResultSet? = null;
    try {
        // 准备sql语句
        pst = prepareStatement(sql)
        // 设置参数
        pst.setParameters(params)
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
 * @param sql
 * @param params 参数
 * @param transform 结果转换函数
 * @return
 */
public fun <T> Connection.queryRows(sql: String, params: List<Any?>? = null, transform:(MutableMap<String, Any?>) -> T): List<T> {
    return queryResult<List<T>>(sql, params){ rs: ResultSet ->
        // 处理查询结果
        val result = LinkedList<T>()
        rs.forEachRow { row:MutableMap<String, Any?> ->
            result.add(transform(row));// 转换一行数据
        }
        result;
    }
}

/**
 * 查询一列(多行)
 * @param sql
 * @param params 参数
 * @param transform 结果转换函数
 * @return
 */
public fun Connection.queryColumn(sql: String, params: List<Any?>? = null): List<Any?> {
    return queryResult<List<Any?>>(sql, params){ rs: ResultSet ->
        // 处理查询结果
        val result = LinkedList<Any?>()
        rs.forEachCell(1) { cell: Any? ->
            result.add(cell);
        }
        result;
    }
}

/**
 * 查询一行(多列)
 * @param sql
 * @param params 参数
 * @param transform 结果转换函数
 * @return
 */
public fun <T> Connection.queryRow(sql: String, params: List<Any?>? = null, transform:(MutableMap<String, Any?>) -> T): T? {
    return queryResult<T?>(sql, params){ rs: ResultSet ->
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
 * @param sql
 * @param params 参数
 * @return
 */
public fun Connection.queryCell(sql: String, params: List<Any?>? = null): Pair<Boolean, Any?> {
    return queryResult<Pair<Boolean, Any?>>(sql, params){ rs: ResultSet ->
        // 处理查询结果
        rs.nextCell(1)
    }
}

/****************************** 从ResultSet中获取数据 *******************************/
/**
 * 获得结果集的下一行
 * @return
 */
public inline fun ResultSet.nextRow(): MutableMap<String, Any?>? {
    if(next()) {
        // 获得一行
        val row:MutableMap<String, Any?> = HashMap<String, Any?>();
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
 * @param action 访问者函数
 */
public inline fun ResultSet.forEachRow(action: (MutableMap<String, Any?>) -> Unit): Unit {
    while(true){
        // 获得一行
        val row = nextRow()
        if(row == null)
            break;

        // 处理一行
        action(row)
    }
}

/**
 * 访问结果集的下一行的某列
 * @param i 第几列
 * @return
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
 * @param i 第几列
 * @param action 处理函数
 * @return
 */
public inline fun ResultSet.forEachCell(i:Int, action: (Any?) -> Unit): Unit {
    while(true){
        // 获得一行某列
        val (hasNext, value) = nextCell(i)
        if(!hasNext)
            break;

        // 处理一行某列
        action(value)
    }
}

/****************************** 读取Blob/Clob字段 *******************************/
/**
 * Blob转ByteArray
 * @return
 */
public fun Blob?.toByteArray(): ByteArray? {
    if (this == null)
        return null

    var input: InputStream? = null
    try {
        input = this.binaryStream
        if (input == null)
            return null
        val data = ByteArray(this.length().toInt())        // byte[] data = new byte[is.available()];
        if (data.size == 0)
            return null
        input.read(data)
        return data
    } finally {
        input?.close()

    }
}

/**
 * Clob转String
 * @return
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

/****************************** 记录转换器 *******************************/
/**
 * 在查找对应带 Map 参数的构造函数时，所需要的参数类型列表
 */
private val tranformingConstructorParamTypes = listOf(MutableMap::class.java)

/**
 * 获得类的记录转换器
 *   不同的目标类型，有不同的记录转换器
 *   1 Orm类：实例化并调用original()
 *   2 Map类: 直接返回记录数据，不用转换
 *   3 其他类：如果实现带 Map 参数的构造函数，如 constructor(data: MutableMap<String, Any?>)，就调用
 *
 * @param clazz 要转换的类
 * @return 转换的匿名函数
 */
public val <T:Any> KClass<T>.recordTranformer: ((MutableMap<String, Any?>) -> T)
    get(){
        // 1 如果是orm类，则实例化并调用original()
        if(IOrm::class.java.isAssignableFrom(java)){
            // TODO: 优化性能，缓存结果
            return {
                val obj = java.newInstance() as IOrm;
                obj.original(it) as T
            }
        }

        // 2 如果是map类，则直接返回，不用转换
        if(Map::class.java.isAssignableFrom(java)){
            return {
                it as T;
            }
        }

        // 3 否则，调用该类的构造函数（假定该类有接收map参数的构建函数）
        // 获得类的构造函数
        val construtor = this.findConstructor(tranformingConstructorParamTypes)
        if(construtor == null)
            throw UnsupportedOperationException("类${this}没有构造函数constructor(MutableMap)");

        // 调用构造函数
        return {
            construtor.call(it) as T; // 转换一行数据: 直接调用构造函数
        }
    }