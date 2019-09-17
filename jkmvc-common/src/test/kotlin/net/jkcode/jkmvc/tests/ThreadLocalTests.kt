package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.common.getProperty
import net.jkcode.jkmvc.common.makeThreads
import org.junit.Test
import java.util.concurrent.ForkJoinPool
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField

/**
 * 线程测试
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-18 5:36 PM
 */
class ThreadLocalTests {

    /**
     * ThreadLocal
     *   父子互不影响
     */
    @Test
    fun testThreadLocalInChildThread(){
        val msgs = ThreadLocal<String>()

        msgs.set("a")
        println("mainThread: " + msgs.get()) // a
        makeThreads(1) {
            val name = Thread.currentThread().name
            println("childThread($name): " + msgs.get()) // null
            msgs.set("b")
            println("childThread($name): " + msgs.get()) // b
        }
        println("mainThread: " + msgs.get()) // a
    }

    /**
     * InheritableThreadLocal
     *   单向: 父 写 子, 但子 不能写 父
     */
    @Test
    fun testInheritableThreadLocalInChildThread(){
        val msgs = InheritableThreadLocal<String>()

        msgs.set("a")
        println("mainThread: " + msgs.get()) // a
        makeThreads(1) {
            val name = Thread.currentThread().name
            println("childThread($name): " + msgs.get()) // a
            msgs.set("b")
            println("childThread($name): " + msgs.get()) // b
        }
        println("mainThread: " + msgs.get()) // a
    }

    /**
     * 在无关的线程中测试
     */
    @Test
    fun testInheritableThreadLocalInOtherThread(){
        val msgs = InheritableThreadLocal<String>()

        msgs.set("a")
        println("mainThread: " + msgs.get()) // a
        // 线程池的中的线程也是当前线程创建的
        ForkJoinPool.commonPool().execute {
            val name = Thread.currentThread().name
            println("otherThread($name): " + msgs.get()) // a
            msgs.set("b")
            println("otherThread($name): " + msgs.get()) // b

            makeThreads(1) {
                println("childThread($name): " + msgs.get()) // b
                msgs.set("c")
                println("childThread($name): " + msgs.get()) // c
            }
        }
        Thread.sleep(1000)
        println("mainThread: " + msgs.get()) // a
    }

    @Test
    fun testThreadLocalsProp(){
        val msgs = ThreadLocal<String>()
        msgs.set("test")

        // 获得属性
        val prop = Thread::class.getProperty("threadLocals") as KProperty1<Thread, *>
        // 开放访问
        prop.javaField!!.setAccessible(true)
        println(prop is KMutableProperty1<*, *>) // true

        // 读属性
        val thread = Thread.currentThread()
        var map = prop.get(thread)
        println(map)

        // 写属性
        (prop as KMutableProperty1<Thread, Any?>).set(thread, null)
        map = prop.get(thread)
        println(map)
    }

}