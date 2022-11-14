package net.jkcode.jkmvc.tests

import net.jkcode.jphp.ext.JphpLauncher
import org.junit.Test

class JphpTests {

    @Test
    fun testJphpLauncher(){
        val lan = JphpLauncher
        val ret = lan.run("src/test/resources/orm.php")
        println("----> $ret")
    }
}