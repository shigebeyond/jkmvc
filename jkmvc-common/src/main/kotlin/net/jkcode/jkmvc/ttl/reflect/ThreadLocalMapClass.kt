package net.jkcode.jkmvc.ttl.reflect

import net.jkcode.jkmvc.common.getAccessibleMethod

/**
 * ThreadLocalMap类
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-17 10:29 AM
 */
object ThreadLocalMapClass {

    /**
     * ThreadLocalMap 类
     */
    //public val clazz = ThreadLocalMap::class.java
    public val clazz = Class.forName("java.lang.ThreadLocal\$ThreadLocalMap")

    /**
     * ThreadLocalMap.set()私有方法的引用
     */
    public val setMethod = clazz.getAccessibleMethod("set", ThreadLocal::class.java, Any::class.java)

    /**
     * ThreadLocalMap.getEntry()包内方法的引用
     */
    public val getEntryMethod = clazz.getAccessibleMethod("getEntry", ThreadLocal::class.java)

    /**
     * ThreadLocalMap.remove()包内方法的引用
     */
    public val removeMethod = clazz.getAccessibleMethod("remove", ThreadLocal::class.java)

}