package net.jkcode.jkmvc.serialize

import org.nustaq.serialization.FSTConfiguration
import java.io.InputStream

/**
 * 基于fast-serialization的序列化
 *
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-11-10 4:18 PM
 */
class FstSerializer: ISerializer {

    /**
     * 配置
     */
    public val conf = FSTConfiguration.createDefaultConfiguration()

    /**
     * 序列化
     *
     * @param obj
     * @return
     */
    public override fun serialize(obj: Any): ByteArray? {
        return conf.asByteArray(obj)
    }

    /**
     * 反序列化
     *
     * @param bytes
     * @return
     */
    public override fun unserialize(bytes: ByteArray): Any? {
        return conf.getObjectInput(bytes).readObject()
    }

    /**
     * 反序列化
     *
     * @param input
     * @return
     */
    public override fun unserialize(input: InputStream): Any? {
        return conf.getObjectInput(input).readObject()
    }

}