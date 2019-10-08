package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.common.generateId
import net.jkcode.jkmvc.common.tryClone
import org.junit.Test

/**
 * 测试克隆
 * @author shijianhang<772910474@qq.com>
 * @date 2019-10-05 5:29 PM
 */
class CloneTest {

    @Test
    fun testClone(){
        val m = Man("shi", 1)
        println(m)
        val m2 = m.tryClone() as Man
        m2.age = m2.age + 1
        println(m2)
        // 默认的clone实现(Object.clone())会拷贝所有属性, 但是不会调用构造方法, 因此id也会相等, 如果要不等, 必须自己改写 clone() 方法
        println(m == m2)
        println(m.id == m2.id)
    }

    @Test
    fun testDeepClone(){
        val master = Man("shi", 21)
        val members = listOf(Man("li", 1))
        val family = Family(master, members)
        println(family)

        // 浅克隆
        println("浅克隆")
        val family2 = family.clone() as Family
        println(family2)
        println(family.master === family2.master) //true
        println(family.members === family2.members) //true

        // 深克隆
        println("深克隆")
        family.deepClone = true
        val family3 = family.clone() as Family
        println(family3)
        println(family.master === family3.master) // false
        println(family.members === family3.members) // false
        println(family.members[0] === family3.members[0]) // true
    }
}