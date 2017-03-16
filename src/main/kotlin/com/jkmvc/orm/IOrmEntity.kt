package com.jkmvc.orm

/**
 * ORM之实体对象
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
interface IOrmEntity{

    /**
     * 判断是否有某字段
     *
     * @param string column
     * @return
     */
    fun hasColumn(column: String): Boolean;

    /**
     * 设置对象字段值
     *
     * @param  string column 字段名
     * @param  mixed  value  字段值
     */
    operator fun set(column: String, value: Any?);

    /**
     * 获得对象字段
     *
     * @param   string column 字段名
     * @return  mixed
     */
    operator fun <T> get(column: String, defaultValue: Any? = null): T;

    /**
     * 设置多个字段值
     *
     * @param  array values   字段值的数组：<字段名 => 字段值>
     * @param  array expected 要设置的字段名的数组
     * @return ORM
     */
    fun values(values: Map<String, Any?>, expected: List<String>? = null): IOrm;

    /**
     * 获得字段值
     * @return array
     */
    fun asArray(): Map<String, Any?>;
}