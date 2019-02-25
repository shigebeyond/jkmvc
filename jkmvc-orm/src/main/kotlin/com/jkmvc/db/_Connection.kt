package com.jkmvc.db

import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.sql.*
import java.util.*
import kotlin.reflect.KClass


// db的日志
val dbLogger = LoggerFactory.getLogger("com.jkmvc.db")

typealias Row = Map<String, Any?>
typealias MutableRow = MutableMap<String, Any?>

/****************************** Connection直接提供查询与更新数据的方法 *******************************/
/**
 * 全局共享的可复用的用于存储一行数据的 HashMap 对象池
 *   就是每查到一行, 就用该对象来临时存储该行数据, 用完就清空该行数据
 *   线程安全, 每个线程只共享一个 HashMap 对象, 无论你查到多少行, 仅占用一个 HashMap 对象, 极大的节省内存
 *   用在 queryRow() / queryRows() 的 transform 函数中, 参数就是不可变的行数据(类型转为 Row), 表示禁止改变或引用行数据
 */
private val reusedRows:ThreadLocal<MutableRow> = ThreadLocal.withInitial {
    HashMap<String, Any?>();
}

/**
 * PreparedStatement扩展
 * 设置参数
 *
 * @param params 全部参数的列表
 * @param start 要设置的参数的开始序号
 * @param length 要设置的参数的个数
 * @param
 */
