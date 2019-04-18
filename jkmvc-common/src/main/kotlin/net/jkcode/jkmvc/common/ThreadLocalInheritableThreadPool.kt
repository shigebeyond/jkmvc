package net.jkcode.jkmvc.common

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.javaField

/**
 * 可继承ThreadLocal的线程池
 *   特性: worker thread 在执行任务时, 会继承 caller thread的 ThreadLocal数据
 *   目标: 主要是为了解决异步执行时, 线程状态(ThreadLocal)的传递问题, 如 jkmvc 将当前 Db/HttpRequest 等对象都是记录到 ThreadLocal对象中, 以方便访问, 但是一旦异步执行后就丢失了
 *   实现: 改写 execute() 方法, 在执行之前继承一下 ThreadLocal对象, 在执行后就清理一下 ThreadLocal对象
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-18 4:28 PM
 */
class ThreadLocalInheritableThreadPool(protected val pool: ExecutorService): ExecutorService by pool {

    companion object {

        // 预先创建2个 ThreadLocal 对象
        // 主要是给 caller thread 调用以便初始化 threadLocals/inheritableThreadLocals 2个属性
        protected val local = ThreadLocal<Any>()
        protected val inheritableLocal = InheritableThreadLocal<Any>()

        // ThreadLocal 2个相关属性
        // Thread.threadLocals 属性
        protected val threadLocalProp = Thread::class.getProperty("threadLocals") as KMutableProperty1<Thread, Any?>
         // Thread.inheritableThreadLocals 属性
        protected val inheritableThreadLocalProp = Thread::class.getProperty("inheritableThreadLocals") as KMutableProperty1<Thread, Any?>

        init {
            // 开放属性访问
            threadLocalProp.javaField!!.isAccessible = true
            inheritableThreadLocalProp.javaField!!.isAccessible = true
        }

        // 公共的线程池: 包装 ForkJoinPool.commonPool()
        public val commonPool by lazy {
            ThreadLocalInheritableThreadPool(ForkJoinPool.commonPool())
        }

        // CompletableFuture.asyncPool 属性
        //protected val asyncPoolProp = CompletableFuture::class.getStaticProperty("asyncPool") as KMutableProperty0<Any?>
        protected val asyncPoolProp = CompletableFuture::class.java.getWritableFinalField("asyncPool")

        // 将公共线程池应用到 CompletableFuture.asyncPool
        public fun applyCommonPoolToCompletableFuture(){
            asyncPoolProp.set(null, commonPool)
        }

    }

    /**
     * 改写 execute() 方法
     *    在执行之前继承一下 ThreadLocal对象, 在执行后就清理一下 ThreadLocal对象
     */
    public override fun execute(command: Runnable) {
        val caller = Thread.currentThread()
        // 1 初始化 threadLocals/inheritableThreadLocals 2个属性
        // 如果 caller thread 没有使用 ThreadLocal对象(即相关的2个属性为null)，而 worker thread 可能用到ThreadLocal对象，这就要求预先创建 ThreadLocal对象
        if (threadLocalProp.get(caller) == null)
            local.get()
        if (inheritableThreadLocalProp.get(caller) == null)
            inheritableLocal.get()

        // 2 先拿住2个属性, 解决在单线程的线程池中嵌套提交异步任务而导致ThreadLocal对象丢失的情况
        // 如 caller thread 提交任务(传递ThreadLocal对象) -> worker thread 执行任务(清理ThreadLocal对象) -> worker thread 嵌套提交任务(丢失ThreadLocal对象)
        val value = threadLocalProp.get(caller)
        val inheritableValue = inheritableThreadLocalProp.get(caller)

        // 调用被代理的线程池
        pool.execute(){
            // 在执行之前, 继承(直接引用) caller thread 的ThreadLocal对象, 因为是同一个ThreadLocal对象, 因此 caller thread 与 worker thread 都能看到批次的改动
            val worker = Thread.currentThread()
            threadLocalProp.set(worker, value)
            inheritableThreadLocalProp.set(worker, inheritableValue)
            println("[${worker.name}] 继承 [${caller.name}] 的ThreadLocal对象")

            // 执行
            command.run()

            // 在执行结束后, 清理
            threadLocalProp.set(worker, null)
            inheritableThreadLocalProp.set(worker, null)
            println("[${worker.name}] 清理ThreadLocal对象")
        }
    }

}
