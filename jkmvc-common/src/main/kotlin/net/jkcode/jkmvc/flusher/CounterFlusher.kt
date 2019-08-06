package net.jkcode.jkmvc.flusher

import net.jkcode.jkmvc.common.VoidFuture
import net.jkcode.jkmvc.common.trySupplierFuture
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

/**
 * 计数来刷盘
 *    定时刷盘 + 定量刷盘
 *    注意: 1 doFlush()直接换新的计数, 而处理旧的计数
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-17 8:27 AM
 */
abstract class CounterFlusher(
        flushQuota: Int, // 触发刷盘的计数大小
        flushTimeoutMillis: Long // 触发刷盘的定时时间
) : ITimeFlusher<Int, Unit>(flushQuota, flushTimeoutMillis) {

    /**
     * 2个future来轮换
     */
    protected val futures: Array<CompletableFuture<Unit>> = arrayOf(CompletableFuture<Unit>(), CompletableFuture<Unit>())

    /**
     * 2个计数器来轮换
     */
    protected val counters: Array<AtomicInteger> = arrayOf(AtomicInteger(0), AtomicInteger(0))

    /**
     * 获得请求计数
     * @return
     */
    public override fun requestCount(): Int {
        val index = currIndex()
        return counters[index].get()
    }

    /**
     * 添加计数
     * @param num
     * @return
     */
    public override fun add(num: Int): CompletableFuture<Unit> {
        val index = currIndex()
        val f = futures[index]
        // 添加计数 + 定量刷盘
        tryFlushWhenAdd(counters[index].addAndGet(num))
        return f
    }

    /**
     * 处理旧索引的请求
     * @param oldIndex 旧的索引, 因为新的索引已切换, 现在要处理旧的索引的数据
     */
    protected override fun doFlush(oldIndex: Int) {
        val oldFuture = futures[oldIndex]
        futures[oldIndex] = CompletableFuture() // 换一个新的future

        val oldCounter = counters[oldIndex]
        val oldCount = oldCounter.get()
        oldCounter.set(0) // 换一个新的计数

        // 无请求要处理
        if(oldCount <= 0) {
            // 设置异步结果
            oldFuture.complete(null)
            return
        }

        // 处理刷盘的请求
        trySupplierFuture {
            handleRequests(oldCount)
        }.whenComplete { r, ex ->
            // 设置异步结果
            if(ex == null)
                oldFuture.complete(null)
            else
                oldFuture.completeExceptionally(ex)
        }
    }

    /**
     * 处理刷盘的请求
     * @param reqCount 请求计数
     * @return
     */
    protected abstract fun handleRequests(reqCount: Int): CompletableFuture<Void>

}