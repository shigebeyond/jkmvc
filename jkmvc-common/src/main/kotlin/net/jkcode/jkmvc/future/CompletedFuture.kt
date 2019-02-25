package net.jkcode.jkmvc.future

import java.util.concurrent.Future

/**
 * 已完成的异步结果，没有延迟
 *
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-12-30 6:43 PM
 */
open class CompletedFuture<T>(protected open val result: T): Future<T> {
    /**
     * 任务是否完成
     * @return
     */
    public override fun isDone(): Boolean {
        return true
    }

    /**
     * 尝试取消任务
     * @return 是否取消成功
     */
    public override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        return false
    }

    /**
     * 任务是否被取消
     * @return
     */
    public override fun isCancelled(): Boolean {
        return false
    }

    /**
     * 等待指定的时间并返回结果
     * @param timeout
     * @param unit
     * @return
     */
    public override fun get(timeout: Long, unit: java.util.concurrent.TimeUnit?): T {
        return result
    }

    /**
     * 等待并返回结果
     * @return
     */
    public override fun get(): T {
        return result
    }

}
