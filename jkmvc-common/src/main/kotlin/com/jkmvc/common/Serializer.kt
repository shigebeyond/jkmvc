package com.jkmvc.common

import java.io.*

/**
 * 序列化
 *
 * @author shijianhang
 * @create 2017-10-04 下午3:29
 **/
object Serializer : ISerializer {
    /**
     * 序列化
     *
     * @param obj
     * @return
     */
    public override fun serialize(obj: Any): ByteArray? {
        try {
            val bo = ByteArrayOutputStream()
            val oo = ObjectOutputStream(bo)
            oo.writeObject(obj)
            return bo.toByteArray()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * 反序列化
     *
     * @param bytes
     * @return
     */
    public override fun unserizlize(bytes: ByteArray): Any? {
        try {
            val bi = ByteArrayInputStream(bytes)
            val oi = ObjectInputStream(bi)
            return oi.readObject()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}