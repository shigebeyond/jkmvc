package com.jkmvc.orm

import kotlin.properties.ReadWriteProperty

/**
 * ORM之实体对象
 *
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
interface IOrmEntity{

    /**
     * 获得属性代理
     */
    fun <T> property(): ReadWriteProperty<IOrm, T>;

    /**
     * 判断是否有某字段
     *
     * @param column
     * @return
     */
    fun hasColumn(column: String): Boolean;

    /**
     * 设置对象字段值
     *
     * @param column 字段名
     * @param  value  字段值
     */
    operator fun set(column: String, value: Any?);

    /**
     * 获得对象字段
     *
     * @param column 字段名
     * @return
     */
    operator fun <T> get(column: String, defaultValue: Any? = null): T;

    /**
     * 设置多个字段值
     *
     * @param values   字段值的数组：<字段名 to 字段值>
     * @param expected 要设置的字段名的数组
     * @return
     */
    fun values(values: Map<String, Any?>, expected: List<String>? = null): IOrm;

    /**
     * 获得字段值
     * @return
     */
    fun asArray(): Map<String, Any?>;
}