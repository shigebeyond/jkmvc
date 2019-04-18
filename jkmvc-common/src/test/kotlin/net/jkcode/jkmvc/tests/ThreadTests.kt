package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.common.*
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField

/**
 * 线程测试
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-18 5:36 PM
 */
class ThreadTests {

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
            println("childThread: " + msgs.get()) // null
            msgs.set("b")
            println("childThread: " + msgs.get()) // b
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
            println("childThread: " + msgs.get()) // a
            msgs.set("b")
            println("childThread: " + msgs.get()) // b
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
            println("otherThread: " + msgs.get()) // a
            msgs.set("b")
            println("otherThread: " + msgs.get()) // b

            makeThreads(1) {
                println("childThread: " + msgs.get()) // b
                msgs.set("c")
                println("childThread: " + msgs.get()) // c
            }
        }
        Thread.sleep(1000)
        println("mainThread: " + msgs.get()) // a
    }

    /**
     * 在无关的线程中测试
     */
    @Test
    fun testInheritableThreadLocalInCompletableFuture() {
        // 应用线程池
        ThreadLocalInheritableThreadPool.applyCommonPoolToCompletableFuture()

        val msgs = InheritableThreadLocal<String>()
        msgs.set("0")
        println("mainThread: " + msgs.get()) // 0

        val runid = AtomicInteger(0)
        val step = AtomicInteger(0)
        val run: () -> Unit = {
            val id = runid.getAndIncrement()
            val srcTid = Thread.currentThread().name
            val stepRun = {
                step.incrementAndGet()
                val destTid = Thread.currentThread().name
                println("rid=$id, srcTid=$srcTid, destTid=$destTid, step=$step - before : " + msgs.get()) //
                msgs.set(step.toString())
                println("rid=$id, srcTid=$srcTid, destTid=$destTid, step=$step - after : " + msgs.get()) //
            }
            CompletableFuture.runAsync(stepRun)
                    .thenRunAsync(stepRun)
                    .thenRunAsync(stepRun)
        }
        // 没有经过 ThreadLocalInheritableThreadPool, 是不能继承 ThreadLocal 的
        //makeThreads(3, run)

        CommonThreadPool.execute(run)
        CommonThreadPool.execute(run)
        CommonThreadPool.execute(run)

        Thread.sleep(3000)
        println("mainThread: " + msgs.get()) // 0
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

    /**
     * 可继承ThreadLocal的线程池
     *    与 testInheritableThreadLocalInOtherThread 对比
     */
    @Test
    fun testThreadLocalInheritableThreadPool(){
        val pool = ThreadLocalInheritableThreadPool(ForkJoinPool.commonPool())

        val msgs = InheritableThreadLocal<String>()

        msgs.set("a")
        println("mainThread: " + msgs.get()) // a
        // 线程池的中的线程也是当前线程创建的
        pool.execute {
            println("otherThread: " + msgs.get()) // a
            msgs.set("b")
            println("otherThread: " + msgs.get()) // b

            // 没有经过 ThreadLocalInheritableThreadPool, 是不能继承 ThreadLocal 的
            makeThreads(1) {
                println("childThread: " + msgs.get()) // b
                msgs.set("c")
                println("childThread: " + msgs.get()) // c
            }
        }
        Thread.sleep(1000)
        println("mainThread: " + msgs.get()) // b
    }
}