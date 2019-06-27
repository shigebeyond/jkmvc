package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.common.randomInt
import net.jkcode.jkmvc.tests.entity.Message
import net.jkcode.jkmvc.tests.model.MessageModel
import org.junit.Test

class EntityTests{

    @Test
    fun testEntity(){
        val msg = buildEntity()
        println(msg)
    }

    private fun buildEntity(): Message {
        val msg = Message()
        msg.fromUid = randomInt(10)
        msg.toUid = randomInt(10)
        msg.content = "hello entity"
        return msg
    }

    @Test
    fun testOrm(){
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
    fun testOrmFromEntity(){
        val entity = buildEntity()
        val orm = MessageModel()
        orm.from(entity)
        orm.save()
        println(orm)
    }

}