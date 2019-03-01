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
interface RateLimiter {

    // 可配置的单例
    companion object: NamedConfiguredSingletons<ICache>() {
        /**
         * 单例类的配置，内容是哈希 <单例名 to 单例类>
         */
        public override val instsConfig: IConfig = Config.instance("rate-limiter", "yaml")
    }

    /**
     * 获得一次访问许可
     */
    fun acquire(): Boolean

}
