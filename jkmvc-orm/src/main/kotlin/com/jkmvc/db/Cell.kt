package com.jkmvc.db

/**
 * 封装一个单元格的值, 主要用于查询单个值的结果封装
 * @author shijianhang<772910474@qq.com>
 * @date 2018-12-17 3:38 PM
 */
data class Cell<T>(val hasNext: Boolean, val value: T?) {

    /**
     * 获得值
     * @return
     */
    public fun get(): T? {
        return if(hasNext) value else null
    }

    /**
     * 获得值, 无则返回默认值
     * @param default
     * @return
     */
    public fun getOrDefault(default:T): T{
        return get() ?: default
    }

    /**
     * 获得值, 无则抛出异常
     * @param default
     * @return
     */
    public fun getOrThrow(ex: () -> Exception): T{
        return get() ?: throw ex()
    }

}