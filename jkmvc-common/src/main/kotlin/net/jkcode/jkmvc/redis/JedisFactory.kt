package net.jkcode.jkmvc.redis

import net.jkcode.jkmvc.common.Config
import net.jkcode.jkmvc.common.getOrPutOnce
import net.jkcode.jkmvc.ttl.AllRequestScopedTransferableThreadLocal
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * jedis工厂
 *
 * @author shijianhang
 * @create 2017-10-03 下午9:46
 **/
object JedisFactory: IJedisFactory {

    /********************* 连接池 *******************/
    /**
     * redis连接池
     */
    private val pools: ConcurrentHashMap<String, JedisPool> = ConcurrentHashMap();

    /**
     * 获得连接池
     *
     * @param name
     * @return
     */
    private fun getPool(name:String): JedisPool {
        return pools.getOrPutOnce(name){
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
        val address = config.getString("address")!!
        val (host, port) = address.split(':')
        val pass = config.getString("password")

        // 构建redis连接池配置
        val poolConfig = buildJedisPoolConfig(config)

        // 构建连接池
        return JedisPool(poolConfig, host, port.toInt(), 10000, pass)
    }

    /********************* 线程安全的单例 *******************/
    /**
     * 线程安全的redis连接
     */
    private val jedises: AllRequestScopedTransferableThreadLocal<HashMap<String, Jedis>> = object: AllRequestScopedTransferableThreadLocal<HashMap<String, Jedis>>({HashMap()}){ // 所有请求域的可传递的 ThreadLocal
        public override fun doEndScope() {
            // 请求结束要调用 close() 来关闭连接
            val jedises = get()
            for((name, jedis) in jedises)
                //getPool(name).returnResource(jedis)
                jedis.close()
            jedises.clear()

            super.doEndScope()
        }
    }

    /**
     * 获得redis连接
     *
     * @param name 配置标识
     * @return
     */
    public fun getConnection(name: String = "default"): Jedis {
        // 获得已有连接
        var jedis:Jedis = jedises.get().getOrPut(name){
            getPool(name).resource
        }
        // 尝试重连
        jedis.connect()
        return jedis
    }

}