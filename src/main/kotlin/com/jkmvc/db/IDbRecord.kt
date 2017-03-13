package com.jkmvc.db

import java.util.*

/**
 * 封装查询结果
 * 扩展 [] 操作符，来代理属性读写
 */
interface IDbRecord {

    /**
     * 设置属性
     */
    public operator fun set(k: String, v: Any?);

    /**
     * 读取属性
     */
    public operator fun <T> get(name: String, defaultValue: Any? = null): T ;

    /**
     * Get attribute of mysql type: varchar, char, enum, set, text, tinytext, mediumtext, longtext
     */
    public fun getStr(name: String): String ;

    /**
     * Get attribute of mysql type: int, integer, tinyint(n) n > 1, smallint, mediumint
     */
    public fun getInt(name: String): Int?;

    /**
     * Get attribute of mysql type: bigint, unsign int
     */
    public fun getLong(name: String): Long? ;

    /**
     * Get attribute of mysql type: unsigned bigint
     */
    public fun getBigInteger(name: String): java.math.BigInteger ;

    /**
     * Get attribute of mysql type: date, year
     */
    public fun getDate(name: String): java.util.Date ;

    /**
     * Get attribute of mysql type: time
     */
    public fun getTime(name: String): java.sql.Time ;

    /**
     * Get attribute of mysql type: timestamp, datetime
     */
    public fun getTimestamp(name: String): java.sql.Timestamp;

    /**
     * Get attribute of mysql type: real, double
     */
    public fun getDouble(name: String): Double? ;

    /**
     * Get attribute of mysql type: float
     */
    public fun getFloat(name: String): Float?;

    /**
     * Get attribute of mysql type: bit, tinyint(1)
     */
    public fun getBoolean(name: String): Boolean? ;

    /**
     * Get attribute of mysql type: decimal, numeric
     */
    public fun getBigDecimal(name: String): java.math.BigDecimal;
    /**
     * Get attribute of mysql type: binary, varbinary, tinyblob, blob, mediumblob, longblob
     */
    public fun getBytes(name: String): ByteArray;
    /**
     * Get attribute of any type that extends from Number
     */
    public fun getNumber(name: String): Number;
}