package net.jkcode.jkmvc.common

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 针对不同key加锁
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-06 8:54 AM
 */
class MultiKeyLock {

    /**
     * <键 to 锁>
     */
    protected val locks: ConcurrentHashMap<Any, AtomicBoolean> by lazy {
        ConcurrentHashMap<Any, AtomicBoolean>()
    }

    /**
     * 快速加锁, 锁不住不等待
     * @param key
     * @param block
     * @return
     */
    public fun <T> quickLock(key: Any, block: () -> T): T? {
        val lock = locks.getOrPutOnce(key){
            AtomicBoolean(false)
        }
        if(lock.compareAndSet(false, true)) // 加锁
            try{
                return block() // 处理
            }finally {
                lock.set(false) // 解锁
            }

        return null
    }

}