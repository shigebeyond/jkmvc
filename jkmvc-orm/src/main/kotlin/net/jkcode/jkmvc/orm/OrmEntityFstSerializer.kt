package net.jkcode.jkmvc.orm

import org.nustaq.serialization.FSTBasicObjectSerializer
import org.nustaq.serialization.FSTClazzInfo
import org.nustaq.serialization.FSTObjectInput
import org.nustaq.serialization.FSTObjectOutput
import org.nustaq.serialization.serializers.FSTMapSerializer

/**
 * ORM之实体对象的序列器
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 */
class OrmEntityFstSerializer : FSTBasicObjectSerializer() {

    /**
     * map的序列器
     */
    protected val mapSerializer: FSTMapSerializer = FSTMapSerializer()

    /**
     * 写
     */
    public override fun writeObject(out: FSTObjectOutput, toWrite: Any, clzInfo: FSTClazzInfo, referencedBy: FSTClazzInfo.FSTFieldInfo, streamPosition: Int) {
        val entity = toWrite as OrmEntity
        // 写 OrmEntity.data
        mapSerializer.writeObject(out, entity.getData(), clzInfo, referencedBy, streamPosition)
    }

    /**
     * 读
     */
    public override fun instantiate(objectClass: Class<*>, `in`: FSTObjectInput, serializationInfo: FSTClazzInfo, referencee: FSTClazzInfo.FSTFieldInfo, streamPosition: Int): Any {
        val data = mapSerializer.instantiate(HashMap::class.java, `in`, serializationInfo, referencee, streamPosition) as HashMap<String, Any?>
        val entity = objectClass.newInstance() as OrmEntity
        entity.getData().putAll(data)
        return entity
    }
}
