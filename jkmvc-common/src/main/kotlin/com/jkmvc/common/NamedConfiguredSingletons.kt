package com.jkmvc.common

/**
 * 名字可配的单例池
 *   可以动态的单例名来获得单例，但是需要实现配置单例名及其实现类
 *   如序列器的配置
 *   <code>
 *      jdk: com.jkmvc.serialize.JdkSerializer
 *      fst: com.jkmvc.serialize.FstSerializer
 *  </code>
 *  一般用在枚举某个接口的实现类
 *
 * @author shijianhang
 * @create 2017-12-16 下午3:17
 **/
abstract class NamedConfiguredSingletons<T> : INamedConfiguredSingletons<T> {

    /**
     * 根据单例名来获得单例
     *
     * @param name 单例名
     * @return
     */
    public override fun instance(name: String): T{
        val clazz: String = instsConfig[name]!!
        return BeanSingletons.instance(clazz) as T
    }

}