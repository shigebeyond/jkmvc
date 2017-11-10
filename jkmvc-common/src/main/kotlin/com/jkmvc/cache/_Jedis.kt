package com.jkmvc.cache

import com.jkmvc.common.Config
import redis.clients.jedis.Jedis;

/**
 * jedis单例
 */
private var _inst:Jedis? = null

public fun Jedis.instance(): Jedis {
    if(_inst == null){
        val config = Config.instance("redis")
        //连接redis服务器
        val host = config.getString("host")!!
        val port = config.getInt("port", 6379)!!
        val jedis = Jedis(host, port);
        //权限认证
        val pass = config.getString("password")
        if(pass != null)
            jedis.auth(pass);
        return jedis;
    }
    return _inst!!
}


