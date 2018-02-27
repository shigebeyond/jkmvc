package com.jkmvc.cache

import com.jkmvc.common.Config
import com.jkmvc.serialize.ISerializer
import redis.clients.jedis.Jedis

/**
 * redis做的缓存
 * @author shijianhang
 * @create 2018-02-27 下午7:24
 **/
class JedisCache(protected val name: String = "default"):ICache{

    /**
     * redis配置
     */
    public val config = Config.instance("redis.${name}", "yaml")

    /**
     * 序列化
     */
    public val serializer: ISerializer = ISerializer.instance(config["serializeType"]!!)

    /**
     * redis连接
     */
    private val jedis: Jedis
        get(){
            return JedisFactory.instance(name)
        }


    /**
     * 根据键获得值
     *
     * @param key 键
     * @return
     */
    public override fun get(key: Any): Any? {
        val value = jedis.get(serializer.serialize(key))
        return serializer.unserizlize(value)
    }

    /**
     * 设置键值
     *
     * @param key 键
     * @param value 值
     * @param expires 过期时间（秒）
     */
    public override fun put(key: Any, value: Any, expires:Int):Unit {
        //jedis.set(key.toString(), value.toString(), "NX", "EX", expires)
        jedis.set(serializer.serialize(key), serializer.serialize(value), "NX".toByteArray(), "EX".toByteArray(), expires)
    }

    /**
     * 删除指定的键的值
     * @param key 要删除的键
     */
    public override fun remove(key: Any):Unit {
        jedis.del(serializer.serialize(key))
    }

    /**
     * 清空缓存
     */
    public override fun clear():Unit {
        jedis.flushAll()
    }

}