package com.jkmvc.common

import java.util.concurrent.ConcurrentHashMap

/**
 * 有名字的单例
 *   可以动态的单例名来获得单例，但是需要实现配置单例名及其实现类
 *   如序列器的配置
 *   <code>
 *      jdk: com.jkmvc.serialize.JdkSerializer
 *      fst: com.jkmvc.serialize.FstSerializer
 *  </code>
 *
 * @author shijianhang
 * @create 2017-12-16 下午3:17
 **/
abstract class NamedSingleton<T> : INamedSingleton<T> {

    /**
     * 单例池
     */
    public val insts: ConcurrentHashMap<String, T> = ConcurrentHashMap();

    /**
     * 根据单例名来获得单例
     *
     * @param name 单例名
     * @return
     */
    public override fun instance(name: String): T{
        return insts.getOrPut(name){
            Class.forName(instsConfig[name]!!).newInstance() as T
        }
    }

}