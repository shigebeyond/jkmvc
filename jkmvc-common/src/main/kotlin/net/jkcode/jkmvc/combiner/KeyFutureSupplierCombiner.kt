package net.jkcode.jkmvc.combiner

import net.jkcode.jkmvc.common.getOrPutOnce
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 异步结果的持有者
 */
class FutureHolder<RequestArgumentType /* 请求参数类型 */, ResponseType /* 响应类型 */>(public val key: RequestArgumentType /* 请求参数 */ ){
    /**
     * 等待数
     */
    protected val waitNum: AtomicInteger = AtomicInteger(0)

    /**
     * 异步结果
     */
    protected var future: CompletableFuture<ResponseType>? = null

    init {
        // println("----- create FutureHolder")
    }

    /**
     * 异步调用取值操作, 但只调用一次
     *    只有第一个线程才能异步调用, 并缓存唯一的异步结果, 叫源异步结果
     *    所有线程均使用源异步结果, 但由于每个线程有自己的副作用(如在消费完后删除源异步结果), 因此每个线程只是使用源异步结果的克隆
     *
     * @param futureSupplier 取值操作, 如果返回值类型是 CompletableFuture, 则直接调用作为源异步结果
     * @return
     */
    public fun supplyAsyncOnce(futureSupplier: (RequestArgumentType) -> CompletableFuture<ResponseType>): CompletableFuture<ResponseType>{
        // 增加等待数, 防止结果被删除
        if (waitNum.getAndIncrement() > 0 && future != null)
            return cloneThenDeleteSrcFuture()

        // 加锁来异步调用取值操作, 并缓存异步结果
        synchronized(this) {
            // 第一个线程才异步调用取值操作
            if (future == null) {
                // println("----- first future")
                future = futureSupplier.invoke(key)
            }
        }

        // 其他线程直接使用异步结果
        return cloneThenDeleteSrcFuture()
    }

    /**
     * 克隆源异步结果, 在消费完后删除
     * @return
     */
    protected fun cloneThenDeleteSrcFuture(): CompletableFuture<ResponseType> {
        return future!!.thenApplyAsync {
            // println("----- then")
            // 减少等待数, 同时在等待数为0(所有线程消费完)后, 删除异步结果, 以便下一个轮回
            if (waitNum.decrementAndGet() == 0)
                future = null
            it
        }
    }
}

/**
 * 针对每个key的取值操作合并, 也等同于取值操作去重
 *    如请求合并/cache合并等
 *    实现就是改进 ConcurrentMap.getOrPutOnce(key, defaultValue), 补上在多个取值操作处理完后删除key, 以便下一个轮回
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-10 9:47 AM
 */
open class KeyFutureSupplierCombiner<RequestArgumentType /* 请求参数类型 */, ResponseType /* 响应类型 */>(
        public val futureSupplier: (RequestArgumentType) -> CompletableFuture<ResponseType> // 取值操作, 其返回值类型是 CompletableFuture, 直接调用作为源异步结果
) {

    /**
     * <键 to 结果>
     */
    protected val resultHolders: ConcurrentHashMap<Any, FutureHolder<RequestArgumentType, ResponseType>> = ConcurrentHashMap()

    /**
     * 针对当前key合并取值操作, 异步
     *
     * @param key
     * @return
     */
    public fun add(key: RequestArgumentType): CompletableFuture<ResponseType> {
        val holder = resultHolders.getOrPutOnce(key){
            FutureHolder(key)
        }

        return holder.supplyAsyncOnce(futureSupplier)
    }
}