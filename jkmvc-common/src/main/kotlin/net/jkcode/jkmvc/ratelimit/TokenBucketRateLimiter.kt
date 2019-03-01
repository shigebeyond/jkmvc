package net.jkcode.jkmvc.ratelimit

import net.jkcode.jkmvc.common.time
import java.util.concurrent.atomic.AtomicInteger

/**
 * 令牌桶限流算法
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-01 9:55 AM
 */
class TokenBucketRateLimiter(public val bucketSize: Int /* 令牌桶大小 */) : RateLimiter {

    /**
     * 重注令牌的周期时长
     */
    protected val periodMillis = 200

    /**
     * 每个周期注入的令牌数
     */
    protected var tokenPerPeriod: Int = bucketSize / (1000 / periodMillis)

    /**
     * 当前令牌数
     */
    protected val currentToken: AtomicInteger = AtomicInteger(bucketSize)

    /**
     * 上个周期
     */
    protected var lastPeriod: Long = -1L

    init {
        if (bucketSize <= 0) {
            throw IllegalArgumentException("bucketSize must be positive!")
        }
    }

    /**
     * 扣令牌: 令牌数量 - 1
     * @return
     */
    public override fun acquire(): Boolean {
        var period = time() / periodMillis

        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (period < lastPeriod)
            throw RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastPeriod - period))

        // 1 同一周期
        if (lastPeriod == period)
            return currentToken.getAndDecrement() > 0 // 直接扣令牌

        // 2 不同周期
        val lastToken = currentToken.get()
        lastPeriod = period
        currentToken.compareAndSet(lastToken, bucketSize) // 注满令牌, 只有第一个成功
        return currentToken.getAndDecrement() > 0 // 继续扣令牌
    }


}