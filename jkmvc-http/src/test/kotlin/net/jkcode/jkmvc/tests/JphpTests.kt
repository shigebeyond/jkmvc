package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.http.view.jphp.JphpLauncher
import org.junit.Test
import php.runtime.env.Context
import java.io.File

class JphpTests {

    private val context = Context(File("unknown"))

/*    @Test
    fun testJphp(){
        val e = HashMap::class.java.toClassEntity(context)
        print(e)

    }*/

    @Test
    fun testJphpLauncher(){
        JphpLauncher.run("src/test/resources/index.php", mapOf("name" to "shijianhang"))
    }

}