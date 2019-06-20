package net.jkcode.jkmvc.common

import io.netty.util.concurrent.EventExecutor
import io.netty.util.concurrent.MultithreadEventExecutorGroup
import io.netty.util.concurrent.SingleThreadEventExecutor
import net.jkcode.jkmvc.closing.ClosingOnShutdown
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField

/**
 * 公共的线程池
 *   执行任务时要处理好异常
 */
public val CommonThreadPool: ExecutorService =
        ThreadLocalInheritableThreadPool.commonPool
        //ForkJoinPool.commonPool()
        //Executors.newFixedThreadPool(8)

/**
 * 关闭线程池
 */
public val threadPoolCloser = object: ClosingOnShutdown(){
    override fun close() {
        println("-- 关闭线程池, 并等待任务完成 --")
        // 停止工作线程: 不接收新任务
        CommonThreadPool.shutdown()

        // 等待任务完成
        CommonThreadPool.awaitTermination(1, TimeUnit.DAYS) // 等长一点 = 死等
    }

}

/**
 * 单个线程的启动+等待
 * @param join 是否等待线程结束
 * @return
 */
public fun Thread.start(join: Boolean = true): Thread {
    start()
    if(join)
        join()
    return this
}

/**
 * 多个个线程的启动+等待
 * @param join 是否等待线程结束
 * @return
 */
public fun List<Thread>.start(join: Boolean = true): List<Thread> {
    for(t in this)
        t.start()
    if(join)
        for(t in this)
            t.join()
    return this
}

/**
 * 创建线程
 * @param num 线程数
 * @param join 是否等待线程结束
 * @param runnable 线程体
 * @return
 */
public fun makeThreads(num: Int, join: Boolean = true, runnable: (Int) -> Unit): List<Thread> {
    return (0 until num).map { i ->
        Thread({
            runnable.invoke(i)
        }, "test-thread_$i")
    }.start(join)
}

/**
 * 创建线程
 * @param num 线程数
 * @param join 是否等待线程结束
 * @param runnable 线程体
 * @return
 */
public fun makeThreads(num: Int, runnable: (Int) -> Unit): List<Thread>{
    return makeThreads(num, true, runnable)
}

/****************************** 每个线程有独立任务队列 的线程池 *****************************/
/**
 * MultithreadEventExecutorGroup.children 属性
 */
private val childrenProp: KProperty1<MultithreadEventExecutorGroup, Array<EventExecutor>> by lazy{
    val prop = MultithreadEventExecutorGroup::class.getProperty("children") as KProperty1<MultithreadEventExecutorGroup, Array<EventExecutor>>
    prop.javaField!!.isAccessible = true
    prop
}

// 获得子执行器个数: executorGroup.executorCount()
// 使用某个子子执行器来执行任务: executorGroup.getExecutor(i).execute(runnable)
/**
 * 获得某个子执行器(单线程)
 * @param index 子执行器下标
 * @return
 */
public fun MultithreadEventExecutorGroup.getExecutor(index: Int): SingleThreadEventExecutor {
    val children: Array<EventExecutor> = childrenProp.get(this)
    return children.get(index) as SingleThreadEventExecutor
}

/**
 * 根据 arg 来选择一个固定的线程
 * @param arg
 * @return
 */
public fun MultithreadEventExecutorGroup.selectExecutor(arg: Any): SingleThreadEventExecutor {
    return selectExecutor(arg.hashCode())
}

/**
 * 根据 arg 来选择一个固定的线程
 * @param arg
 * @return
 */
public fun MultithreadEventExecutorGroup.selectExecutor(arg: Int): SingleThreadEventExecutor {
    return getExecutor(arg % executorCount())
}