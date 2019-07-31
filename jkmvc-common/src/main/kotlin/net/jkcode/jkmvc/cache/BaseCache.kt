package net.jkcode.jkmvc.cache

import io.netty.util.Timeout
import io.netty.util.TimerTask
import net.jkcode.jkmvc.common.CommonMilliTimer
import net.jkcode.jkmvc.common.trySupplierFuture
import net.jkcode.jkmvc.lock.IKeyLock
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

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
     * @return
     */
    public override fun get(key: Any): Any?{
        val v = doGet(key)
        return if(v == Unit) null else v
    }

    /**
     * 根据键获得值
     *
     * @param key 键
     * @return
     */
    public abstract fun doGet(key: Any): Any?

    /**
     * 设置键值
     *
     * @param key 键
     * @param value 值
     * @param expireSencond 过期秒数
     */
    public override fun put(key: Any, value: Any?, expireSencond:Long):Unit{
        doPut(key, value ?: Unit, expireSencond)
    }

    /**
     * 设置键值
     *
     * @param key 键
     * @param value 值
     * @param expireSencond 过期秒数
     */
    public abstract fun doPut(key: Any, value: Any, expireSencond:Long):Unit

    /**
     * 根据键获得值
     *
     * @param key 键
     * @param expireSeconds 过期秒数
     * @param waitMillis 等待的毫秒数
     * @param dataLoader 回源函数, 兼容函数返回类型是CompletableFuture, 同一个key的并发下只调用一次
     * @return
     */
    public override fun getOrPut(key: Any, expireSeconds:Long, waitMillis:Long, dataLoader: () -> Any?): CompletableFuture<Any?> {
        // 1 尝试读缓存
        val v = doGet(key)
        if (v != null)
            return CompletableFuture.completedFuture(if(v == Unit) null else v)

        // 2 无缓存, 则回源
        val result = CompletableFuture<Any?>()
        // 2.1 锁住key, 则回源, 防止并发回源
        val locked = lock.quickLockCleanly(key){
            // 回源
            trySupplierFuture(dataLoader).whenComplete { r, ex ->
                if(ex != null) {
                    result.completeExceptionally(ex)
                    throw ex
                }

                // 写缓存
                this.put(key, r, expireSeconds)
                result.complete(r)
            }
        }
        // 2.2 锁不住key, 则等待指定毫秒数后读缓存
        if(!locked){
            CommonMilliTimer.newTimeout(object : TimerTask {
                override fun run(timeout: Timeout) {
                    // 读缓存
                    val v = get(key)
                    result.complete(v)
                }
            }, waitMillis, TimeUnit.MILLISECONDS)
        }

        return result
    }

}