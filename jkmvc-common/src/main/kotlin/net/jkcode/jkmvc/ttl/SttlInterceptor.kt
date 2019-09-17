package net.jkcode.jkmvc.ttl

import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function

/**
 * 可传递ScopedTransferableThreadLocal的拦截器, 只传递ScopedTransferableThreadLocal, 无关ThreadLocal
 *   特性: worker thread 在执行任务时, 会传递 caller thread的 ScopedTransferableThreadLocal数据
 *   目标: 主要是为了解决异步执行时, 线程状态(ScopedTransferableThreadLocal)的传递问题, 如 jkmvc 将当前 Db/HttpRequest 等对象都是记录到 ScopedTransferableThreadLocal对象中, 以方便访问, 但是一旦异步执行后就丢失了
 *   实现: 在执行之前传递一下 ScopedTransferableThreadLocal对象, 在执行后就恢复一下 ScopedTransferableThreadLocal对象
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-18 4:28 PM
 */
object SttlInterceptor {

    /**
     * 包含前置后置处理
     * @param callerLocals caller thread 的ScopedTransferableThreadLocal对象
     * @param action
     * @return
     */
    public inline fun <T> wrap(callerLocals: Local2Value = ScopedTransferableThreadLocal.getLocal2Value(), action:() -> T): T {
        // 0 worker thread 的ScopedTransferableThreadLocal对象
        val workerLocals = ScopedTransferableThreadLocal.getLocal2Value()
        try {

            // 1 在执行之前, 传递(直接引用) caller thread 的ScopedTransferableThreadLocal对象, 因为是同一个ScopedTransferableThreadLocal对象, 因此 caller thread 与 worker thread 都能看到批次的改动
            ScopedTransferableThreadLocal.setLocal2Value(callerLocals)

            // 2 执行
            return action.invoke()
        }finally {
            // 3 在执行结束后, 恢复ScopedTransferableThreadLocal对象
            ScopedTransferableThreadLocal.setLocal2Value(workerLocals)
        }
    }

    /**
     * 拦截 Runnable
     * @param callerLocals caller thread 的ScopedTransferableThreadLocal对象
     * @param command
     * @return
     */
    public fun intercept(callerLocals: Local2Value = ScopedTransferableThreadLocal.getLocal2Value(), command: Runnable): Runnable {
        return object: Runnable{
            override fun run() {
                wrap(callerLocals){ command.run() }
            }
        }
    }

    /**
     * 拦截 Function
     * @param callerLocals caller thread 的ScopedTransferableThreadLocal对象
     * @param fn
     * @return
     */
    public fun <T, U> intercept(callerLocals: Local2Value = ScopedTransferableThreadLocal.getLocal2Value(), fn: Function<T, U>): Function<T, U> {
        return object: Function<T, U>{
            override fun apply(t: T): U {
                return wrap(callerLocals){ fn.apply(t) }
            }
        }
    }

    /**
     * 拦截 BiFunction
     * @param callerLocals caller thread 的ScopedTransferableThreadLocal对象
     * @param fn
     * @return
     */
    public fun <T, U, V> intercept(callerLocals: Local2Value = ScopedTransferableThreadLocal.getLocal2Value(), fn: BiFunction<T, U, out V>): BiFunction<T, U, out V> {
        return object: BiFunction<T, U, V>{
            override fun apply(t: T, u: U): V {
                return wrap(callerLocals){ fn.apply(t, u) }
            }
        }
    }

    /**
     * 拦截 Consumer
     * @param callerLocals caller thread 的ScopedTransferableThreadLocal对象
     * @param consumer
     * @return
     */
    public fun <T> intercept(callerLocals: Local2Value = ScopedTransferableThreadLocal.getLocal2Value(), consumer: Consumer<T>): Consumer<T> {
        return object: Consumer<T>{
            override fun accept(t: T) {
                return wrap(callerLocals){ consumer.accept(t) }
            }
        }
    }

