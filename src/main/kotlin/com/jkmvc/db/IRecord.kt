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
    public fun getStr(name: String): String {
        return this[name]
    }

    /**
     * Get attribute of mysql type: int, integer, tinyint(n) n > 1, smallint, mediumint
     */
    public fun getInt(name: String): Int? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: bigint, unsign int
     */
    public fun getLong(name: String): Long? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: unsigned bigint
     */
    public fun getBigInteger(name: String): java.math.BigInteger {
        return this[name]
    }

    /**
     * Get attribute of mysql type: date, year
     */
    public fun getDate(name: String): java.util.Date {
        return this[name]
    }

    /**
     * Get attribute of mysql type: time
     */
    public fun getTime(name: String): java.sql.Time {
        return this[name]
    }

    /**
     * Get attribute of mysql type: timestamp, datetime
     */
    public fun getTimestamp(name: String): java.sql.Timestamp {
        return this[name]
    }

    /**
     * Get attribute of mysql type: real, double
     */
    public fun getDouble(name: String): Double? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: float
     */
    public fun getFloat(name: String): Float? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: bit, tinyint(1)
     */
    public fun getBoolean(name: String): Boolean? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: decimal, numeric
     */
    public fun getBigDecimal(name: String): java.math.BigDecimal {
        return this[name]
    }

    /**
     * Get attribute of mysql type: binary, varbinary, tinyblob, blob, mediumblob, longblob
     */
    public fun getBytes(name: String): ByteArray {
        return this[name]
    }

    /**
     * Get attribute of any type that extends from Number
     */
    public fun getNumber(name: String): Number {
        return this[name]
    }
}