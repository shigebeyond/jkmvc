package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.http.jphp.JphpLauncher
import net.jkcode.jkutil.common.getAccessibleField
import org.junit.Test
import php.runtime.env.Context
import php.runtime.env.Environment
import net.jkcode.jkmvc.http.jphp.WrapJavaObject
import php.runtime.loader.compile.StandaloneCompiler
import php.runtime.memory.support.MemoryUtils
import java.io.File

class JphpTests {

    private val context = Context(File("unknown"))

    @Test
    fun testJphp(){
        val f = MemoryUtils::class.java.getAccessibleField("CONVERTERS")
        println(f!!.name)
    }

    @Test
    fun testJphpLauncher(){
        val lan = JphpLauncher.instance()
        val data = mapOf(
                "name" to "shijianhang",
                "info" to mapOf("age" to 11, "addr" to "nanning"),
                "javaobj" to WrapJavaObject.of(lan.environment, mapOf("goods_id" to 1, "goods_name" to "火龙果", "quantity" to 13))
        )
        lan.run("src/test/resources/index.php", data)
    }

    @Test
    fun testCompile(){
        // .php源文件目录
        val srcDir = "/home/shi/code/java/jkmvc/jkmvc-http/src/test/resources"
        // .class编译文件的目录
        val destDir = srcDir + "/bin/"
        val env = Environment()
        val compiler = StandaloneCompiler(File(srcDir), env)
        // 编译
        compiler.compile(File(destDir), null)
        println("编译成功")
    }

}