package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.common.*
import net.jkcode.jkmvc.ttl.ScopedTransferableThreadLocal
import net.jkcode.jkmvc.ttl.SttlThreadPool
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
            val name = Thread.currentThread().name
            println("childThread($name): " + msgs.get()) // null
            msgs.set("b")
            println("childThread($name): " + msgs.get()) // b
        }
        println("mainThread: " + msgs.get()) // a
    }

    /**
     * ScopedTransferableThreadLocal
     *   单向: 父 写 子, 但子 不能写 父
     */
    @Test
    fun testScopedTransferableThreadLocalInChildThread(){
        val msgs = ScopedTransferableThreadLocal<String>()

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
    fun testScopedTransferableThreadLocalInOtherThread(){
        val msgs = ScopedTransferableThreadLocal<String>()

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

    /**
     * 测试 CompletableFuture 是否继承 ThreadLocal
     *   发起 runAsync() 与 完成 complete() 都是在 SttlThreadPool.commonPool 触发
     */
    @Test
    fun testScopedTransferableThreadLocalInCompletableFuture() {
        // 修改 CompletableFuture.asyncPool 属性为 SttlThreadPool.commonPool
        // SttlThreadPool.applyCommonPoolToCompletableFuture()

        val msgs = ScopedTransferableThreadLocal<String>()
        msgs.set("0")
        println("mainThread: " + msgs.get()) // 0

        val runid = AtomicInteger(0)
        val step = AtomicInteger(0)
        // 读写 ThreadLocal
        val run: () -> Unit = {
            val id = runid.getAndIncrement()
            val srcTname = Thread.currentThread().name
            val stepRun = {
                step.incrementAndGet()
                val destTname = Thread.currentThread().name
                println("rid=$id, srcTname=$srcTname, destTname=$destTname, step=$step - before : " + msgs.get()) //
                msgs.set(step.toString())
                println("rid=$id, srcTname=$srcTname, destTname=$destTname, step=$step - after : " + msgs.get()) //
            }

            // 完成 complete() 在 SttlThreadPool.commonPool 触发
            CompletableFuture.runAsync(stepRun)
                    .thenRunAsync(stepRun)
                    .thenRunAsync(stepRun)
        }

        // 多线程发起
        // 没有经过 SttlThreadPool, 是不能继承 ThreadLocal 的
        //makeThreads(3, run)

        // 发起 runAsync() 在 SttlThreadPool.commonPool 触发
        CommonThreadPool.execute(run)
        CommonThreadPool.execute(run)
        CommonThreadPool.execute(run)

        Thread.sleep(3000)
        println("mainThread: " + msgs.get()) // 0
    }

    /**
     * 测试 CompletableFuture 是否继承 ThreadLocal
     *    完成 complete() 在 SttlThreadPool.commonPool 之外的其他线程触发
     */
    @Test
    fun testScopedTransferableThreadLocalInCompletableFuture2() {
        // 修改 CompletableFuture.asyncPool 属性为 SttlThreadPool.commonPool
        SttlThreadPool.applyCommonPoolToCompletableFuture()

        val msgs = ThreadLocal<String>()
        msgs.set("0")
        println("mainThread: " + msgs.get()) // 0

        val runid = AtomicInteger(0)
        val step = AtomicInteger(0)
        // 读写 ThreadLocal
        val run: () -> Unit = {
            val id = runid.getAndIncrement()
            step.incrementAndGet()
            val name = Thread.currentThread().name
            println("rid=$id, Tname=$name, step=$step - before : " + msgs.get()) //
            msgs.set(step.toString())
            println("rid=$id, Tname=$name, step=$step - after : " + msgs.get()) //
        }

        //
        val num = 2
        val futures = (0 until num).map{
            val f = CompletableFuture<Unit>() // 当前 ThreadLocal 应该是 msgs = 0, 但是由于 complete() 在其他线程改变了 ThreadLocal 变为 msgs = 1 => 没办法,
                f.thenRunAsync(run)
                //.thenRunAsync(run)
            f
        }

        // 完成 complete() 在其他线程
        // 没有经过 SttlThreadPool, 是不能继承 ThreadLocal 的
        makeThreads(num){i ->
            run()
            futures[i].complete(null)
            val name = Thread.currentThread().name
            println("completeThread($name)")
            //当前 ThreadLocal 变成 msgs = 1
        }

        Thread.sleep(30000)
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
     *    与 testScopedTransferableThreadLocalInOtherThread 对比
     */
    @Test
    fun testSttlThreadPool(){
        val pool = SttlThreadPool(ForkJoinPool.commonPool())

        val msgs = ScopedTransferableThreadLocal<String>()

        msgs.set("a")
        println("mainThread: " + msgs.get()) // a
        // 线程池的中的线程也是当前线程创建的
        pool.execute {
            val name = Thread.currentThread().name
            println("otherThread($name): " + msgs.get()) // a
            msgs.set("b")
            println("otherThread($name): " + msgs.get()) // b

            // 没有经过 SttlThreadPool, 是不能继承 ThreadLocal 的
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