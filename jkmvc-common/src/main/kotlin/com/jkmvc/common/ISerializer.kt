package com.jkmvc.common

/**
 * 序列化
 *
 * @author shijianhang
 * @create 2017-10-04 下午3:29
 **/
interface ISerializer {
    /**
     * 序列化
     *
     * @param obj
     * @return
     */
    fun serialize(obj: Any): ByteArray?

    /**
     * 反序列化
     *
     * @param bytes
     * @return
     */
    fun unserizlize(bytes: ByteArray): Any?
}