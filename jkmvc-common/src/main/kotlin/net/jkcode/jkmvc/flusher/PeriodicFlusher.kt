package net.jkcode.jkmvc.flusher

import io.netty.util.Timeout
import io.netty.util.TimerTask
import net.jkcode.jkmvc.common.CommonMilliTimer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 定时刷盘
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-17 8:27 AM
 */
abstract class PeriodicFlusher<RequestArgumentType /* 请求参数类型 */, ResponseType /* 返回值类型 */>(protected val flushTimeoutMillis: Long /* 触发刷盘的定时时间 */) : IFlusher<RequestArgumentType, ResponseType> {

    /**
     * 定时器状态: 0: 已停止 / 非0: 进行中
     *   用于控制是否停止定时器
     */
    protected val timerState: AtomicInteger = AtomicInteger(0)

    /**
     * 空 -> 非空: 启动定时
     *   在添加请求时调用
     */
    protected fun touchTimer() {
        if ((timerState.get() == 0 || isRequestEmpty()) && timerState.getAndIncrement() == 0)
            startTimer()
    }

    /**
     * 启动刷盘的定时任务
     */
    protected fun startTimer(){
        CommonMilliTimer.newTimeout(object : TimerTask {
            override fun run(timeout: Timeout) {
                // 刷盘
                flush(){
                    // 空: 停止定时
                    if(isRequestEmpty() && timerState.decrementAndGet() == 0)
                        return@flush

                    // 非空: 继续启动定时
                    startTimer()
                }
            }
        }, flushTimeoutMillis, TimeUnit.MILLISECONDS)
    }

}