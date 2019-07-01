package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.common.randomInt
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

    @Test
    fun testOrmPersist(){
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

    @Test
    fun testOrmSerialize(){
        var msg = MessageModel()
        msg.fromUid = randomInt(10)
        msg.toUid = randomInt(10)
        msg.content = "hello orm"
        // toString()
        println(msg.toString())
        // toMap()
        println(msg.toMap())
    }

    @Test
    fun testOrmFromEntity(){
        val entity = buildEntity()
        val orm = MessageModel()
        orm.from(entity)
        orm.save()
        println(orm)
    }

}