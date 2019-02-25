package net.jkcode.jkmvc.redis

import net.jkcode.jkmvc.common.Config
import redis.clients.jedis.Jedis;

/**
 * jedis单例
 */
private var _inst:Jedis? = null

public fun Jedis.instance(): Jedis {
    if(_inst == null){
        val config = Config.instance("redis")
        //连接redis服务器
        val address = config.getString("address")!!
        val (host, port) = address.split(':')
        val jedis = Jedis(host, port.toInt());
        //权限认证
        val pass = config.getString("password")
        if(pass != null)
            jedis.auth(pass);
        return jedis;
    }
    return _inst!!
}


