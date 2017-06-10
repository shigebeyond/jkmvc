package com.jkmvc.db

/**
 * sql编译结果
 *
 * @author shijianhang
 * @date 2017-6-10 下午8:02:47
 */
interface ISqlCompiledResult {
    /**
     * 编译好的sql
     */
    var sql: String
    /**
     *  实际参数 = 静态参数 + 动态参数
     */
    val params: List<Any?>

    /**
     * 判断是否为空
     */
    fun isEmpty(): Boolean

    /**
     * 清空编译结果
     */
    fun clear(): SqlCompiledResult

    /**
     * 预览sql
     * @return
     */
    fun previewSql(): String
}