package net.jkcode.jkmvc.cache

import net.jkcode.jkmvc.lock.IKeyLock
import net.jkcode.jkmvc.lock.LocalKeyLock

/**
 * 基础缓存类
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-06 10:42 AM
 */
abstract class BaseCache: ICache {

    /**
     * 对key的锁, 防止并发回源
     */
    protected val lock: IKeyLock = IKeyLock.instance("jedis")

    /**
     * 根据键获得值
     *
     * @param key 键
     * @param expires 过期时间（秒）
     * @param defaultValue 回源值的函数
     * @return
     */
    public override fun getOrPut(key: Any, expires:Long, defaultValue: () -> Any): Any? {
        val v = this.get(key)
        if (v != null)
            return v

        // 锁住key, 防止并发回源
        return lock.quickLockCleanly(key){
            val default = defaultValue()
            this.put(key, default, expires)
            default
        }
    }

}