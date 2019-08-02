package net.jkcode.jkmvc.flusher

import java.util.concurrent.CompletableFuture

/**
 * 无响应值的请求队列刷盘器
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-22 4:24 PM
 */
abstract class UnitRequestQueueFlusher<RequestType /* 请求类型 */>(
        flushSize: Int, // 触发刷盘的计数大小
        flushTimeoutMillis: Long // 触发刷盘的定时时间
): RequestQueueFlusher<RequestType, Unit>(flushSize, flushTimeoutMillis) {

    /**
     * 是否无响应值
     *    即响应值值类型为 Void / Unit, 则框架帮设置异步响应
     */
    protected override val noResponse: Boolean = true

    /**
     * 多个请求入队
     * @param reqs
     * @return
     */
    public fun addAll(reqs: List<RequestType>): CompletableFuture<Unit> {
        val index = currIndex()
        val queue = queues[index]

        // 添加请求
        val resFuture = CompletableFuture<Unit>()
        for(arg in reqs)
            queue.offer(arg to resFuture) // 多个请求使用同一个future, 因为future.complete(result)是幂等的, 多次调用但实际只完成一次

        // 尝试定量刷盘
        tryFlushWhenAdd(queue.size)
        return resFuture
    }

}

