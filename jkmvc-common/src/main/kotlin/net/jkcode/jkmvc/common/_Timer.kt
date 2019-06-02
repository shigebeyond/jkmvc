package net.jkcode.jkmvc.common

import io.netty.util.HashedWheelTimer
import io.netty.util.Timeout
import io.netty.util.TimerTask
import net.jkcode.jkmvc.closing.ClosingOnShutdown
import java.util.concurrent.TimeUnit

/**
 * 公共的毫秒级定时器
 *   HashedWheelTimer 是单线程的, 因此每个定时任务执行耗时不能太长, 如果有耗时任务, 则扔到其他线程池(如ForkJoinPool.commonPool())中处理
 */
public val CommonMilliTimer by lazy{
    HashedWheelTimer(1, TimeUnit.MILLISECONDS, 256 /* 2的次幂 */)
}

/**
 * 公共的秒级定时器
 *   HashedWheelTimer 是单线程的, 因此每个定时任务执行耗时不能太长, 如果有耗时任务, 则扔到其他线程池(如ForkJoinPool.commonPool())中处理
 */
public val CommonSecondTimer by lazy{
    HashedWheelTimer(200, TimeUnit.MILLISECONDS, 64 /* 2的次幂 */)
}

/**
 * 添加周期性任务
 * @param task 任务
 * @param period 周期时间
 * @param unit 时间单位
 * @return
 */
public fun HashedWheelTimer.newPeriodic(task: () -> Unit, period: Long, unit: TimeUnit): Timeout{
    // 定时触发
    return newTimeout(object : TimerTask {
        override fun run(timeout: Timeout) {
            // 执行任务
            task.invoke()

            // 递归
            newTimeout(this, period, unit)
        }
    }, period, unit)
}

/**
 * 关闭定时器
 */
public val timerCloser = object: ClosingOnShutdown(){
    override fun close() {
        CommonMilliTimer.stop()
        CommonSecondTimer.stop()
    }

}