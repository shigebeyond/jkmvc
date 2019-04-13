package net.jkcode.jkmvc.common

/**
 * 针对不同key加锁
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-06 8:54 AM
 */
interface IKeyLock {

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