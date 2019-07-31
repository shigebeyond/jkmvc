package net.jkcode.jkmvc.cache

import net.jkcode.jkmvc.common.Config
import net.jkcode.jkmvc.common.IConfig
import net.jkcode.jkmvc.singleton.NamedConfiguredSingletons
import java.util.concurrent.CompletableFuture

/**
 * 缓存操作接口
 *
 * @author shijianhang
 * @create 2018-02-27 下午8:20
 **/
interface ICache {

    // 可配置的单例
    companion object: NamedConfiguredSingletons<ICache>() {
        /**
         * 单例类的配置，内容是哈希 <单例名 to 单例类>
         */
        public override val instsConfig: IConfig = Config.instance("cache", "yaml")
    }

    /**
     * 根据键获得值
     *
     * @param key 键
     * @return
     */
    fun get(key: Any): Any?

    /**
     * 根据键获得值
     *
     * @param key 键
     * @param expireSeconds 过期秒数
     * @param waitMillis 等待的毫秒数
     * @param dataLoader 回源函数, 兼容函数返回类型是CompletableFuture, 同一个key的并发下只调用一次
     * @return
     */
    fun getOrPut(key: Any, expireSeconds:Long, waitMillis:Long = 200, dataLoader: () -> Any?): CompletableFuture<Any?>

    /**
     * 设置键值
     *
     * @param key 键
     * @param value 值
     * @param expireSencond 过期秒数
     */
    fun put(key: Any, value: Any?, expireSencond:Long):Unit

    /**
     * 删除指定的键的值
     * @param key 要删除的键
     */
    fun remove(key: Any):Unit

    /**
     * 清空缓存
     */
    fun clear():Unit

}
