package com.jkmvc.db

import java.util.*

/**
 * 封装查询结果
 * 扩展 [] 操作符，来代理属性读写
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
class Record(protected val data: MutableMap<String, Any?> = HashMap<String, Any?>()) : IRecord {
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
        return (data.get(name) ?: defaultValue) as T
    }

    /**
     * Get attribute of mysql type: varchar, char, enum, set, text, tinytext, mediumtext, longtext
     */
    public override fun getString(name: String): String {
        return this[name]
    }

    /**
     * Get attribute of mysql type: int, integer, tinyint(n) n > 1, smallint, mediumint
     */
    public override fun getInt(name: String): Int? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: bigint, unsign int
     */
    public override fun getLong(name: String): Long? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: unsigned bigint
     */
    public override fun getBigInteger(name: String): java.math.BigInteger {
        return this[name]
    }

    /**
     * Get attribute of mysql type: date, year
     */
    public override fun getDate(name: String): java.util.Date {
        return this[name]
    }

    /**
     * Get attribute of mysql type: time
     */
    public override fun getTime(name: String): java.sql.Time {
        return this[name]
    }

    /**
     * Get attribute of mysql type: timestamp, datetime
     */
    public override fun getTimestamp(name: String): java.sql.Timestamp {
        return this[name]
    }

    /**
     * Get attribute of mysql type: real, double
     */
    public override fun getDouble(name: String): Double? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: float
     */
    public override fun getFloat(name: String): Float? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: bit, tinyint(1)
     */
    public override fun getBoolean(name: String): Boolean? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: tinyint(1)
     */
    public override fun getShort(name: String): Short? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: decimal, numeric
     */
    public override fun getBigDecimal(name: String): java.math.BigDecimal {
        return this[name]
    }

    /**
     * Get attribute of mysql type: binary, varbinary, tinyblob, blob, mediumblob, longblob
     */
    public override fun getBytes(name: String): ByteArray {
        return this[name]
    }

    /**
     * Get attribute of any type that extends from Number
     */
    public override fun getNumber(name: String): Number {
        return this[name]
    }

    public override fun toString(): String {
        return "${this.javaClass}: " + data.toString()
    }
}