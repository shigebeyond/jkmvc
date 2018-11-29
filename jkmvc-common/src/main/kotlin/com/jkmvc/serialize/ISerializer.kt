package com.jkmvc.serialize

import com.jkmvc.common.Config
import com.jkmvc.common.IConfig
import com.jkmvc.common.NamedSingleton
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * 序列器
 *
 * @author shijianhang
 * @create 2017-10-04 下午3:29
 **/
interface ISerializer {

    // 可配置的单例
    companion object: NamedSingleton<ISerializer>() {
        /**
         * 配置，内容是哈希 <单例名 to 单例类>
         */
        public override val config: IConfig = Config.instance("serializer", "yaml")
    }

    /**
     * 序列化
     *
     * @param obj
     * @return
     */
    fun serialize(obj: Any): ByteArray?

    /**
     * 反序列化
     *
     * @param bytes
     * @return
     */
    fun unserizlize(bytes: ByteArray): Any? {
        return unserizlize(ByteArrayInputStream(bytes))
    }

    /**
     * 反序列化
     *
     * @param input
     * @return
     */
    fun unserizlize(input: InputStream): Any?
}