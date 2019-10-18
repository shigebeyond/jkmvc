package net.jkcode.jkmvc.tests

import com.caucho.hessian.client.HessianProxyFactory
import net.jkcode.jkmvc.serialize.ISerializer
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * 测试序列化
 * @author shijianhang<772910474@qq.com>
 * @date 2019-10-17 8:43 PM
 */
class SerializeTests {

    @Test
    fun testSerialize(){
        //val obj = "hello world"
        //val obj = LongArray(3)
        //val obj = BitSet.valueOf(words)
        val obj = BitSet()
        obj.set(100)
        println(obj)
        val instance = ISerializer.instance("fst")
        val bs = instance.serialize(obj)
        if(bs != null) {
            val obj2 = instance.unserialize(bs!!)
            println(obj2)
        }
    }

    val factory = HessianProxyFactory().also { it.setHessian2Request(true) }

    val file = "/home/shi/test/hessian/java.bin"

    @Test
    fun testHessianWrite(){
        var bs = ByteArrayOutputStream()
        val out = factory.getHessianOutput(bs)
        out.call("method1", arrayOf("param1", 2, "param3"))
        File(file).writeBytes(bs.toByteArray())
        println("done")
    }

    @Test
    fun testHessianRead(){
        var fs = FileInputStream(file)
        val `in` = factory.getHessianInput(fs)
        `in`.startCall()
        println(`in`.method)
    }

}