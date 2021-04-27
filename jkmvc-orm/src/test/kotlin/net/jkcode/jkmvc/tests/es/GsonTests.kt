package net.jkcode.jkmvc.tests.es

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import net.jkcode.jkmvc.tests.entity.MessageEntity
import net.jkcode.jkutil.common.randomInt
import org.junit.Test

class GsonTests {

    @Test
    fun testMap() {
        val gsonBuilder = GsonBuilder()
        // es字段命名为: 下划线
        // 生成的json中的字段名, 都是下划线的
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        val gson = gsonBuilder.create()
        val e = mapOf("name" to "shi", "age" to 1)
        val json = gson.toJson(e)
        println(json)

        val e2 = gson.fromJson(json, HashMap::class.java)
        println(e2)
    }

    @Test
    fun testEntity() {
        val gsonBuilder = GsonBuilder()
        // es字段命名为: 下划线
        // 生成的json中的字段名, 都是下划线的
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        val gson = gsonBuilder.create()
        val e = buildEntity(1)
        val json = gson.toJson(e.toMap())
        println(json)

        val e2 = gson.fromJson<MessageEntity>(json, MessageEntity::class.java)
        println(e2)
    }

    private fun buildEntity(i: Int): MessageEntity {
        val e = MessageEntity()
        e.id = i
        e.fromUid = randomInt(10)
        e.toUid = randomInt(10)
        e.content = if(i % 2 == 0) "welcome $i" else "Goodbye $i"
        e.created = i * 60L
        return e
    }

}