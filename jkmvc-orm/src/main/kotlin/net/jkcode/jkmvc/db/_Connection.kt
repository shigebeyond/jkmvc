package net.jkcode.jkmvc.db

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

typealias DbRow = Map<String, Any?>

/****************************** PreparedStatement扩展 *******************************/
/**
 * 设置参数
 *
 * @param params 全部参数的列表
 * @param start 要设置的参数的开始序号
 * @param length 要设置的参数的个数
 * @param
 */
public inline fun PreparedStatement.setParameters(params: List<*>, start:Int = 0, length:Int = params.size): PreparedStatement {
    if(length < 0 || length > params.size)
        //throw IllegalArgumentException("预编译sql中设置参数错误：需要的参数个数为 $length, 实际参数个数为 ${params.size}")
        throw IllegalArgumentException("Mismatch `PreparedStatement` parameter size： It needs [$length], but pass [${params.size}]")

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
 * 获得自动生成的主键
 *
 * @param pst
 * @return
 */
public inline fun PreparedStatement.getGeneratedKey(): Long {
    var rs: ResultSet? = null
    try {
        rs = this.getGeneratedKeys(); //获取新增id
        rs.next();
        return rs.getLong(1); //返回新增id
    }finally{
        rs?.close()
    }
}

/****************************** Connection直接提供查询与更新数据的方法 *******************************/
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
public inline fun Connection.execute(sql: String, params: List<*> = emptyList<Any>(), generatedColumn:String? = null): Long {
    var pst: PreparedStatement? = null
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
            return pst.getGeneratedKey()

        // 非insert语句，返回行数
        return rows.toLong()
    }finally{
        pst?.close()
    }
}

/**
 * Connection扩展
 * 执行更新, 并处理结果集
 *
 * @param sql
 * @param params 参数
 * @param transform 结果转换函数
 * @return
 */
public inline fun <T> Connection.execute(sql: String, params: List<*> = emptyList<Any>(), transform: (ResultSet) -> T): T? {
    var pst: PreparedStatement? = null
    try{
        // 准备sql语句
        pst = prepareStatement(sql)
        // 设置参数
        pst.setParameters(params)
        // 执行
        pst.executeUpdate()
        // 处理结果集
        val rs = pst.getResultSet()
        return rs?.use {
            transform(it)
        }
    }finally{
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
public inline fun Connection.batchExecute(sql: String, params: List<*>, lengthPerExec:Int): IntArray {
    // 计算批处理的次数
    if(lengthPerExec <= 0 || params.size % lengthPerExec > 0)
        //throw IllegalArgumentException("批处理sql中设置参数错误：一次处理需要的参数个数为$lengthPerExec, 全部参数个数为${params.size}, 后者必须是前者的整数倍");
        throw IllegalArgumentException("Mismatch `Connection.batchExecute()` parameter size：It handles [$lengthPerExec] paramters once, and only accepts an integral multiple of [$lengthPerExec], but you pass [${params.size}]");

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
 * 查询并处理结果集

 * @param sql
 * @param params 参数
 * @param transform 结果转换函数
 * @return
 */
public inline fun <T> Connection.queryResult(sql: String, params: List<*> = emptyList<Any>(), transform:(ResultSet) -> T): T {
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
        return transform(rs);
    } finally {
        rs?.close()
        pst?.close()
    }
}


