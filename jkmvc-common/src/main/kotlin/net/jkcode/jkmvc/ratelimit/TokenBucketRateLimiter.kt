package net.jkcode.jkmvc.ratelimit

import net.jkcode.jkmvc.common.currMillis
import java.util.concurrent.atomic.AtomicInteger

/**
 * 令牌桶限流算法
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-01 9:55 AM
 */
class TokenBucketRateLimiter(public val bucketSize: Int /* 令牌桶大小 */,
                             public val intervalMillis: Int = 200 /* 重置的时间窗口时长, 单位毫秒 */
) : IRateLimiter {

    /**
     * 每个时间窗口注入的令牌数
     */
    protected var tokenPerInterval: Int = bucketSize / (1000 / intervalMillis)

    /**
     * 当前令牌数
     */
    protected val currentToken: AtomicInteger = AtomicInteger(tokenPerInterval)

    /**
     * 上个时间窗口
     */
    @Volatile
    protected var lastInterval: Long = -1L

    init {
        if (bucketSize <= 0)
            throw IllegalArgumentException("bucketSize must be positive!")
    }

    /**
     * 扣令牌: 令牌数量 - 1
     * @return
     */
    public override fun acquire(): Boolean {
        var interval = currMillis() / intervalMillis
        val lastInterval = this.lastInterval

        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (interval < lastInterval)
            throw RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastInterval - interval))

        // 1 同一时间窗口
        if (lastInterval == interval)
            return currentToken.get() > 0 && currentToken.getAndDecrement() > 0 // 直接扣令牌

        // 2 不同时间窗口
        val lastToken = currentToken.get()
        if(currentToken.compareAndSet(lastToken, tokenPerInterval)) // 注满令牌, 只有第一个成功
            this.lastInterval = interval
        return currentToken.get() > 0 && currentToken.getAndDecrement() > 0 // 继续扣令牌
    }

}