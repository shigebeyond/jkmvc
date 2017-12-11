package com.jkmvc.cache

import com.jkmvc.common.Config
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * jedis工厂
 *
 * @author shijianhang
 * @create 2017-10-03 下午9:46
 **/
object JedisFactory {

    /**
     * redis连接池
     */
    private val pools: ConcurrentHashMap<String, JedisPool> = ConcurrentHashMap<String, JedisPool>();

    /**
     * 线程安全的redis连接
     */
    private val jedises:ThreadLocal<MutableMap<String, Jedis>> = ThreadLocal.withInitial {
        HashMap<String, Jedis>();
    }

    /**
     * 获得连接池
     *
     * @param name
     * @return
     */
    public fun getPool(name:String): JedisPool {
        return pools.getOrPut(name){
            buildPool(name)
        }
    }

    /**
     * 获得连接池
     *
     * @param name
     * @return
     */
    private fun buildPool(name: String): JedisPool {
        // 获得redis配置
        val config = Config.instance("redis.${name}", "yaml")
        val host = config.getString("host")!!
        val port = config.getInt("port", 6379)!!
        val pass = config.getString("password")

        // 构建redis连接池配置
        val poolConfig = JedisPoolConfig()
        poolConfig.maxTotal = config.getInt("maxTotal", 10)!!
        poolConfig.maxIdle = 5
        poolConfig.maxWaitMillis = (1000 * 10).toLong()
        //在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
        poolConfig.testOnBorrow = true

        return JedisPool(poolConfig, host, port, 10000, pass)
    }

    /**
     * 获得redis连接
     *
     * @param name 配置标识
     * @return
     */
    @Synchronized
    public fun instance(name: String = "default"): Jedis {
        // 获得已有连接
        var jedis:Jedis = jedises.get().getOrPut(name){
            getPool(name).resource
        }
        // 尝试重连
        jedis.connect()
        return jedis
    }

}