package com.jkmvc.future

/**
 * 已完成的异步结果，没有延迟
 *
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-12-30 6:43 PM
 */
open class CompletedFuture<T>(protected val result: T): java.util.concurrent.Future<T> {
    /**
     * Returns `true` if this task completed.

     * Completion may be due to normal termination, an cause, or
     * cancellation -- in all of these cases, this method will return
     * `true`.

     * @return `true` if this task completed
     */
    public override fun isDone(): Boolean {
        return true
    }

    /**
     * Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, has already been cancelled,
     * or could not be cancelled for some other reason. If successful,
     * and this task has not started when `cancel` is called,
     * this task should never run.  If the task has already started,
     * then the `mayInterruptIfRunning` parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.

     *
     * After this method returns, subsequent calls to [.isDone] will
     * always return `true`.  Subsequent calls to [.isCancelled]
     * will always return `true` if this method returned `true`.

     * @param mayInterruptIfRunning `true` if the thread executing this
     * * task should be interrupted; otherwise, in-progress tasks are allowed
     * * to complete
     * *
     * @return `false` if the task could not be cancelled,
     * * typically because it has already completed normally;
     * * `true` otherwise
     */
    public override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        return false
    }

    /**
     * Returns `true` if this task was cancelled before it completed
     * normally.

     * @return `true` if this task was cancelled before it completed
     */
    public override fun isCancelled(): Boolean {
        return false
    }

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.

     * @param timeout the maximum time to wait
     * *
     * @param unit the time unit of the timeout argument
     * *
     * @return the computed result
     * *
     * @throws CancellationException if the computation was cancelled
     * *
     * @throws ExecutionException if the computation threw an
     * * cause
     * *
     * @throws InterruptedException if the current thread was interrupted
     * * while waiting
     * *
     * @throws TimeoutException if the wait timed out
     */
    public override fun get(timeout: Long, unit: java.util.concurrent.TimeUnit?): T {
        return result
    }

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.

     * @return the computed result
     * *
     * @throws CancellationException if the computation was cancelled
     * *
     * @throws ExecutionException if the computation threw an
     * * cause
     * *
     * @throws InterruptedException if the current thread was interrupted
     * * while waiting
     */
    public override fun get(): T {
        return result
    }

}
