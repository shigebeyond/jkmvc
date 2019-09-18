package net.jkcode.jksoa.lock

import net.jkcode.jkmvc.lock.IKeyLock
import net.jkcode.jkmvc.common.currMillis
import java.util.*

/**
 * 分布式锁接口
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-01-11 12:24 PM
 */
abstract class IDistributedKeyLock(): IKeyLock {

    /**
     * key对应的过期时间, 用于记录本地jvm获得过的锁的过期时间
     */
    private val key2ExpireTimes: ThreadLocal<MutableMap<Any, Long?>> = ThreadLocal.withInitial {
        HashMap<Any, Long?>()
    }

    /**
     * 是否获得锁
     * @param key
     * @return
     */
    public fun isNotExpired(key: Any): Boolean{
        val expireTime = key2ExpireTimes.get()[key]
        return expireTime != null && expireTime!! < currMillis() // 未过期
    }

    /**
     * 快速加锁, 锁不住不等待, 有过期时间
     *
     * @param key
     * @param expireSeconds 锁的过期时间, 单位秒
     * @return
     */
    public override fun quickLock(key: Any, expireSeconds: Int): Boolean{
        val result = doQuickLock(key, expireSeconds)
        // 更新过期时间
        if(result)
            key2ExpireTimes.get()[key] = currMillis() + expireSeconds * 1000
        return result
    }

    /**
     * 快速加锁, 锁不住不等待, 有过期时间
     *
     * @param key
     * @param expireSeconds 锁的过期时间, 单位秒
     * @return
     */
    public abstract fun doQuickLock(key: Any, expireSeconds: Int): Boolean;

    /**
     * 解锁
     *
     * @param key
     */
    public override fun unlock(key: Any){
        doUnlock(key)
        // 删除过期时间
        key2ExpireTimes.get().remove(key)
    }

    /**
     * 解锁
     *
     * @param key
     */
    public abstract fun doUnlock(key: Any);

}