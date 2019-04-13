package net.jkcode.jkmvc.lock

import net.jkcode.jkmvc.common.Config
import net.jkcode.jkmvc.common.IConfig
import net.jkcode.jkmvc.singleton.NamedConfiguredSingletons

/**
 * 针对不同key加锁
 *    单机用 local: IKeyLock.instance("local")
 *    分布式用 jedis: IKeyLock.instance("jedis")
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-06 8:54 AM
 */
interface IKeyLock {

    // 可配置的单例
    companion object: NamedConfiguredSingletons<IKeyLock>() {
        /**
         * 单例类的配置，内容是哈希 <单例名 to 单例类>
         */
        public override val instsConfig: IConfig = Config.instance("lock", "yaml")
    }

    /**
     * 快速加锁, 锁不住不等待, 有过期时间(只针对分布式锁)
     *
     * @param key
     * @param expireSeconds 锁的过期时间, 单位秒
     * @return 是否加锁成功
     */
    fun quickLock(key: Any, expireSeconds: Int = 5): Boolean

    /**
     * 解锁
     *
     * @param key
     */
    fun unlock(key: Any)

    /**
     * 快速加锁, 锁不住不等待, 有过期时间
     *
     * @param expireSeconds 锁的过期时间, 单位秒
     * @param block 处理
     */
    public fun quickLockCleanly(key: Any, expireSeconds: Int = 5, block: () -> Unit){
        if(quickLock(key, expireSeconds)){
            try{
                block()
            }finally {
                unlock(key)
            }
        }
    }
}