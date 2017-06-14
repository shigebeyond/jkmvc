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
    var staticParams: LinkedList<Any?>

    /**
     * 动态参数的个数 = 静态参数中?的个数
     */
    val dynamicParamsSize:Int;

    /**
     * 判断是否为空
     * @return
     */
    fun isEmpty(): Boolean

    /**
     * 清空编译结果
     * @return
     */
    fun clear(): ICompiledSql

    /**
     * 构建实际参数 = 静态参数 + 动态参数
     *
     * @param params 动态参数
     * @return
     */
    fun buildParams(dynamicParams: List<Any?> = emptyList()): List<Any?>

    /**
     * 构建实际参数 = 静态参数 + 动态参数
     *
     * @param params 动态参数
     * @return
     */
    fun buildParams(dynamicParams: Array<out Any?>): List<Any?>{
       return buildParams(dynamicParams.asList())
    }

    /**
     * 构建实际参数 = 静态参数 + 动态参数
     *
     * @param dynamicParamses 多次处理的动态参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @return
     */
    fun buildBatchParamses(dynamicParamses: List<Any?>, paramSize: Int):List<Any?>;

    /**
     * 预览sql
     *
     * @param params 动态参数
     * @param fromIndex 动态参数的开始索引
     * @return
     */
    fun previewSql(dynamicParams:List<Any?> = emptyList(), fromIndex:Int = 0): String
}