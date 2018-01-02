package com.jkmvc.future

import org.apache.http.concurrent.Cancellable
import org.apache.http.concurrent.FutureCallback
import java.util.concurrent.*

/**
 * 异步结果的通用实现
 *   可以通过以下方法来表示完成状态: cancel() / failed() / completed()
 *   参考包 org.apache.httpcomponents:httpcore:4.4.7 中的类 org.apache.http.concurrent.BasicFuture 的实现，但由于 BasicFuture 中的属性都是public的，所以子类无法实现IResponse接口，因此无法继承，只是复制
 *
 * @ClassName: BasicFuture
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-12-30 6:43 PM
 */
open class BasicFuture<T>(protected val callback: FutureCallback<T?>? = null /* 回调 */): Future<T?>, Cancellable {

    /**
     * this的锁，即this自己
     *   Kotlin的Any类似于Java的Object，但没有wait()，notify()和notifyAll()方法，因此只能将 this 转为 Object 才能调用这些方法
     */
    protected val thisLock:java.lang.Object = this as java.lang.Object

    /**
     * 是否已完成
     */
    @Volatile protected var completed: Boolean = false

    /**
     * 是否被取消
     */
    @Volatile protected var cancelled: Boolean = false

    /**
     * 结果
     */
    @Volatile public var result: T? = null
        protected set

    /**
     * 异常
     */
    @Volatile public var ex: Exception? = null
        protected set

    /**
     * 是否被取消
     * @return
     */
    public override fun isCancelled(): Boolean {
        return this.cancelled
    }

    /**
     * 是否已完成
     * @return
     */
    public override fun isDone(): Boolean {
        return this.completed
    }

    /**
     * 尝试获得结果，如果该响应未完成，则抛出异常
     * @return
     */
    @Throws(ExecutionException::class)
    protected fun tryGetResult(): T? {
        if (ex != null)
            throw ExecutionException(ex)

        if (cancelled)
            throw CancellationException()

        return this.result
    }

    /**
     * 同步获得结果，无超时
     * @return
     */
    @Synchronized @Throws(InterruptedException::class, ExecutionException::class)
    public override fun get(): T? {
        while (!this.completed)
            thisLock.wait()

        return tryGetResult()
    }

    /**
     * 同步获得结果，有超时
     *
     * @param timeout
     * @param unit
     * @return
     */
    @Synchronized @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    public override fun get(timeout: Long, unit: TimeUnit): T? {
        val msecs = unit.toMillis(timeout)
        val startTime = if (msecs <= 0) 0 else System.currentTimeMillis()
        var waitTime = msecs
        if (completed)
            return tryGetResult()

        if (waitTime <= 0)
            throw TimeoutException()

        while (true) {
            thisLock.wait(waitTime)
            if (completed)
                return tryGetResult()

            waitTime = msecs - (System.currentTimeMillis() - startTime)
            if (waitTime <= 0)
                throw TimeoutException()
        }
    }

    /**
     * 完成
     *
     * @param result
     * @return
     */
    public fun completed(result: T?): Boolean {
        synchronized(this) {
            if (this.completed) // 处理重入
                return false

            // 标识完成 + 记录结果
            completed = true
            this.result = result
            thisLock.notifyAll()
        }
        // 回调
        callback?.completed(result)
        return true
    }

    /**
     * 失败
     *
     * @param exception
     * @return
     */
    public fun failed(exception: Exception): Boolean {
        synchronized(this) {
            if (completed) // 处理重入
                return false

            // 标识完成 + 记录异常
            completed = true
            ex = exception
            thisLock.notifyAll()
        }
        // 回调
        callback?.failed(exception)
        return true
    }

    /**
     * 取消
     *
     * @param mayInterruptIfRunning
     * @return
     */
    public override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        synchronized(this) {
            if (this.completed) // 处理重入
                return false

            // 标识完成 + 标识取消
            completed = true
            cancelled = true
            thisLock.notifyAll()
        }
        // 回调
        callback?.cancelled()
        return true
    }

    /**
     * 取消
     *
     * @return
     */
    public override fun cancel(): Boolean {
        return cancel(true)
    }

}
