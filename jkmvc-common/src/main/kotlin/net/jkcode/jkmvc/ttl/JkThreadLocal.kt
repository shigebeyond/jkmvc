//package java.lang
package net.jkcode.jkmvc.ttl

import net.jkcode.jkmvc.ttl.reflect.ThreadClass
import net.jkcode.jkmvc.ttl.reflect.ThreadLocalClass
import net.jkcode.jkmvc.ttl.reflect.ThreadLocalMapClass
import net.jkcode.jkmvc.ttl.reflect.ThreadLocalMapEntryClass

/**
 * ThreadLocal扩展
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-17 10:29 AM
 */
class JkThreadLocal<T>(public val supplier: ()->T) : ThreadLocal<T>() {

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
        //val map: ThreadLocal.ThreadLocalMap = t.threadLocals
        val map = ThreadClass.threadLocalField.get(t)
        if (map != null)
            //map.set(this, value)
            ThreadLocalMapClass.setMethod.invoke(map, this, value)
        else
            //createMap(t, value)
            ThreadLocalClass.createMapMethod.invoke(this, t, value)
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
        //val map: ThreadLocal.ThreadLocalMap = t.threadLocals
        val map = ThreadClass.threadLocalField.get(t)
        if (map == null)
            return null

        // 获得对应的值
        // val e = m.getEntry(this)
        val e = ThreadLocalMapClass.getEntryMethod.invoke(map, this)
        if(e == null)
            return null

        //return e.value as T
        return ThreadLocalMapEntryClass.valueField.get(e) as T
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
        //val map: ThreadLocal.ThreadLocalMap = t.threadLocals
        val map = ThreadClass.threadLocalField.get(t)
        if (map == null)
            return

        // 删除对应的值
        // val e = m.getEntry(this)
        val e = ThreadLocalMapClass.getEntryMethod.invoke(map, this)
        if(e == null)
            return

        // 值没有改变, 则删除
        //if(e.value == value)
        if(ThreadLocalMapEntryClass.valueField.get(e) == value)
            //m.remove(this)
            ThreadLocalMapClass.removeMethod.invoke(map, this)
    }

}