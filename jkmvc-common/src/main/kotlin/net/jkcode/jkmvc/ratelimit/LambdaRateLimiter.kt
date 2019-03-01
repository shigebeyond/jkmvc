package net.jkcode.jkmvc.ratelimit

/**
 * 用lambda封装限流器
 * @author shijianhang<772910474@qq.com>
 * @date 2019-01-23 7:56 PM
 */
class LambdaRateLimiter(protected val lambda: () -> Boolean) : IRateLimiter {

    /**
     * 获得一次访问许可
     */
    public override fun acquire(): Boolean {
        return lambda()
    }

}