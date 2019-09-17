package java.lang

import net.jkcode.jkmvc.common.getAccessibleMethod

/**
 * ThreadLocal扩展
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-17 10:29 AM
 */
class JkThreadLocal<T>(public val supplier: ()->T) : ThreadLocal<T>() {

    /**
     * ThreadLocalMap.set()私有方法的引用
     */
    public val setMethod = ThreadLocal.ThreadLocalMap::class.java.getAccessibleMethod("set", ThreadLocal::class.java, Any::class.java)

    /**
     * ThreadLocalMap.getEntry()包内方法的引用
     */
    public val getEntryMethod = ThreadLocal.ThreadLocalMap::class.java.getAccessibleMethod("getEntry", ThreadLocal::class.java)

    /**
     * ThreadLocalMap.remove()包内方法的引用
     */
    public val removeMethod = ThreadLocal.ThreadLocalMap::class.java.getAccessibleMethod("remove", ThreadLocal::class.java)

    /**
     * 获得初始值
     */
    public override fun initialValue(): T {
        return supplier.invoke()
    }

    /**
     * 设置线程的值
     *
     * @param t 线程
     * @param value
     */
    public fun set(t: Thread, value: T) {
        val map = getMap(t)
        if (map != null)
            //map.set(this, value)
            setMethod.invoke(map, this, value)
        else
            createMap(t, value)
    }

    /**
     * 获得线程的值
     *   1. 父类的 get(), 相当于本方法的调用 get(Thread.currentThread())
     *   2. 如果没有值不会初始化的
     *
     * @param t 线程
     * @return
     */
    public fun get(t: Thread): T? {
        // 获得线程自有的 ThreadLocalMap
        val map: ThreadLocal.ThreadLocalMap = t.threadLocals
        if (map == null)
            return null

        // 获得对应的值
        // val e = m.getEntry(this)
        val e = getEntryMethod.invoke(map, this) as ThreadLocal.ThreadLocalMap.Entry
        return e?.value as T?
    }

    /**
     * 删除线程的值
     * @param t 线程
     * @param value 要删除的值, 匹配了才能删除, 因为该值可能被多个线程引用
     */
    public fun remove(t: Thread, value: T?){
        if(value == null)
            return

        // 获得线程自有的 ThreadLocalMap
        val map: ThreadLocal.ThreadLocalMap = t.threadLocals
        if (map != null) {
            // 删除对应的值
            // val e = m.getEntry(this)
            val e = getEntryMethod.invoke(map, this) as ThreadLocal.ThreadLocalMap.Entry
            if(e != null && e.value == value)  // 值没有改变
                //m.remove(this)
                removeMethod.invoke(map, this)
        }
    }

}