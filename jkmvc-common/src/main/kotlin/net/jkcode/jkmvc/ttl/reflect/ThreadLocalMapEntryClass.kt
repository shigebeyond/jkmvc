package net.jkcode.jkmvc.ttl.reflect

import net.jkcode.jkmvc.common.getAccessibleField

/**
 * ThreadLocalMap.Entry类
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-17 10:29 AM
 */
object ThreadLocalMapEntryClass {

    /**
     * ThreadLocalMap.Entry 类
     */
    //public val clazz = ThreadLocalMap::class.java
    public val clazz = Class.forName("java.lang.ThreadLocal\$ThreadLocalMap\$Entry")

    /**
     * ThreadLocalMap.Entry.value 字段
     */
    public val valueField = clazz.getAccessibleField("value")!!
}