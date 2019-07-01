package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.common.randomInt
import net.jkcode.jkmvc.orm.OrmEntity
import net.jkcode.jkmvc.serialize.ISerializer
import net.jkcode.jkmvc.tests.entity.MessageEntity
import net.jkcode.jkmvc.tests.model.MessageModel
import org.junit.Test

class EntityTests{

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
        println("find: " + msg)

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
        var msg = buildEntity()
        println(msg)

        val instance = ISerializer.instance("fst")
        val bs = instance.serialize(msg)
        if(bs != null) {
            val msg2 = instance.unserizlize(bs!!)
            println(msg2)
        }
    }

    @Test
    fun testModelFromEntity(){
        val entity = buildEntity()
        val orm = MessageModel()
        orm.from(entity)
        orm.save()
        println(orm)
    }

}