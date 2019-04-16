package net.jkcode.jkmvc.combiner

import net.jkcode.jkmvc.common.isSuperClass
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 结果的持有者
 */
class ResultHolder{

    /**
     * 等待数
     */
    protected val waitNum: AtomicInteger = AtomicInteger(0)

    /**
     * 结果值
     */
    protected var resultFuture: CompletableFuture<Any>? = null

    /**
     * 异步调用取值操作, 但只调用一次
     *    只有第一个线程才能异步调用, 并缓存唯一的异步结果, 叫源异步结果
     *    所有线程均使用源异步结果, 但由于每个线程有自己的副作用(如在消费完后删除源异步结果), 因此每个线程只是使用源异步结果的克隆
     *
     * @param supplier 取值操作, 如果返回值类型是 CompletableFuture, 则直接调用作为源异步结果
     * @return
     */
    public inline fun <reified T> supplyAsyncOnce(noinline supplier: () -> T): CompletableFuture<T>{
        // 增加等待数, 防止结果被删除
        if (waitNum.getAndIncrement() > 0 && resultFuture != null)
            return cloneThenDeleteSrcFuture()

        // 加锁来异步调用取值操作, 并缓存异步结果
        return synchronized(this) {
            // 第一个线程才异步调用取值操作
            if (resultFuture == null) {
                if (CompletableFuture::class.java.isSuperClass(T::class.java)) // 如果异步取值操作, 主要是针对rpc的取值操作, 因为rpc调用是异步的, 调用完不是返回结果值, 而是返回future
                    resultFuture = supplier.invoke() as CompletableFuture<Any>
                else
                    resultFuture = CompletableFuture.supplyAsync(supplier)
            }
            // 其他线程直接使用异步结果
            cloneThenDeleteSrcFuture()
        }
    }

    /**
     * 克隆源异步结果, 在消费完后删除
     * @return
     */
    protected fun <T> cloneThenDeleteSrcFuture(): CompletableFuture<T> {
        return resultFuture!!.thenApply {
            // 减少等待数, 同时在等待数为0(所有线程消费完)后, 删除异步结果, 以便下一个轮回
            if (waitNum.decrementAndGet() == 0)
                resultFuture = null
            it as T
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
class KeySupplierCombiner {

    /**
     * <键 to 结果>
     */
    protected val resultHolders: ConcurrentHashMap<Any, ResultHolder> = ConcurrentHashMap()

    /**
     * 针对当前key合并取值操作, 异步
     *
     * @param key
     * @param supplier 取值操作, 如果返回值类型是 CompletableFuture, 则直接调用作为源异步结果
     * @return
     */
    public inline fun <reified T> add(key: Any, noinline supplier: () -> T): CompletableFuture<T> {
        val holder = resultHolders.getOrPut(key){
            ResultHolder()
        }

        return holder.supplyAsyncOnce(supplier)
    }
}