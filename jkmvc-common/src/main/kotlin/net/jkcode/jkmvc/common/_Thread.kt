package net.jkcode.jkmvc.common

import net.jkcode.jkmvc.closing.ClosingOnShutdown
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit

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
 * @return
 */
public fun Thread.startAndJoin(): Thread {
    start()
    join()
    return this
}

/**
 * 多个个线程的启动+等待
 * @return
 */
public fun List<Thread>.startAndJoin(): List<Thread> {
    for(t in this)
        t.start()
    for(t in this)
        t.join()
    return this
}

/**
 * 创建线程
 * @param num 线程数
 * @param runnable 线程体
 * @return
 */
public fun makeThreads(num: Int, runnable: () -> Unit): List<Thread> {
    return (0 until num).map { Thread(runnable, "thread_$it") }.startAndJoin()
}