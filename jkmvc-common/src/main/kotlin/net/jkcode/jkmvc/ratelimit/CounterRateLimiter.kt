package net.jkcode.jkmvc.ratelimit

import net.jkcode.jkmvc.common.time
import java.util.concurrent.atomic.AtomicInteger

/**
 *  计数器限流算法
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-01 10:16 AM
 */
class CounterRateLimiter(public val maxQps: Int) : RateLimiter {

    /**
     * 周期时长
     */
    protected val periodMillis = 200

    /**
     * 每个周期的最大请求数
     */
    protected val maxReqPerInterval: Int = maxQps / (1000 / periodMillis)

    /**
     * 当前请求数
     */
    protected val count = AtomicInteger(0)

    /**
     * 上个周期
     */
    protected var lastPeriod: Long = -1L

    init {
        if (maxQps <= 0) {
            throw IllegalArgumentException("maxQps must be positive!")
        }
    }

    /**
     * 计数+1, 然后检查是否超过限额
     * @return
     */
    public override fun acquire(): Boolean {

        var period = time() / periodMillis

        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (period < lastPeriod)
            throw RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastPeriod - period))

        // 1 同一周期
        if (lastPeriod == period)
            return count.getAndIncrement() <= maxReqPerInterval // 计数+1

        // 2 不同周期
        val lastCount = count.get()
        lastPeriod = period
        count.compareAndSet(lastCount, 0) // 计数清零, 只有第一个成功
        return count.getAndIncrement() <= maxReqPerInterval // 计数+1
    }

}
