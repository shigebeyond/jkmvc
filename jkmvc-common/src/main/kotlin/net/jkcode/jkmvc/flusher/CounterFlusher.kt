package net.jkcode.jkmvc.flusher

import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 定时刷盘 + 定量刷盘
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-17 8:27 AM
 */
abstract class CounterFlusher(
        protected val flushSize: Int /* 触发刷盘的计数大小 */,
        flushTimeoutMillis: Long /* 触发刷盘的定时时间 */) : PeriodicFlusher(flushTimeoutMillis) {

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
     * 请求是否为空
     * @return
     */
    public override fun isRequestEmpty(): Boolean {
        val i = if(switch.get()) 1 else 0
        return counters[i].get() == 0L
    }

    /**
     * 添加计数
     * @param num
     * @return
     */
    public fun add(num: Long): CompletableFuture<Unit> {
        // 1 添加计数
        val i = if(switch.get()) 1 else 0
        if(counters[i].addAndGet(num) > flushSize)
            flush(false)

        // 2 空 -> 非空: 启动定时
        touchTimer()

        // 3 定量刷盘
        if(counters[i].get() >= flushSize)
            flush(false)

        return futures[i]
    }

    /**
     * 将积累的请求刷掉
     * @param byTimeout 是否定时触发 or 定量触发
     * @param timerCallback 定时刷盘的回调, 会调用 startTimer() 来继续下一轮定时
     */
    public override fun flush(byTimeout: Boolean, timerCallback: (() -> Unit)?) {
        val oldSwitch = switch.get()
        val oldI = if(oldSwitch) 1 else 0
        // 切换开关
        if(switch.compareAndSet(oldSwitch, !oldSwitch)){
            val oldFuture = futures[oldI]
            futures[oldI] = CompletableFuture() // 换一个新的future

            val oldCounter = counters[oldI]
            oldCounter.set(0) // 换一个新的计数

            // 执行flush
            handleFlush()
            oldFuture.complete(null)
        }

        // 调用回调
        timerCallback?.invoke()
    }

    /**
     * 处理刷盘
     */
    protected abstract fun handleFlush(): Boolean

}