    /**
     * 拦截 BiConsumer
     * @param callerLocals caller thread 的ScopedTransferableThreadLocal对象
     * @param consumer
     * @return
     */
    public fun <T, U> intercept(callerLocals: Local2Value = ScopedTransferableThreadLocal.getLocal2Value(), consumer: BiConsumer<T, U>): BiConsumer<T, U> {
        return object: BiConsumer<T, U>{
            override fun accept(t: T, u: U) {
                return wrap(callerLocals){ consumer.accept(t, u) }
            }
        }
    }

    /**
     * 拦截 command lambda
     * @param callerLocals caller thread 的ScopedTransferableThreadLocal对象
     * @param command
     * @return
     */
    public fun intercept(callerLocals: Local2Value = ScopedTransferableThreadLocal.getLocal2Value(), command: () -> Any?): (() -> Any?) {
        return { ->
            wrap(callerLocals, command)
        }
    }

    /**
     * 拦截 fn lambda
     * @param callerLocals caller thread 的ScopedTransferableThreadLocal对象
     * @param fn
     * @return
     */
    public fun intercept(callerLocals: Local2Value = ScopedTransferableThreadLocal.getLocal2Value(), fn: (Any?) -> Any?): ((Any?) -> Any?) {
        return { r ->
            wrap(callerLocals){ fn.invoke(r) }
        }
    }

    /**
     * 拦截 complete lambda
     * @param callerLocals caller thread 的ScopedTransferableThreadLocal对象
     * @param complete
     * @return
     */
    public fun intercept(callerLocals: Local2Value = ScopedTransferableThreadLocal.getLocal2Value(), complete: (Any?, Throwable?) -> Any?): ((Any?, Throwable?) -> Any?) {
        return { r, ex ->
            wrap(callerLocals){ complete.invoke(r, ex) }
        }
    }

    /********************** lambda 转其他类型 ***********************/
    /**
     * 拦截 Runnable
     * @param callerLocals caller thread 的ScopedTransferableThreadLocal对象
     * @param command
     * @return
     */
    public fun interceptToRunnable(callerLocals: Local2Value = ScopedTransferableThreadLocal.getLocal2Value(), command: () -> Unit): Runnable {
        return object: Runnable{
            override fun run() {
                wrap(callerLocals){ command.invoke() }
            }
        }
    }

    /**
     * 拦截 Function
     * @param callerLocals caller thread 的ScopedTransferableThreadLocal对象
     * @param fn
     * @return
     */
    public fun <T, U> interceptToFunction(callerLocals: Local2Value = ScopedTransferableThreadLocal.getLocal2Value(), fn: (T) -> U): Function<T, U> {
        return object: Function<T, U>{
            override fun apply(t: T): U {
                return wrap(callerLocals){ fn.invoke(t) }
            }
        }
    }

    /**
     * 拦截 BiFunction
     * @param callerLocals caller thread 的ScopedTransferableThreadLocal对象
     * @param fn
     * @return
     */
    public fun <T, U, V> interceptToBiFunction(callerLocals: Local2Value = ScopedTransferableThreadLocal.getLocal2Value(), fn: (T, U) -> V): BiFunction<T, U, out V> {
        return object: BiFunction<T, U, V>{
            override fun apply(t: T, u: U): V {
                return wrap(callerLocals){ fn.invoke(t, u) }
            }
        }
    }

    /**
     * 拦截 Consumer
     * @param callerLocals caller thread 的ScopedTransferableThreadLocal对象
     * @param consumer
     * @return
     */
    public fun <T> interceptToConsumer(callerLocals: Local2Value = ScopedTransferableThreadLocal.getLocal2Value(), consumer: (T) -> Unit): Consumer<T> {
        return object: Consumer<T>{
            override fun accept(t: T) {
                return wrap(callerLocals){ consumer.invoke(t) }
            }
        }
    }

    /**
     * 拦截 BiConsumer
     * @param callerLocals caller thread 的ScopedTransferableThreadLocal对象
     * @param consumer
     * @return
     */
    public fun <T, U> interceptToBiConsumer(callerLocals: Local2Value = ScopedTransferableThreadLocal.getLocal2Value(), consumer: (T, U) -> T): BiConsumer<T, U> {
        return object: BiConsumer<T, U>{
            override fun accept(t: T, u: U) {
                return wrap(callerLocals){ consumer.invoke(t, u) }
            }
        }
    }
}
