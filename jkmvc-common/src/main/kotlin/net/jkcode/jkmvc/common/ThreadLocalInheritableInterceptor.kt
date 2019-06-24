package net.jkcode.jkmvc.common

import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.javaField

/**
 * 可继承ThreadLocal的拦截器
 *   特性: worker thread 在执行任务时, 会继承 caller thread的 ThreadLocal数据
 *   目标: 主要是为了解决异步执行时, 线程状态(ThreadLocal)的传递问题, 如 jkmvc 将当前 Db/HttpRequest 等对象都是记录到 ThreadLocal对象中, 以方便访问, 但是一旦异步执行后就丢失了
 *   实现: 在执行之前继承一下 ThreadLocal对象, 在执行后就清理一下 ThreadLocal对象
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-18 4:28 PM
 */
class ThreadLocalInheritableInterceptor(protected val cleaning: Boolean = true /* 是否在执行后就清理 ThreadLocal对象: 对于CompletableFuture, 其后续处理仍然会用到ThreadLocal对象, 此时不能清理 */) {

    companion object {

        // 预先创建2个 ThreadLocal 对象
        // 主要是给 caller thread 调用以便初始化 threadLocals/inheritableThreadLocals 2个属性
        protected val localObj = ThreadLocal<Any>()
        protected val inheritableLocalObj = InheritableThreadLocal<Any>()

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
    }

    // 创建时(不是执行时)的 ThreadLocal 2个相关属性值
    protected var value: Any? = null
    protected var inheritableValue: Any? = null

    init {
        val caller = Thread.currentThread()
        // 1 初始化 threadLocals/inheritableThreadLocals 2个属性
        // 如果 caller thread 没有使用 ThreadLocal对象(即相关的2个属性为null)，而 worker thread 可能用到ThreadLocal对象，这就要求预先创建 ThreadLocal对象
        if (threadLocalProp.get(caller) == null)
            localObj.get()
        if (inheritableThreadLocalProp.get(caller) == null)
            inheritableLocalObj.get()

        // 2 先拿住2个属性, 解决在单线程的线程池中嵌套提交异步任务而导致ThreadLocal对象丢失的情况
        // 如 caller thread 提交任务(传递ThreadLocal对象) -> worker thread 执行任务(清理ThreadLocal对象) -> worker thread 嵌套提交任务(丢失ThreadLocal对象)
        value = threadLocalProp.get(caller)
        inheritableValue = inheritableThreadLocalProp.get(caller)
    }

    /**
     * 包含前置后置处理
     * @param action
     * @return
     */
    public inline fun <T> wrap(action:() -> T): T {
        try {
            // 3 在执行之前, 继承(直接引用) caller thread 的ThreadLocal对象, 因为是同一个ThreadLocal对象, 因此 caller thread 与 worker thread 都能看到批次的改动
            beforeExecute()

            // 4 执行
            return action.invoke()
        }finally {
            // 5 在执行结束后, 清理
            afterExecute()
        }
    }

    /**
     * 在执行之前, 继承(直接引用) caller thread 的ThreadLocal对象, 因为是同一个ThreadLocal对象, 因此 caller thread 与 worker thread 都能看到批次的改动
     */
    public fun beforeExecute() {
        val worker = Thread.currentThread()
        threadLocalProp.set(worker, value)
        inheritableThreadLocalProp.set(worker, inheritableValue)
        //println("[${worker.name}] 继承 [${caller.name}] 的ThreadLocal对象")
    }

    /**
     * 在执行结束后, 清理ThreadLocal对象
     */
    public fun afterExecute() {
        if(cleaning) {
            val worker = Thread.currentThread()
            threadLocalProp.set(worker, null)
            inheritableThreadLocalProp.set(worker, null)
            //println("[${worker.name}] 清理ThreadLocal对象")
        }

        value = null
        inheritableValue = null
    }

    /**
     * 拦截 Runnable
     */
    public fun intercept(command: Runnable): Runnable {
        return object: Runnable{
            override fun run() {
                wrap { command.run() }
            }
        }
    }

    /**
     * 拦截 Function
     */
    public fun <T, U> intercept(fn: Function<T, U>): Function<T, U>? {
        return object: Function<T, U>{
            override fun apply(t: T): U {
                return wrap { fn.apply(t) }
            }
        }
    }

    /**
     * 拦截 BiFunction
     */
    public fun <T, U, V> intercept(fn: BiFunction<T, U, out V>): BiFunction<T, U, out V> {
        return object: BiFunction<T, U, V>{
            override fun apply(t: T, u: U): V {
                return wrap { fn.apply(t, u) }
            }
        }
    }

    /**
     * 拦截 Consumer
     */
    public fun <T> intercept(consumer: Consumer<T>): Consumer<T>? {
        return object: Consumer<T>{
            override fun accept(t: T) {
                return wrap { consumer.accept(t) }
            }
        }
    }

    /**
     * 拦截 BiConsumer
     */
    public fun <T, U> intercept(consumer: BiConsumer<T, U>): BiConsumer<T, U>? {
        return object: BiConsumer<T, U>{
            override fun accept(t: T, u: U) {
                return wrap { consumer.accept(t, u) }
            }
        }
    }

    /**
     * 拦截 command lambda
     */
    public fun intercept(command: () -> Any?): (() -> Any?) {
        return { ->
            wrap(command)
        }
    }

    /**
     * 拦截 fn lambda
     */
    public fun intercept(fn: (Any?) -> Any?): ((Any?) -> Any?) {
        return { r ->
            wrap{ fn.invoke(r) }
        }
    }

    /**
     * 拦截 complete lambda
     */
    public fun intercept(complete: (Any?, Throwable?) -> Any?): ((Any?, Throwable?) -> Any?) {
        return { r, e ->
            wrap{ complete.invoke(r, e) }
        }
    }

}
