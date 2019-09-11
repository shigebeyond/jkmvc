package net.jkcode.jkmvc.serialize

import org.nustaq.serialization.FSTConfiguration
import org.nustaq.serialization.FSTObjectSerializer
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
    protected val conf = FSTConfiguration.createDefaultConfiguration()

    /**
     * 给特定类指定序列器
     * @param cl 类
     * @param ser 序列器
     * @param includeSubclasses 是否包含子类
     */
    public fun putSerializer(cl: Class<*>, ser: FSTObjectSerializer, includeSubclasses: Boolean) {
        val reg = conf.getCLInfoRegistry().getSerializerRegistry()
        reg.putSerializer(cl, ser, includeSubclasses)
    }

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