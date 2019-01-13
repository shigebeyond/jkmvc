package com.jkmvc.cache

import com.jkmvc.common.Config
import com.jkmvc.common.IConfig
import com.jkmvc.common.NamedSingleton

/**
 * 缓存操作接口
 *
 * @author shijianhang
 * @create 2018-02-27 下午8:20
 **/
interface ICache {

    // 可配置的单例
    companion object: NamedSingleton<ICache>() {
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
    operator fun get(key: Any): Any?

    /**
     * 设置键值
     *
     * @param key 键
     * @param value 值
     * @param expires 过期时间（秒）
     */
    fun put(key: Any, value: Any, expires:Int):Unit

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
