package net.jkcode.jkmvc.flusher

import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 计数来刷盘
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-17 8:27 AM
 */
abstract class CounterFlusher(
        protected val flushSize: Int /* 触发刷盘的计数大小 */,
        flushTimeoutMillis: Long /* 触发刷盘的定时时间 */) : PeriodicFlusher<Int, Unit>(flushTimeoutMillis) {

    /**
     * 开关, 2值的轮换
     */
    protected val switch = AtomicBoolean(false)

    /**
     * 2个future来轮换
     */
    protected val futures = arrayOf(CompletableFuture<Unit>(), CompletableFuture<Unit>())

    /**
     * 2个计数器来轮换
     */
    protected val counters = arrayOf(AtomicLong(0), AtomicLong(0))

    /**
     * 获得当前索引
     * @return
     */
    protected fun currIndex(): Int {
        return if (switch.get()) 1 else 0
    }

    /**
     * 获得请求计数
     * @return
     */
    public fun requestCount(): Long {
        val i = currIndex()
        return counters[i].get()
    }

    /**
     * 请求是否为空
     * @return
     */
    public override fun isRequestEmpty(): Boolean {
        return requestCount() == 0L
    }

    /**
     * 添加计数
     * @param num
     * @return
     */
    public override fun add(num: Int): CompletableFuture<Unit> {
        // 1 添加计数 + 定量刷盘
        val i = currIndex()
        val f = futures[i]
        if(counters[i].addAndGet(num.toLong()) >= flushSize)
            flush(false)

        // 2 空 -> 非空: 启动定时
        touchTimer()
        return f
    }

    /**
     * 添加多个计数
     *
     * @param args
     * @return 返回异步响应, 如果入队失败, 则返回null
     */
    public override fun addAll(args: List<Int>): CompletableFuture<Unit>{
        return add(args.sum())
    }

    /**
     * 将积累的请求刷掉
     * @param byTimeout 是否定时触发 or 定量触发
     * @param timerCallback 定时刷盘的回调, 会调用 startTimer() 来继续下一轮定时
     */
    public override fun flush(byTimeout: Boolean, timerCallback: (() -> Unit)?) {
        val oldSwitch = switch.get()
        val oldI = currIndex()
        // 切换开关
        if(switch.compareAndSet(oldSwitch, !oldSwitch)){
            //println("CounterFlusher.flush() : switch from [$oldSwitch] to [${!oldSwitch}]")
            val oldFuture = futures[oldI]
            futures[oldI] = CompletableFuture() // 换一个新的future

            val oldCounter = counters[oldI]
            val oldCount = oldCounter.get()
            oldCounter.set(0) // 换一个新的计数

            // 执行flush
            handleFlush(oldCount)

            // future完成
            oldFuture.complete(null)
        }

        // 调用回调
        timerCallback?.invoke()
    }

    /**
     * 处理刷盘
     * @param reqCount 请求计数
     * @return 是否处理完毕, 同步处理返回true, 异步处理返回false
     */
    protected abstract fun handleFlush(reqCount: Long): Boolean

}