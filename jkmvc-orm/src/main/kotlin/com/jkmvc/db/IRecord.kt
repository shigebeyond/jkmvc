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
    operator fun set(column: String, value: Any?);

    /**
     * 读取属性
     */
    operator fun <T> get(column: String, defaultValue: Any? = null): T ;

    /********************** 各种转为对应类型的getter，必须在接口中实现，以便同时被 Record/OrmEntity 继承，复用代码 *********************/
    /**
     * Get attribute of mysql type: varchar, char, enum, set, text, tinytext, mediumtext, longtext
     */
    fun getString(name: String): String {
        return this[name]
    }

    /**
     * Get attribute of mysql type: int, integer, tinyint(n) n > 1, smallint, mediumint
     */
    fun getInt(name: String): Int? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: bigint, unsign int
     */
    fun getLong(name: String): Long? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: unsigned bigint
     */
    fun getBigInteger(name: String): java.math.BigInteger {
        return this[name]
    }

    /**
     * Get attribute of mysql type: date, year
     */
    fun getDate(name: String): java.util.Date {
        return this[name]
    }

    /**
     * Get attribute of mysql type: time
     */
    fun getTime(name: String): java.sql.Time {
        return this[name]
    }

    /**
     * Get attribute of mysql type: timestamp, datetime
     */
    fun getTimestamp(name: String): java.sql.Timestamp {
        return this[name]
    }

    /**
     * Get attribute of mysql type: real, double
     */
    fun getDouble(name: String): Double? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: float
     */
    fun getFloat(name: String): Float? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: bit, tinyint(1)
     */
    fun getBoolean(name: String): Boolean? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: tinyint(1)
     */
    fun getShort(name: String): Short? {
        return this[name]
    }

    /**
     * Get attribute of mysql type: decimal, numeric
     */
    fun getBigDecimal(name: String): java.math.BigDecimal {
        return this[name]
    }

    /**
     * Get attribute of mysql type: binary, varbinary, tinyblob, blob, mediumblob, longblob
     */
    fun getBytes(name: String): ByteArray {
        return this[name]
    }

    /**
     * Get attribute of any type that extends from Number
     */
    fun getNumber(name: String): Number {
        return this[name]
    }

}