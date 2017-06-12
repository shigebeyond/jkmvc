package com.jkmvc.db

import java.util.*

/**
 * 编译好的sql
 *
 * @author shijianhang
 * @date 2017-6-10 下午8:02:47
 */
interface ICompiledSql {
    /**
     * 编译好的sql
     */
    var sql: String

    /**
     * 编译后的sql参数 / 静态参数
     */
    public var staticParams: LinkedList<Any?>

    /**
     * 构建实际参数 = 静态参数 + 动态参数
     *
     * @param params 动态参数
     * @return
     */
    fun buildParams(dynamicParams: Array<out Any?>): List<Any?>

    /**
     * 判断是否为空
     * @return
     */
    fun isEmpty(): Boolean

    /**
     * 清空编译结果
     * @return
     */
    fun clear(): CompiledSql

    /**
     * 预览sql
     *
     * @param params 动态参数
     * @return
     */
    fun previewSql(dynamicParams:Array<out Any?>? = null): String
}