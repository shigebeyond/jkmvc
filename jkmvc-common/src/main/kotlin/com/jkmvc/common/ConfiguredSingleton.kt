package com.jkmvc.common

import java.util.concurrent.ConcurrentHashMap

/**
 * 配置的单例
 *
 * @author shijianhang
 * @create 2017-12-16 下午3:17
 **/
abstract class ConfiguredSingleton<T> : IConfiguredSingleton<T> {

    /**
     * 单例池
     */
    public val insts: ConcurrentHashMap<String, T> = ConcurrentHashMap();

    /**
     * 根据单例名来获得单例
     */
    override fun instance(name: String): T{
        return insts.getOrPut(name){
            Class.forName(config[name]!!).newInstance() as T
        }
    }

}