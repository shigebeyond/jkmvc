package com.jkmvc.db

import java.io.InputStream
import java.io.Reader
import java.sql.*
import java.util.*

/****************************** Connection直接提供查询与更新数据的方法 *******************************/
/**
 * Connection扩展
 * 执行更新
 *
 * @param sql
 * @param paras 参数
 * @param returnGeneratedKey 是否返回自动生成的主键，注：只针对int的自增长主键，不能是其他类型的主键，否则报错
 * @return
 */
public fun Connection.execute(sql: String, paras: List<Any?>? = null, returnGeneratedKey:Boolean = false): Int {
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
 * 查询多行

 * @param sql
 * @param paras 参数
 * @param action 结果转换函数
 * @return
 */
public fun <T> Connection.queryResult(sql: String, paras: List<Any?>? = null, action:(ResultSet) -> T): T {
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
 * @param sql
 * @param paras 参数
 * @param transform 结果转换函数
 * @return
 */
public fun <T> Connection.queryRows(sql: String, paras: List<Any?>? = null, transform:(MutableMap<String, Any?>) -> T): List<T> {
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
 * @param sql
 * @param paras 参数
 * @param transform 结果转换函数
 * @return
 */
public fun <T> Connection.queryRow(sql: String, paras: List<Any?>? = null, transform:(MutableMap<String, Any?>) -> T): T? {
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
 * @param sql
 * @param paras 参数
 * @return
 */
public fun Connection.queryCell(sql: String, paras: List<Any?>? = null): Pair<Boolean, Any?> {
    return queryResult<Pair<Boolean, Any?>>(sql, paras){ rs: ResultSet ->
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

        // 处理一行某列
        if(hasNext)
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