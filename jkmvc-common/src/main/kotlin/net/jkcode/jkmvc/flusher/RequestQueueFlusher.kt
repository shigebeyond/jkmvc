package net.jkcode.jkmvc.flusher

import net.jkcode.jkmvc.common.CommonThreadPool
import net.jkcode.jkmvc.common.drainTo
import net.jkcode.jkmvc.common.getSuperClassGenricType
import net.jkcode.jkmvc.common.runAsync
import net.jkcode.jkmvc.lock.AtomicLock
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 请求队列刷盘器
 *    定时刷盘 + 定量刷盘
 *    注意: 1 使用 ConcurrentLinkedQueue 来做队列, 其 size() 是遍历性能慢, 尽量使用 isEmpty()
 *         2 请求出队要线程安全, 直接由 HashedWheelTimer 单线程来调用, 简单
 *         3 请求处理则扔到线程池
 *    TODO: 请求出队不消耗 HashedWheelTimer 的线程
 *          定时与定量同时触发, 并发问题
 *
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2019-02-12 5:52 PM
 */
abstract class RequestQueueFlusher<RequestArgumentType, ResponseType> (
        protected val flushSize: Int /* 触发刷盘的队列大小 */,
        flushTimeoutMillis: Long /* 触发刷盘的定时时间 */
): PeriodicFlusher(flushTimeoutMillis) {

    /**
     * 请求队列
     *   单个请求 = 请求参数 + 异步响应
     */
    protected val reqQueue: ConcurrentLinkedQueue<Pair<RequestArgumentType, CompletableFuture<ResponseType>>> = ConcurrentLinkedQueue()

    /**
     * 限制flush()并发的锁
     */
    protected val lock: AtomicLock = AtomicLock()

    /**
     * 请求是否为空
     * @return
     */
    public override fun isRequestEmpty(): Boolean {
        return reqQueue.isEmpty()
    }

    /**
     * 单个请求入队
     * @param arg
     * @return 返回异步响应, 如果入队失败, 则返回null
     */
    public fun add(arg: RequestArgumentType): CompletableFuture<ResponseType> {
        // 1 添加
        val resFuture = CompletableFuture<ResponseType>()
        reqQueue.offer(arg to resFuture) // 返回都是true

        // 2 空 -> 非空: 启动定时
        touchTimer()

        // 3 定量刷盘
        if(reqQueue.size >= flushSize)
            flush(false)

        return resFuture
    }

    /**
     * 将队列中的请求刷掉
     * @param byTimeout 是否定时触发 or 定量触发
     * @param timerCallback 定时刷盘的回调, 会调用 startTimer() 来继续下一轮定时
     */
    public override fun flush(byTimeout: Boolean, timerCallback: (() -> Unit)?){
        // 1 锁住了: 处理请求
        val locked = lock.quickLockCleanly {
            //val msg = if(byTimeout) "定时刷盘" else "定量刷盘"
            val futures: MutableList<CompletableFuture<*>>? = if(timerCallback == null) null else LinkedList()
            while(reqQueue.isNotEmpty()) {
                // 1.1 请求出队, 要线程安全, 直接由 HashedWheelTimer 单线程来调用, 简单
                val reqs = ArrayList<Pair<RequestArgumentType, CompletableFuture<ResponseType>>>()
                val num = reqQueue.drainTo(reqs, flushSize)
                if(num == 0)
                    break;

                // 1.2 请求处理, 扔到线程池
                val future = CommonThreadPool.runAsync{
                    try {
                        val args = reqs.map { it.first } // 收集请求参数
                        //println("$msg, 出队请求: $num 个, 请求参数为: $args")

                        // 1.2.1 处理刷盘
                        val done = handleFlush(args, reqs)

                        // 1.2.2 在处理完成后, 如果 ResponseType == Void/Unit, 则框架帮设置异步响应, 否则开发者自行在 handleFlush() 中设置
                        if (done) {
                            val responseType = this.javaClass.getSuperClassGenricType(1)
                            if (responseType == Void::class.java || responseType == Unit::class.java)
                                reqs.forEach { (arg, resFuture) ->
                                    resFuture.complete(null)
                                }

                            // 清空请求列表, 释放内存
                            reqs.clear()
                        }
                    }catch (e: Exception){
                        e.printStackTrace()
                    }
                }
                // 记录future, 用于在请求处理完成后调用回调
                futures?.add(future)
            }

            // 1.3 在请求处理完成后调用回调
            if(futures != null && futures.isNotEmpty())
                CompletableFuture.allOf(*futures.toTypedArray()).thenRun {
                    // 调用回调
                    timerCallback?.invoke()
                    futures.clear()
                }

        }

        // 2 锁不住: 如果是定时刷盘, 主动调用回调
        if(!locked && byTimeout == true)
            timerCallback?.invoke()
    }

    /**
     * 处理刷盘的请求
     *     如果 同步 + ResponseType != Void/Unit, 则需要你主动设置异步响应
     * @param args
     * @param reqs
     * @return 是否处理完毕, 同步处理返回true, 异步处理返回false
     */
    protected abstract fun handleFlush(args: List<RequestArgumentType>, reqs: ArrayList<Pair<RequestArgumentType, CompletableFuture<ResponseType>>>): Boolean

}