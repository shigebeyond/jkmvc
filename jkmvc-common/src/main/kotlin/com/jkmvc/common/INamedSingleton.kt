package com.jkmvc.common

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
interface INamedSingleton<T> {
    /**
     * 单例类的配置，内容是哈希 <单例名 to 单例类>
     */
    val instsConfig: IConfig

    /**
     * 根据单例名来获得单例
     *
     * @param name 单例名
     * @return
     */
    fun instance(name: String): T
}