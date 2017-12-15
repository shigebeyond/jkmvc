package com.jkmvc.serialize

import java.io.InputStream

/**
 * 序列器
 *
 * @author shijianhang
 * @create 2017-10-04 下午3:29
 **/
interface ISerializer {

    companion object{

        /**
         * 根据类型来获得序列化器
         *
         * @param type
         * @return
         */
        public fun instance(type: String): ISerializer {
            return SerializeType.valueOf(type).serializer
        }
    }

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

    /**
     * 反序列化
     *
     * @param input
     * @return
     */
    fun unserizlize(input: InputStream): Any?
}