public fun PreparedStatement.setParameters(params: List<Any?>, start:Int = 0, length:Int = params.size): PreparedStatement {
    if(length < 0 || length > params.size)
        throw ArrayIndexOutOfBoundsException("预编译sql中设置参数错误：需要的参数个数为 $length, 实际参数个数为 ${params.size}")

    // 设置参数
    for (i in 0..(length - 1)) {
        // 实际参数从start开始
        var value = params[start + i]

        // 枚举值: 转int
        if(value is Enum<*>)
            value = value.ordinal

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
    return this
}

/**
 * Connection扩展
 * 执行更新
 *
 * @param sql
 * @param params 参数
 * @param generatedColumn 返回的自动生成的主键名
 *       注：只针对int的自增长主键，不能是其他类型的主键，否则报错
 *       注：mysql可以不指定自增主键名，但oracle必须指定，否则调用pst.getGeneratedKeys()报错：不允许的操作
 * @return
 */
public fun Connection.execute(sql: String, params: List<Any?> = emptyList(), generatedColumn:String? = null): Int {
    var pst: PreparedStatement? = null
    var rs: ResultSet? = null;
    try{
        // 准备sql语句
        pst = if(generatedColumn == null)
                prepareStatement(sql)
              else
                prepareStatement(sql, arrayOf(generatedColumn)) // fix bug: oracle必须指定第二个参数，否则调用pst.getGeneratedKeys()报错：不允许的操作
        // 设置参数
        pst.setParameters(params)
        // 执行
        val rows:Int = pst.executeUpdate()
        // insert语句，返回自动生成的主键
        if(generatedColumn != null /*&& "INSERT.*".toRegex(RegexOption.IGNORE_CASE).matches(sql)*/)
            return getGeneratedKey(pst)

        // 非insert语句，返回行数
        return rows;
    }finally{
        rs?.close()
        pst?.close()
    }
}

/**
 * 获得自动生成的主键
 *
 * @param pst
 * @return
 */
private fun getGeneratedKey(pst: PreparedStatement): Int {
    var rs: ResultSet? = null
    try {
        rs = pst.getGeneratedKeys(); //获取新增id
        rs.next();
        return rs.getInt(1); //返回新增id
    }finally{
        rs?.close()
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
public fun <T> Connection.queryResult(sql: String, params: List<Any?> = emptyList(), action:(ResultSet) -> T): T {
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
public fun <T> Connection.queryRows(sql: String, params: List<Any?> = emptyList(), transform: (Row) -> T): List<T> {
    return queryResult<List<T>>(sql, params){ rs: ResultSet ->
        // 处理查询结果
        val result = LinkedList<T>()
        rs.forEachRow { row: Row ->
            result.add(transform(row));// 转换一行数据
        }
        result;
    }
}

/**
 * 查询一列(多行)
 * @param sql
 * @param params 参数
 * @param clazz 值类型
 * @param transform 结果转换函数
 * @return
 */
public fun <T:Any> Connection.queryColumn(sql: String, params: List<Any?> = emptyList(), clazz: KClass<T>? = null): List<T?> {
    return queryResult(sql, params){ rs: ResultSet ->
        // 处理查询结果
        val result = LinkedList<T?>()
        rs.forEachCell(1, clazz) { cell: T? ->
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
public fun <T> Connection.queryRow(sql: String, params: List<Any?> = emptyList(), transform: (Row) -> T): T? {
    val row: MutableRow = reusedRows.get() // 复用map
    return queryResult<T?>(sql, params){ rs: ResultSet ->
        // 处理查询结果
        rs.nextRow(row)

        var result:T? = null
        if(row.isNotEmpty()) {
            try {
                // 转换一行数据
                result = transform(row)
            }finally {
                // 清空以便复用
                row.clear()
            }
        }
        result
    }
}

/**
 * 查询一行一列
 * @param sql
 * @param params 参数
 * @return
 */
public fun <T:Any> Connection.queryCell(sql: String, params: List<Any?> = emptyList(), clazz: KClass<T>? = null): Cell<T> {
    return queryResult(sql, params){ rs: ResultSet ->
        // 处理查询结果
        rs.nextCell(1, clazz)
    }
}

/****************************** 从ResultSet中获取数据 *******************************/
/**
 * 获得结果集的单个值
 *   参考 org.springframework.jdbc.support.JdbcUtils#getResultSetValue(java.sql.ResultSet, int)
 * @param i
 * @return
 */
public fun ResultSet.getValue(i:Int): Any? {
    val obj: Any? = getObject(i)
    if(obj == null)
        return null

    // 二进制大对象
    if (obj is Blob)
        return obj.getBytes(1, obj.length().toInt())

    // 字符型大对象
    if (obj is Clob)
        return obj.getSubString(1, obj.length().toInt())

    // oracle时间对象
    val className = obj.javaClass.name
    if ("oracle.sql.TIMESTAMP" == className || "oracle.sql.TIMESTAMPTZ" == className)
        return getTimestamp(i)

    if (className.startsWith("oracle.sql.DATE")) {
        val metaDataClassName = metaData.getColumnClassName(i)
        if ("java.sql.Timestamp" == metaDataClassName || "oracle.sql.TIMESTAMP" == metaDataClassName) {
            return getTimestamp(i)
        }

        return  getDate(i)
    }

    return obj
}

/**
 * 根据返回值的类型 XXX 来获得 ResultSet 的 getXXX() 方法, 即某列的取值方法
 *   参考 org.springframework.jdbc.support.JdbcUtils#getResultSetValue(java.sql.ResultSet, int, java.lang.Class<?>)
 * @param clazz
 * @return
 */
public fun getResultSetValueGetter(clazz: KClass<*>? = null): (ResultSet.(Int) -> Any?) {
    if(clazz == null)
        return ResultSet::getValue

    return when(clazz){
        String::class -> ResultSet::getString

        Int::class -> ResultSet::getInt
        Long::class -> ResultSet::getLong
        Float::class -> ResultSet::getFloat
        Double::class -> ResultSet::getDouble
        Boolean::class -> ResultSet::getBoolean
        Short::class -> ResultSet::getShort
        Byte::class -> ResultSet::getByte
        BigDecimal::class -> ResultSet::getBigDecimal

        java.util.Date::class -> ResultSet::getDate
        java.sql.Date::class -> ResultSet::getDate
        java.sql.Time::class -> ResultSet::getTime
        java.sql.Timestamp::class -> ResultSet::getTimestamp

        ByteArray::class -> ResultSet::getBytes
        Blob::class -> ResultSet::getBlob
        Clob::class -> ResultSet::getClob

        else -> { columnIndex:Int -> getObject(columnIndex, clazz.java) }
    }
}

/**
 * 获得结果集的下一行
 * @return
 */
public inline fun ResultSet.nextRow(row:MutableRow): Unit {
    if(next()) {
        // 获得一行
        for (i in 1..metaData.columnCount) { // 多列
            val label: String = metaData.getColumnLabel(i); // 字段名
            val value: Any? = getValue(i) // 字段值
            row[label] = value;
        }
    }
}

/**
 * 遍历结果集的每一行
 * @param action 访问者函数
 */
public fun ResultSet.forEachRow(action: (Row) -> Unit): Unit {
    val row: MutableRow = reusedRows.get() // 复用map
    while(true){
        // 获得一行
        nextRow(row)
        if(row.isEmpty())
            break;

        try {
            // 处理一行
            action(row)
        }finally {
            // 清空以便复用
            row.clear()
        }
    }
}

/**
 * 访问结果集的下一行的某列的值
 * @param i 第几列
 * @param clazz 值类型
 * @return
 */
public inline fun <T:Any> ResultSet.nextCell(i:Int, clazz: KClass<T>? = null): Cell<T> {
    val hasNext = next()
    val getter = getResultSetValueGetter(clazz)
    var value: T? = if(hasNext) (getter(i) as T) else null; // 字段值
    return Cell(hasNext, value);
}

/**
 * 遍历结果集的每一行的某列
 * @param i 第几列
 * @param clazz值类型
 * @param action 处理函数
 * @return
 */
public inline fun <T:Any> ResultSet.forEachCell(i:Int, clazz: KClass<T>? = null, action: (T?) -> Unit): Unit {
    while(true){
        // 获得一行某列
        val (hasNext, value) = nextCell(i, clazz)
        if(!hasNext)
            break;

        // 处理一行某列
        action(value)
    }
}
