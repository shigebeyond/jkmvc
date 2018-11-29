package com.jkmvc.serialize

import com.dyuproject.protostuff.LinkedBuffer
import com.dyuproject.protostuff.ProtostuffIOUtil
import com.dyuproject.protostuff.Schema
import com.dyuproject.protostuff.runtime.RuntimeSchema
import org.objenesis.ObjenesisStd
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap


/**
 * 基于Protostuff的序列化
 *
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-11-10 4:18 PM
 */
class ProtostuffSerializer: ISerializer {

    private val objenesis = ObjenesisStd(true)

    private val schemaCache = ConcurrentHashMap<Class<*>, Schema<*>>()

    private fun getSchma(clazz: Class<Any>): Schema<Any> {
        return schemaCache.getOrPut(clazz){
            RuntimeSchema.createFrom(clazz)
        } as Schema<Any>
    }

    /**
     * 序列化
     *
     * @param obj
     * @return
     */
    public override fun serialize(obj: Any): ByteArray? {
        val buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE)//使用LinkedBuffer分配一块默认大小的buffer空间
        try {
            val clazz = obj.javaClass
            val schema = getSchma(clazz)
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer)//使用给定的schema将对象序列化为一个byte数组，并返回
        } finally {
            buffer.clear()
        }
    }

    /**
     * 反序列化
     *
     * @param bytes
     * @return
     */
    public override fun unserizlize(bytes: ByteArray): Any? {
        val clazz = Any::class.java
        val obj:Any = objenesis.newInstance(clazz)
        val schma = getSchma(clazz)
        ProtostuffIOUtil.mergeFrom(bytes, obj, schma)
        return obj
    }

    /**
     * 反序列化
     *
     * @param input
     * @return
     */
    public override fun unserizlize(input: InputStream): Any? {
        val clazz = Any::class.java
        val obj:Any = objenesis.newInstance(clazz)
        val schma = getSchma(clazz)
        ProtostuffIOUtil.mergeFrom(input, obj, schma)
        return obj
    }

}