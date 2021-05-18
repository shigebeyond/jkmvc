package net.jkcode.jkmvc.tests.es

import net.jkcode.jkmvc.es.EsDocRepository
import net.jkcode.jkmvc.tests.entity.MessageEntity
import net.jkcode.jkutil.common.randomInt
import org.junit.Test

class EsDocRepositoryTests {

    val rep = EsDocRepository.instance(MessageEntity::class.java)

    @Test
    fun testSave() {
        val e = buildEntity(1)
        val r = rep.save(e)
        println("插入单个文档: " + r)
    }

    @Test
    fun testSaveAll() {
        val items = ArrayList<MessageEntity>()

        for (i in 1..10) {
            val e = buildEntity(i)
            items.add(e)
        }

        rep.saveAll(items)
        println("批量插入")
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

    @Test
    fun testUpdate() {
        val e = rep.findById("1")!!
        e.fromUid = randomInt(10)
        e.toUid = randomInt(10)
        val r = rep.update(e)
        println("更新文档: " + r)
    }

    // curl 'localhost:9200/esindex/message/1?pretty=true'
    @Test
    fun testFindById() {
        val id = "1"
        val entity = rep.findById(id)
        println("查单个：" + entity.toString())
    }

    @Test
    fun testFindAllByIds() {
        val ids = listOf("1", "2")
        val entities = rep.findAllByIds(ids)
        println("查多个：" + entities)
    }

    @Test
    fun testDeleteById() {
        rep.deleteById("1")
        println("删除id=1文档")
    }

    @Test
    fun testDeleteAll() {
        val pageSize = 5
        val query = rep.queryBuilder()
                .must("fromUid", ">=", 0)
        //val ids = rep.deleteAll(query)
        val ids = query.deleteDocs(pageSize)
        println("删除" + ids.size + "个文档: id in " + ids)
    }

    @Test
    fun testFindAll() {
        val query = rep.queryBuilder()
                //.must("fromUid", ">=", 0)
                .orderByField("id") // 排序
                .limit(10) // 分页
        val (list, size) = rep.findAll(query)
        println("查到 $size 个文档")
        for (item in list)
            println(item)
    }

    @Test
    fun testCount() {
        val query = rep.queryBuilder()
                //.must("fromUid", ">=", 0)
        val size = rep.count(query)
        println("查到 $size 个文档")
    }


    @Test
    fun testScrollAll() {
        val query = rep.queryBuilder()
                //.must("fromUid", ">=", 0)
                .orderByField("id") // 排序
                //.limit(1) // 分页无效, 都是查所有数据
        val list = rep.scrollAll(query, 5)
        println("查到 ${list.size} 个文档")
        for (item in list)
            println(item)
    }

}