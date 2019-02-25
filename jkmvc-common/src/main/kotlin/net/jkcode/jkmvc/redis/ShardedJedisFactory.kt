package net.jkcode.jkmvc.redis

import net.jkcode.jkmvc.common.AddressesParser
import net.jkcode.jkmvc.common.Config
import net.jkcode.jkmvc.common.getOrPutOnce
import redis.clients.jedis.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * jedis工厂
 *
 * @author shijianhang
 * @create 2017-10-03 下午9:46
 **/
object ShardedJedisFactory: IJedisFactory {

    /**
     * redis连接池
     */
    private val pools: ConcurrentHashMap<String, ShardedJedisPool> = ConcurrentHashMap();

    /**
     * 线程安全的redis连接
     */
    private val jedises:ThreadLocal<MutableMap<String, ShardedJedis>> = ThreadLocal.withInitial {
        HashMap<String, ShardedJedis>();
    }

    /**
     * 获得连接池
     *
     * @param name
     * @return
     */
    public fun getPool(name:String): ShardedJedisPool {
        return pools.getOrPutOnce(name){
            buildPool(name)
        }
    }

    /**
     * 获得Redis集群的连接池
     * @param name
     * @return
     */
    private fun buildPool(name: String): ShardedJedisPool {
        // 获得redis配置
        val config = Config.instance("redis.${name}", "yaml")
        val addresses = AddressesParser.parse(config.getString("address")!!)
        val pass = config.getString("password")

        // 构建redis连接池配置
        val poolConfig = buildJedisPoolConfig(config)

        // 构建redis节点
        val shards = addresses.map {
            val (host, port) = it
            JedisShardInfo(host, port, 10000)
        }

        // 构建连接池
        return ShardedJedisPool(poolConfig, shards)
    }

    /**
     * 获得redis连接
     *
     * @param name 配置标识
     * @return
     */
    public fun instance(name: String = "default"): ShardedJedis {
        // 获得已有连接
        var jedis:ShardedJedis = jedises.get().getOrPut(name){
            getPool(name).resource
        }
        // 尝试重连
        //jedis.connect()
        return jedis
    }

}