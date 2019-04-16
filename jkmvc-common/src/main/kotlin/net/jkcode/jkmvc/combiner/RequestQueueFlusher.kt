package net.jkcode.jkmvc.combiner

import io.netty.util.Timeout
import io.netty.util.TimerTask
import net.jkcode.jkmvc.common.CommonMilliTimer
import net.jkcode.jkmvc.common.CommonThreadPool
import net.jkcode.jkmvc.common.drainTo
import net.jkcode.jkmvc.common.getSuperClassGenricType
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 请求队列刷盘器
 *    定时刷盘 + 定量刷盘
 *    注意: 使用 ConcurrentLinkedQueue 来做队列, 其 size() 是遍历性能慢, 尽量使用 isEmpty()
 *
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2019-02-12 5:52 PM
 */
abstract class RequestQueueFlusher<RequestArgumentType, ResponseType> (
        protected val flushTimeoutMillis: Long /* 触发刷盘的定时时间 */,
        protected val flushSize: Int /* 触发刷盘的队列大小 */
) {
    /**
     * 请求队列
     *   单个请求 = 请求参数 + 异步响应
     */
    protected val reqQueue: ConcurrentLinkedQueue<Pair<RequestArgumentType, CompletableFuture<ResponseType>>> = ConcurrentLinkedQueue()

    /**
     * 全局共享的可复用的用于存储临时请求的 List 对象
     */
    protected val tmpReqs:ThreadLocal<ArrayList<Pair<RequestArgumentType, CompletableFuture<ResponseType>>>> = ThreadLocal.withInitial {
        ArrayList<Pair<RequestArgumentType, CompletableFuture<ResponseType>>>()
    }

    /**
     * 状态: 0: 已停止 / 非0: 进行中
     */
    protected val state: AtomicInteger = AtomicInteger(0)

    /**
     * 启动刷盘的定时任务
     */
    protected fun start(){
        CommonMilliTimer.newTimeout(object : TimerTask {
            override fun run(timeout: Timeout) {
                // 刷盘
                flush(){
                    // 空: 停止启动定时
                    if(reqQueue.isEmpty() && state.decrementAndGet() == 0)
                        return@flush

                    // 非空: 继续启动定时
                    start()
                }
            }
        }, flushTimeoutMillis, TimeUnit.MILLISECONDS)
    }

    /**
     * 单个请求入队
     * @param arg
     * @return 返回异步响应, 如果入队失败, 则返回null
     */
    public fun add(arg: RequestArgumentType): CompletableFuture<ResponseType>? {
        if((state.get() == 0 || reqQueue.isEmpty()) && state.getAndIncrement() == 0) // 空 -> 非空: 启动定时
            start()

        val resFuture = CompletableFuture<ResponseType>()
        val result = reqQueue.offer(arg to resFuture)
        if(!result)
            return null

        if(reqQueue.size >= flushSize) // 定量刷盘
            flush(false)
        return resFuture
    }

    /**
     * 将队列中的请求刷掉
     * @param byTimeout 是否定时触发 or 定量触发
     */
    protected fun flush(byTimeout: Boolean = true, callback: (()->Unit)? = null){
        CommonThreadPool.execute{
            val reqs = tmpReqs.get()
            try {
                println((if(byTimeout) "定时触发" else "定量触发") + "flush()")

                while(reqQueue.isNotEmpty()) {
                    // 取出请求
                    val num = reqQueue.drainTo(reqs, flushSize)
                    println("出队个数: $num")

                    // 处理刷盘
                    val args = reqs.map { it.first } // 收集请求参数
                    handleFlush(args, reqs)

                    // 如果 ResponseType == Void, 则框架帮设置异步响应
                    if (this.javaClass.getSuperClassGenricType(1) == Void::class.java)
                        reqs.forEach {
                            it.second.complete(null)
                        }
                }

                // 调用回调
                callback?.invoke()
            }catch (e: Exception){
                e.printStackTrace()
            }finally {
                reqs.clear()
            }
        }
    }

    /**
     * 处理刷盘的请求
     *     如果 ResponseType != Void, 则需要你主动设置异步响应
     * @param args
     * @param reqs
     */
    protected abstract fun handleFlush(args: List<RequestArgumentType>, reqs: ArrayList<Pair<RequestArgumentType, CompletableFuture<ResponseType>>>)

}