package net.jkcode.jkmvc.ttl

import net.jkcode.jkmvc.common.getWritableFinalField
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool

/**
 * 可传递ScopedTransferableThreadLocal的线程池, 只传递ScopedTransferableThreadLocal, 无关ThreadLocal
 *   特性: worker thread 在执行任务时, 会传递 caller thread的 ScopedTransferableThreadLocal数据
 *   目标: 主要是为了解决异步执行时, 线程状态(ScopedTransferableThreadLocal)的传递问题, 如 jkmvc 将当前 Db/HttpRequest 等对象都是记录到 ScopedTransferableThreadLocal对象中, 以方便访问, 但是一旦异步执行后就丢失了
 *   实现: 改写 execute() 方法, 在执行之前传递一下 ScopedTransferableThreadLocal对象, 在执行后就恢复一下 ScopedTransferableThreadLocal对象
 *   优化: 所有 SttlInterceptor.intercept()方法的 caller thread 的 ScopedTransferableThreadLocal对象引用都是使用 `ScopedTransferableThreadLocal.weakCopyLocal2Value()`, 为 `WeakHashMap`, GC会回收, 但不频繁, 适用于短时间引用
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-18 4:28 PM
 */
class SttlThreadPool(protected val pool: ExecutorService): ExecutorService by pool {

    companion object {

        // 公共的线程池: 包装 ForkJoinPool.commonPool()
        public val commonPool by lazy {
            SttlThreadPool(ForkJoinPool.commonPool())
        }

        // -------------------------------------------
        // CompletableFuture的线程池: 包装 ForkJoinPool.commonPool()
        public val completableFuturePool by lazy {
            SttlThreadPool(ForkJoinPool.commonPool())
        }

        // CompletableFuture.asyncPool 属性
        //protected val asyncPoolProp = CompletableFuture::class.getStaticProperty("asyncPool") as KMutableProperty0<Any?>
        protected val asyncPoolProp = CompletableFuture::class.java.getWritableFinalField("asyncPool")

        // 将公共线程池应用到 CompletableFuture.asyncPool
        public fun applyCommonPoolToCompletableFuture(){
            if(asyncPoolProp.get(null) != completableFuturePool)
                asyncPoolProp.set(null, completableFuturePool)
        }
    }

    /**
     * 改写 execute() 方法
     *    在执行之前传递一下 ScopedTransferableThreadLocal对象, 在执行后就恢复一下 ScopedTransferableThreadLocal对象
     */
    public override fun execute(command: Runnable) {
        pool.execute(SttlInterceptor.intercept(ScopedTransferableThreadLocal.getLocal2Value(), command))
    }

}
