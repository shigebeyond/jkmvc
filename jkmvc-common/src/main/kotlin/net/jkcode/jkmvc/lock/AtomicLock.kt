package net.jkcode.jkmvc.lock

import java.util.concurrent.atomic.AtomicBoolean

/**
 * 使用AtomicBoolean作为锁
 *    false是没锁, true为有锁
 *    AtomicBoolean对象要初始化为false
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-17 9:38 AM
 */
class AtomicLock {

    /**
     * 限制flush()并发的锁
     */
    protected val lock: AtomicBoolean = AtomicBoolean(false)

    /**
     * 快速加锁, 锁不住不等待
     *
     * @param block 处理
     * @return
     */
    public inline fun quickLockCleanly(block: () -> Unit): Boolean {
        val locked = lock.compareAndSet(false, true)
        if(locked){
            try{
                block()
            }finally {
                lock.set(false)
            }
        }
        return locked
    }
}