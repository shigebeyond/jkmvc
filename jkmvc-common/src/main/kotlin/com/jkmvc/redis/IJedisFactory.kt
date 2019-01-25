package com.jkmvc.redis

import com.jkmvc.common.Config
import redis.clients.jedis.JedisPoolConfig

/**
 * redis工厂基类
 * @author shijianhang<772910474@qq.com>
 * @date 2019-01-25 5:30 PM
 */
internal interface IJedisFactory {

    /**
     * 构建redis连接池配置
     * @return
     */
    fun buildJedisPoolConfig(config: Config): JedisPoolConfig {
        val poolConfig = JedisPoolConfig()
        poolConfig.maxTotal = config.getInt("maxTotal", 100)!! // 最大连接数，与并发线程数一致
        poolConfig.maxIdle = config.getInt("maxIdle", 10)!! // 最大空闲连接数
        poolConfig.minIdle = config.getInt("minIdle", 5)!! // 最小空闲连接数
        poolConfig.maxWaitMillis = config.getLong("maxWaitMillis", 1000 * 10)!! // 获取连接时的最大等待毫秒数(如果设置为阻塞时BlockWhenExhausted),如果超时就抛异常, 小于零:阻塞不确定的时间,  默认-1
        poolConfig.testOnBorrow = true // 在获取连接的时候检查有效性, 默认false
        poolConfig.testOnReturn = true // 调用returnObject方法时，是否进行有效检查
        poolConfig.testWhileIdle = true // Idle时进行连接扫描
        poolConfig.timeBetweenEvictionRunsMillis = config.getLong("timeBetweenEvictionRunsMillis", 30000)!! //表示idle object evitor两次扫描之间要sleep的毫秒数
        poolConfig.numTestsPerEvictionRun = config.getInt("numTestsPerEvictionRun", 10)!! //表示idle object evitor每次扫描的最多的对象数
        poolConfig.minEvictableIdleTimeMillis = config.getLong("minEvictableIdleTimeMillis", 60000)!! //表示一个对象至少停留在idle状态的最短时间，然后才能被idle object evitor扫描并驱逐；这一项只有在timeBetweenEvictionRunsMillis大于0时才有意义
        return poolConfig
    }
}