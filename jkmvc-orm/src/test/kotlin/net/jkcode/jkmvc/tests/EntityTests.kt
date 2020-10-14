package net.jkcode.jkmvc.tests

import net.jkcode.jkutil.common.*
import net.jkcode.jkmvc.orm.IOrm
import net.jkcode.jkmvc.orm.serialize.toJson
import net.jkcode.jkutil.serialize.ISerializer
import net.jkcode.jkmvc.tests.entity.MessageEntity
import net.jkcode.jkmvc.tests.model.MessageModel
import org.junit.Test
import java.text.MessageFormat

class EntityTests{

    @Test
    fun testJson(){
        val data = listOf(
                mapOf("id" to 1),
                mapOf("id" to 2)
        )
        println(data.toJson())
    }

    @Test
    fun testEntity(){
        val msg = buildEntity()
        println(msg)
    }

    private fun buildEntity(): MessageEntity {
        val msg = MessageEntity()
        msg.fromUid = randomInt(10)
        msg.toUid = randomInt(10)
        msg.content = "hello entity"
        return msg
    }

    private fun buildModel(): MessageModel {
        var msg = MessageModel()
        msg.fromUid = randomInt(10)
        msg.toUid = randomInt(10)
        msg.content = "hello orm"
        return msg
    }

    @Test
    fun testModelPersist(){
        var msg = MessageModel()
        msg.fromUid = randomInt(10)
        msg.toUid = randomInt(10)
        msg.content = "hello orm"
        msg.create()
        val id = msg.id
        println("create: " + msg)

        msg = MessageModel.queryBuilder().where("id", id).findModel<MessageModel>()!!
        println("findRow: " + msg)

        msg.content = "reply orm"
        msg.update()
        println("update: " + msg)

//        msg.delete()
//        println("delete: " + id)
    }

    /**
     * Orm与db相关, 尽量不使用 ISerializer 来序列化, 只序列化OrmEntity就好
     */
    @Test
    fun testModelSerialize(){
        var msg = buildModel()
        // toString()
        println(msg.toString())

        // toMap()
        println(msg.toMap())
    }

    @Test
    fun testEntitySerialize(){
        var msg: MessageEntity = buildEntity()
        println(msg)

        val instance = ISerializer.instance("fst")
        val bs = instance.serialize(msg)
        if(bs != null) {
            val msg2: MessageEntity = instance.unserialize(bs!!) as MessageEntity
            println(msg2)
            println("" + msg2.fromUid + " => " + msg2.toUid + " : " + msg2.content)
        }
    }

    @Test
    fun testModel2Entity(){
        val entity = buildEntity()

        // 实体转模型
        val orm = MessageModel()
        orm.fromEntity(entity)
        orm.save()

        // 模型转实体
        val entity2: MessageEntity = orm.toEntity()
        println(orm)
    }

    /**
     * 测试实体查询
     */
    @Test
    fun testFindEntity(){
        val entities = MessageModel.queryBuilder().findEntity<MessageModel, MessageEntity>()
        println(entities)
    }

    /**
     * 测试实现接口的代理
     */
    @Test
    fun testInterfaceDelegate(){
        val fs = MessageModel::class.java.getInterfaceDelegateFields()
        println(fs)

        val f = MessageModel::class.java.getInterfaceDelegateField(IOrm::class.java)
        println(f)

        val model = MessageModel()
        val dele = model.getInterfaceDelegate(IOrm::class.java)
        println(dele)
    }

    /**
     * 测试实现属性读写的代理
     */
    @Test
    fun testPropDelegate(){
        val fs = MessageEntity::class.java.getPropDelegateFields()
        println(fs)

        val f = MessageEntity::class.java.getPropoDelegateField("id")
        println(f)

        val model = MessageEntity()
        val dele = model.getPropDelegate("id")
        println(dele)
    }

    @Test
    fun testTransformFieldName(){
        val row = mapOf<String, Any>("to_uid" to 4, "id" to 2, "from_uid" to 8, "content" to "hello world")

        // 1 手工转换字段名
        var start = System.nanoTime()
        for(i in 0..1000000) {
            val e = MessageEntity()
            e["toUid"] = row["to_uid"]
            e["id"] = row["id"]
            e["fromUid"] = row["from_uid"]
            e["content"] = row["content"]
        }
        var runTime = (System.nanoTime() - start).toDouble() / 1000000L
        println(MessageFormat.format("Manual transform field name cost {0,number,#.##} ms", runTime))

        // 2 orm自动转换字段名
        // 参考 net.jkcode.jkmvc.orm._OrmKt.entityRowTransformer
        /*val obj = MessageModel()
        start = System.nanoTime()
        for(i in 0..1000000) {
            // 清空字段值
            obj.clear() // 2
            // 设置字段值
            obj.setOriginal(row) // 9
            // 转为实体
            obj.toEntity() // 12
        }
        runTime = (System.nanoTime() - start).toDouble() / 1000000L
        println(MessageFormat.format("Auto transform field name: cost {0,number,#.##} ms", runTime))
        */
    }

}