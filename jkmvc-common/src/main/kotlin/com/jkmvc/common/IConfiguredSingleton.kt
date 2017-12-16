package com.jkmvc.common

/**
 * 配置的单例
 *
 * @author shijianhang
 * @create 2017-12-16 下午3:17
 **/
interface IConfiguredSingleton<T> {
    /**
     * 配置，内容是哈希 <单例名 to 单例类>
     */
    val config: IConfig

    /**
     * 根据单例名来获得单例
     */
    fun instance(name: String): T
}