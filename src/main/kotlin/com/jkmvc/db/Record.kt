package com.jkmvc.db

import java.util.*

/**
 * 封装查询结果
 * 扩展 [] 操作符，来代理属性读写
 */
open class Record(protected val data: MutableMap<String, Any?> = LinkedHashMap<String, Any?>()) : IRecord {
    /**
     * 设置属性
     */
    public override operator fun set(column: String, value: Any?) {
        data.set(column, value)
    }

    /**
     * 读取属性
     */
    public override operator fun <T> get(name: String, defaultValue: Any?): T {
        val result = data.get(name)
        return (result ?: defaultValue) as T
    }

    /**
     * Get attribute of mysql type: varchar, char, enum, set, text, tinytext, mediumtext, longtext
     */
    public override fun getStr(name: String): String {
        return data[name] as String
    }

    /**
     * Get attribute of mysql type: int, integer, tinyint(n) n > 1, smallint, mediumint
     */
    public override fun getInt(name: String): Int? {
        return data[name] as Int
    }

    /**
     * Get attribute of mysql type: bigint, unsign int
     */
    public override fun getLong(name: String): Long? {
        return data[name] as Long
    }

    /**
     * Get attribute of mysql type: unsigned bigint
     */
    public override fun getBigInteger(name: String): java.math.BigInteger {
        return data[name] as java.math.BigInteger
    }

    /**
     * Get attribute of mysql type: date, year
     */
    public override fun getDate(name: String): java.util.Date {
        return data[name] as java.util.Date
    }

    /**
     * Get attribute of mysql type: time
     */
    public override fun getTime(name: String): java.sql.Time {
        return data[name] as java.sql.Time
    }

    /**
     * Get attribute of mysql type: timestamp, datetime
     */
    public override fun getTimestamp(name: String): java.sql.Timestamp {
        return data[name] as java.sql.Timestamp
    }

    /**
     * Get attribute of mysql type: real, double
     */
    public override fun getDouble(name: String): Double? {
        return data[name] as Double
    }

    /**
     * Get attribute of mysql type: float
     */
    public override fun getFloat(name: String): Float? {
        return data[name] as Float
    }

    /**
     * Get attribute of mysql type: bit, tinyint(1)
     */
    public override fun getBoolean(name: String): Boolean? {
        return data[name] as Boolean
    }

    /**
     * Get attribute of mysql type: decimal, numeric
     */
    public override fun getBigDecimal(name: String): java.math.BigDecimal {
        return data[name] as java.math.BigDecimal
    }

    /**
     * Get attribute of mysql type: binary, varbinary, tinyblob, blob, mediumblob, longblob
     */
    public override fun getBytes(name: String): ByteArray {
        return data[name] as ByteArray
    }

    /**
     * Get attribute of any type that extends from Number
     */
    public override fun getNumber(name: String): Number {
        return data[name] as Number
    }

    override fun toString(): String {
        return data.toString()
    }
}