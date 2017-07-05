package com.jkmvc.db

/**
 * 封装查询结果
 * 扩展 [] 操作符，来代理属性读写
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
interface IRecord {

    /**
     * 设置属性
     */
    public operator fun set(column: String, value: Any?);

    /**
     * 读取属性
     */
    public operator fun <T> get(name: String, defaultValue: Any? = null): T ;

    /**
     * Get attribute of mysql type: varchar, char, enum, set, text, tinytext, mediumtext, longtext
     */
    fun getString(name: String): String

    /**
     * Get attribute of mysql type: int, integer, tinyint(n) n > 1, smallint, mediumint
     */
    fun getInt(name: String): Int?

    /**
     * Get attribute of mysql type: bigint, unsign int
     */
    fun getLong(name: String): Long?

    /**
     * Get attribute of mysql type: unsigned bigint
     */
    fun getBigInteger(name: String): java.math.BigInteger

    /**
     * Get attribute of mysql type: date, year
     */
    fun getDate(name: String): java.util.Date

    /**
     * Get attribute of mysql type: time
     */
    fun getTime(name: String): java.sql.Time

    /**
     * Get attribute of mysql type: timestamp, datetime
     */
    fun getTimestamp(name: String): java.sql.Timestamp

    /**
     * Get attribute of mysql type: real, double
     */
    fun getDouble(name: String): Double?

    /**
     * Get attribute of mysql type: float
     */
    fun getFloat(name: String): Float?

    /**
     * Get attribute of mysql type: bit, tinyint(1)
     */
    fun getBoolean(name: String): Boolean?

    /**
     * Get attribute of mysql type: tinyint(1)
     */
    fun getShort(name: String): Short?

    /**
     * Get attribute of mysql type: decimal, numeric
     */
    fun getBigDecimal(name: String): java.math.BigDecimal

    /**
     * Get attribute of mysql type: binary, varbinary, tinyblob, blob, mediumblob, longblob
     */
    fun getBytes(name: String): ByteArray

    /**
     * Get attribute of any type that extends from Number
     */
    fun getNumber(name: String): Number
}