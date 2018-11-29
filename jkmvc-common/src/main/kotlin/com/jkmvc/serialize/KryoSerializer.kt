package com.jkmvc.serialize

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import org.nustaq.serialization.FSTConfiguration
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * 基于Kryo的序列化
 *
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-11-10 4:18 PM
 */
class KryoSerializer: ISerializer {

    /**
     * 序列化
     *
     * @param obj
     * @return
     */
    public override fun serialize(obj: Any): ByteArray? {
        try {
            val kryo = Kryo()
            val output = Output(4096, 4096)
            kryo.writeClassAndObject(output, obj)
            return output.toBytes()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * 反序列化
     *
     * @param input
     * @return
     */
    public override fun unserizlize(input: InputStream): Any? {
        try {
            val kryo = Kryo()
            val input = Input(input)
            return kryo.readClassAndObject(input)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

}