package net.jkcode.jkmvc.lock

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 针对不同key加锁 -- 本地锁
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-06 8:54 AM
 */
class LocalKeyLock : IKeyLock {

    /**
     * <键 to 锁>
     */
    protected val locks: ConcurrentHashMap<Any, AtomicBoolean> by lazy {
        ConcurrentHashMap<Any, AtomicBoolean>()
    }

    /**
     * 快速加锁, 锁不住不等待, 有过期时间(只针对分布式锁)
     *
     * @param key
     * @param expireSeconds 锁的过期时间, 单位秒
     * @return 是否加锁成功
     */
    public override fun quickLock(key: Any, expireSeconds: Int): Boolean{
        val lock = locks.getOrPut(key){
            AtomicBoolean(false)
        }

        return lock.compareAndSet(false, true) // 加锁
    }

    /**
     * 解锁
     */
    public override fun unlock(key: Any){
        locks[key]!!.set(false)
    }


}