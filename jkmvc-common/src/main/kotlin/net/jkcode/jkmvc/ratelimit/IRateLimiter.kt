package net.jkcode.jkmvc.ratelimit

import net.jkcode.jkmvc.cache.ICache
import net.jkcode.jkmvc.common.Config
import net.jkcode.jkmvc.common.IConfig
import net.jkcode.jkmvc.singleton.NamedConfiguredSingletons

/**
 * 限流算法
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-01 9:56 AM
 */
interface IRateLimiter {

    /**
     * 获得一次访问许可
     */
    fun acquire(): Boolean

}
