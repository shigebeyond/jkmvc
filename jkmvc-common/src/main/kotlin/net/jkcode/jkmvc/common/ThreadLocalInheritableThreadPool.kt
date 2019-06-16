package net.jkcode.jkmvc.common

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool

/**
 * 可继承ThreadLocal的线程池
 *   特性: worker thread 在执行任务时, 会继承 caller thread的 ThreadLocal数据
 *   目标: 主要是为了解决异步执行时, 线程状态(ThreadLocal)的传递问题, 如 jkmvc 将当前 Db/HttpRequest 等对象都是记录到 ThreadLocal对象中, 以方便访问, 但是一旦异步执行后就丢失了
 *   实现: 改写 execute() 方法, 在执行之前继承一下 ThreadLocal对象, 在执行后就清理一下 ThreadLocal对象
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-18 4:28 PM
 */
class ThreadLocalInheritableThreadPool(
        protected val pool: ExecutorService,
        protected val cleaning: Boolean = true /* 是否在执行后就清理 ThreadLocal对象: 对于CompletableFuture, 其后续处理仍然会用到ThreadLocal对象, 此时不能清理 */
): ExecutorService by pool {

    companion object {

        // 公共的线程池: 包装 ForkJoinPool.commonPool()
        public val commonPool by lazy {
            ThreadLocalInheritableThreadPool(ForkJoinPool.commonPool())
        }

        // -------------------------------------------
        // CompletableFuture的线程池: 包装 ForkJoinPool.commonPool()
        public val completableFuturePool by lazy {
            ThreadLocalInheritableThreadPool(ForkJoinPool.commonPool(), false)
        }

        // CompletableFuture.asyncPool 属性
        //protected val asyncPoolProp = CompletableFuture::class.getStaticProperty("asyncPool") as KMutableProperty0<Any?>
        protected val asyncPoolProp = CompletableFuture::class.java.getWritableFinalField("asyncPool")

        // 将公共线程池应用到 CompletableFuture.asyncPool
        public fun applyCommonPoolToCompletableFuture(){
            asyncPoolProp.set(null, completableFuturePool)
        }
    }

    /**
     * 改写 execute() 方法
     *    在执行之前继承一下 ThreadLocal对象, 在执行后就清理一下 ThreadLocal对象
     */
    public override fun execute(command: Runnable) {
        execute(ThreadLocalInheritableInterceptor(cleaning).intercept(command))
    }

}
