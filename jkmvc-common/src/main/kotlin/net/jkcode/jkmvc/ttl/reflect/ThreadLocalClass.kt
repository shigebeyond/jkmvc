package net.jkcode.jkmvc.ttl.reflect

import net.jkcode.jkmvc.common.getAccessibleMethod

/**
 * ThreadLocal类
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-17 10:29 AM
 */
object ThreadLocalClass {

    /**
     * ThreadLocal 类
     */
    public val clazz = ThreadLocal::class.java

    /**
     * ThreadLocal.createMap()私有方法的引用
     */
    public val createMapMethod = clazz.getAccessibleMethod("createMap", Thread::class.java, Any::class.java)

}