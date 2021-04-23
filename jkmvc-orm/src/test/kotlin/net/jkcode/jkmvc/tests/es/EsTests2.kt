package net.jkcode.jkmvc.tests.es

import com.alibaba.fastjson.JSON
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import net.jkcode.jkmvc.es.ESQueryBuilder
import net.jkcode.jkmvc.es.EsManager
import net.jkcode.jkmvc.tests.entity.MessageEntity
import net.jkcode.jkutil.common.randomBoolean
import net.jkcode.jkutil.common.randomInt
import net.jkcode.jkutil.common.toDate
import org.joda.time.DateTime
import org.junit.Test
import java.util.*
import kotlin.collections.HashMap


class EsTests2 {

    private val index = "esindex"

    private val type = "message"

    private val esmgr = EsManager.instance()

    private val time = "2021-04-15".toDate().time / 1000

    @Test
    fun testGson() {
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

    @Test
    fun testAll() {
        testDeleteIndex()
        testCreateIndex()
        testGetIndex()
        testBulkInsertDocs()
        testSearch()
    }

    @Test
    fun testCreateIndex() {
        // gson还是必须用双引号
        var mapping = """{
    'message':{
        'properties':{
            'id':{
                'type':'long'
            },
            'fromUid':{
                'type':'long'
            },
            'toUid':{
                'type':'long'
            },
            'content':{
                'type':'text'
            },
            'created':{
                'type':'long'
            },
        }
    }
}"""
        // gson还是必须用双引号
        mapping = mapping.replace('\'', '"')
        var r = esmgr.createIndex(index)
        println("创建索引[$index]: " + r)
        r = esmgr.putMapping(index, type, mapping)
        println("设置索引[$index]映射[$type]: " + r)
    }

    @Test
    fun testGetIndex() {
        val setting = esmgr.getSetting(index)
        println("----------- setting ----------")
        println(setting)
        val mapping = esmgr.getMapping(index, type)
        println("----------- mapping ----------")
        println(mapping)
    }

    @Test
    fun testIndexExist() {
        val r = esmgr.indexExist(index)
        System.out.println("索引[$index]是否存在：" + r)
    }

    @Test
    fun testAddAliases() {
        val list = Arrays.asList(index)
        esmgr.addIndexAlias(list, "esindex_alias")
        this.testGetIndexAliases()
    }

    @Test
    fun testGetIndexAliases() {
        esmgr.getIndexAliases(index)
    }

    @Test
    fun testRefreshIndex() {
        esmgr.refreshIndex(index)
    }

    @Test
    fun testDeleteIndex() {
        //删除
        val r = esmgr.deleteIndex(index)
        System.out.println("删除索引[$index]：" + r)
    }

    @Test
    fun testInsertDoc() {
        val e = buildEntity(1)
        val r = esmgr.insertDoc(index, type, e)
        println("插入单个文档: " + r)
    }

    @Test
    fun testBulkInsertDocs() {
        val dataList = ArrayList<MessageEntity>()

        for (i in 1..10) {
            val e = buildEntity(i)
            dataList.add(e)
        }

        esmgr.bulkInsertDocs(index, type, dataList)
        println("批量插入")
    }

    private fun buildEntity(i: Int): MessageEntity {
        val e = MessageEntity()
        e.id = i
        e.fromUid = randomInt(10)
        e.toUid = randomInt(10)
        e.content = if(i % 2 == 0) "welcome $i" else "Goodbye $i"
        e.created = time + i * 60
        return e
    }

    @Test
    fun testUpdateDoc() {
        val e = esmgr.getDoc(index, type, "1", MessageEntity::class.java)!!
        e.fromUid = randomInt(10)
        e.toUid = randomInt(10)
        val r = esmgr.updateDoc(index, type, "1", e)
        println("更新文档: " + r)
    }

    // curl 'localhost:9200/esindex/message/1?pretty=true'
    @Test
    fun testGetDoc() {
        val id = "1"
        val entity = esmgr.getDoc(index, type, id, MessageEntity::class.java)
        System.out.println("testGetObject 返回值：" + entity.toString())
    }

    @Test
    fun testMultiGetDoc() {
        val ids = listOf("1", "2")
        val entities = esmgr.multGetDocs(index, type, ids, MessageEntity::class.java)
        System.out.println("testGetObject 返回值：" + entities)
    }

    /*
curl 'localhost:9200/esindex/message/_search?pretty=true'  -H "Content-Type: application/json" -d '
'
     */
    @Test
    fun testSearch() {
        val ts = time + 120 // 2分钟前
        val query = ESQueryBuilder()
                .filter("fromUid", ">=", 1)
                .must("toUid", ">=", 1)
                .shouldWrap {
                    filterWrap {
                        must("content", "like", "Welcome")
                        must("created", "<=", ts) // 两分钟前发的
                    }
                    filterWrap {
                        must("content", "like", "Goodbye")
                        must("created", ">", ts) // 两分钟内发的
                    }
                }
                .limit(10)
                .offset(0)
                .orderBy("id")

        val (list, size) = esmgr.searchDocs(index, type, query, MessageEntity::class.java)
        println("查到 $size 个文档")
        for (item in list)
            println(item)
    }

    @Test
    fun testSearch2() {
        val (list, size) = ESQueryBuilder().index(index).type(type).searchDocs(HashMap::class.java)
        println("查到 $size 个文档")
        for (item in list)
            println(item)
    }

    @Test
    fun testScroll() {
        val pageSize = 5
        val c = ESQueryBuilder().index(index).type(type).scrollDocs(MessageEntity::class.java, pageSize, 100000)
        val times = c.size / pageSize + 1
        println("记录数=${c.size}, 每次取=$pageSize, 取次数=$times")
        for (item in c)
            println(item)
    }

    @Test
    fun testDeleteDoc() {
        esmgr.deleteDoc(index, type, "1")
        println("删除id=1文档")
    }

    @Test
    fun testSearchDeleteDoc() {
        val pageSize = 5
        val ids = ESQueryBuilder().index(index).type(type).deleteDocs(pageSize, 100000)
        println("删除" + ids.size + "个文档: id in " + ids)
    }

    @Test
    fun testScript() {
        val query = ESQueryBuilder().index(index).type(type)
                .addFieldScript("fromUid", "doc['fromUid']+1")
                .filter("fromUid", ">=", 1)
                .limit(10)
        println(query.toSearchSource())
    }

    @Test
    fun testStat() {
        val query = ESQueryBuilder()
        query.filter("currentStatus", 1)
                .filter("currentDealUserId", "1500")
                .aggBy("deptId")
                .aggBy("count(id)", "nid")

        val result = esmgr.searchDocs(index, type, query)
        println("返回结果:" + result.getJsonString())
        if (result.isSucceeded) {
            // 单值
            val n = result.aggregations.getValueCountAggregation("nid").getValueCount()

            // 多值
            val map = result.aggregations.getTermsAggregation("nid").buckets.associate { item ->
                item.key to item.count
            }
        }
    }


    @Test
    fun testStat2() {
        //多级别统计,这种情况，只能返回 result 自己处理了
        val query = ESQueryBuilder()
        //加上时间过滤
        val start = DateTime().plusDays(-10)
        query.filterBetween("createDate", start.toDate().time, DateTime().toDate().time)

        query.aggBy("sheetTypeOne").aggBy("sheetTypeTwo", "agg")
        val result = esmgr.searchDocs(index, type, query)
        println("返回结果:" + JSON.toJSONString(result))
        if (result.isSucceeded) {
            // 多值
            val map = result.aggregations.getTermsAggregation("nid").buckets.forEach { item ->
                println("两级分组统计返回：" + item.key + "---" + JSON.toJSONString(item))
            }
        }
    }
}
