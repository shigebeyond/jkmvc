package net.jkcode.jkmvc.tags.form

import net.jkcode.jkutil.common.getAccessibleField
import net.jkcode.jkutil.common.getProperty
import org.apache.taglibs.standard.tag.common.fmt.MessageSupport
import org.junit.Test

/**
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2020-04-17 10:17 AM
 */
class TagTests {

    @Test
    fun testProp(){
        //val p = MessageTag::class.getProperty("var")
        //val p = org.apache.taglibs.standard.tag.rt.fmt.MessageTag::class.getProperty("var")
        val p = MessageSupport::class.getProperty("var") // 私有属性不能访问
        println(p)
    }

    @Test
    fun testField(){
        val varField = MessageSupport::class.java.getAccessibleField("var")!!
        val scopeField = MessageSupport::class.java.getAccessibleField("scope")!!
        println(varField)
        println(scopeField)
    }
}