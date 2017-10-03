package com.jkmvc.session

import com.jkmvc.common.Config
import redis.clients.jedis.Jedis
import java.util.concurrent.ConcurrentHashMap

/**
 * jedis工厂
 *
 * @author shijianhang
 * @create 2017-10-03 下午9:46
 **/
object JedisFactory {

    /**
     * 缓存redis连接
     */
    private val jedises: ConcurrentHashMap<String, Jedis> by lazy {
        ConcurrentHashMap<String, Jedis>();
    }

    /**
     * 获得redis连接
     *
     * @param name 配置标识
     * @return
     */
    @Synchronized
    public fun instance(name: String = "default"): Jedis {
        return jedises.getOrPut(name){
            buildJedis(name)
        }
    }

    /**
     * 构建redis连接
     *
     * @param name 配置标识
     * @return
     */
    private fun buildJedis(name: String): Jedis {
        val config: Config = Config.instance("redis.$name", "yaml")
        //连接redis服务器
        val host = config.getString("host")!!
        val port = config.getInt("port", 6379)!!
        val jedis = Jedis(host, port);
        //权限认证
        val pass = config.getString("password")
        if (pass != null)
            jedis.auth(pass);
        return jedis;
    }
}