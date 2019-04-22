package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.lock.IKeyLock
import org.junit.Test

/**
 * 测试锁
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-18 5:38 PM
 */
class LockTests {

    @Test
    fun testLock(){
        val lock: IKeyLock = IKeyLock.instance("jedis")
        val key = "test" // 锁的键
        // 加锁
        val locked = lock.quickLock(key, 5)
        if(locked){
            // 业务处理
            println("do sth")

            // 解锁
            lock.unlock(key)
        }

    }

    @Test
    fun testLock2(){
        val lock: IKeyLock = IKeyLock.instance("jedis")
        val key = "test" // 锁的键
        // 加锁, 并自动解锁
        val locked = lock.quickLockCleanly(key, 5){
            // 业务处理
            println("do sth")
        }

    }


}