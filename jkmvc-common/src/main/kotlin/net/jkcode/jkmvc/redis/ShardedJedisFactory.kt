package net.jkcode.jkmvc.redis

import net.jkcode.jkmvc.common.AddressesParser
import net.jkcode.jkmvc.common.Config
import net.jkcode.jkmvc.common.getOrPutOnce
import net.jkcode.jkmvc.ttl.AllRequestScopedTransferableThreadLocal
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

    /********************* 连接池 *******************/
    /**
     * redis连接池
     */
    private val pools: ConcurrentHashMap<String, ShardedJedisPool> = ConcurrentHashMap();

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

    /********************* 线程安全的单例 *******************/
    /**
     * 线程安全的redis连接
     */
    private val jedises: AllRequestScopedTransferableThreadLocal<HashMap<String, ShardedJedis>> = object: AllRequestScopedTransferableThreadLocal<HashMap<String, ShardedJedis>>({HashMap()}){ // 所有请求域的可传递的 ThreadLocal
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
    public fun getConnection(name: String = "default"): ShardedJedis {
        // 获得已有连接
        var jedis:ShardedJedis = jedises.get().getOrPut(name){
            getPool(name).resource
        }
        // 尝试重连
        //jedis.connect()
        return jedis
    }

}