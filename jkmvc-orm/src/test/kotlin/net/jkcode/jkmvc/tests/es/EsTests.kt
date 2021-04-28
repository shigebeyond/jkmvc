package net.jkcode.jkmvc.tests.es

import io.searchbox.core.search.aggregation.Bucket
import net.jkcode.jkmvc.es.ESQueryBuilder
import net.jkcode.jkmvc.es.EsManager
import net.jkcode.jkmvc.es.annotation.esIdProp
import net.jkcode.jkmvc.es.flattenAggRows
import net.jkcode.jkmvc.tests.entity.MessageEntity
import net.jkcode.jkmvc.tests.model.MessageModel
import net.jkcode.jkutil.common.randomInt
import org.junit.Test
import java.util.*
import kotlin.collections.HashMap


class EsTests {

    private val index = "message_index"

    private val type = "_doc"

    private val esmgr = EsManager.instance()

    //private val time = "2021-04-15".toDate().time / 1000
    private val time = 0L

    @Test
    fun testEsId() {
        val prop = MessageModel::class.esIdProp
        println("esid prop: " + prop?.name)
    }

    @Test
    fun testAll() {
        testDeleteIndex()
        testCreateIndex()
        testGetIndex()
        testBulkIndexDocs()
        testRefreshIndex()
        testSearch()
    }

    @Test
    fun testCreateIndex() {
        // gson还是必须用双引号
        var mapping = """{
    '_doc':{
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
            }
        }
    }
}"""
        // gson还是必须用双引号
        mapping = mapping.replace('\'', '"')
        println(mapping)
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
    fun testIndexDoc() {
        val e = buildEntity(1)
        val r = esmgr.indexDoc(index, type, e)
        println("插入单个文档: " + r)
    }

    @Test
    fun testBulkIndexDocs() {
        val items = ArrayList<MessageEntity>()

        for (i in 1..10) {
            val e = buildEntity(i)
            items.add(e)
        }

        esmgr.bulkIndexDocs(index, type, items)
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
        val r = esmgr.updateDoc(index, type, e, "1")
        println("更新文档: " + r)
    }

    @Test
    fun testPartUpdateDoc() {
        val r = esmgr.updateDoc(index, type, mapOf("name" to "shi"), "1")
        println("部分更新文档: " + r)
    }

    // curl 'localhost:9200/esindex/message/1?pretty=true'
    @Test
    fun testGetDoc() {
        val id = "1"
        val entity = esmgr.getDoc(index, type, id, MessageEntity::class.java)
        System.out.println("查单个：" + entity.toString())
    }

    @Test
    fun testMultiGetDoc() {
        val ids = listOf("1", "2")
        val entities = esmgr.multGetDocs(index, type, ids, MessageEntity::class.java)
        System.out.println("查多个：" + entities)
    }

    /*
curl 'localhost:9200/message_index/_doc/_search?pretty=true'  -H "Content-Type: application/json" -d '
'
     */
    @Test
    fun testSearch() {
        val ts = time + 120 // 2分钟前
        println("timestamp: $ts")
        val query = ESQueryBuilder()
//                .filter("fromUid", ">=", 7)
                .must("toUid", ">=", 8)
                .must("created", "<=", 120)
                .must("content", "like", "Welcome")
                /*.shouldWrap {
                    must("content", "like", "Welcome")
                    must("created", "<=", ts) // 两分钟前发的
                }
                .shouldWrap {
                    must("content", "like", "Goodbye")
                    must("created", ">", ts) // 两分钟内发的
                }*/
                /*.mustWrap {
                    should("created", "=", 120)
                    should("fromUid", "=", 8)
                }
                .must("toUid", ">=", 8)
                */
                .limit(10)
                .offset(0)
                .orderByField("id")

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
        query.aggByAndWrapSubAgg("fromUid") {
            aggByAndWrapSubAgg("toUid", null) {
                aggBy("count(id)", "nid")
            }
        }
        val result = esmgr.searchDocs(index, type, query)
        println("返回结果:" + result.getJsonString())
        if (result.isSucceeded) {
            // 单值
//            val n = result.aggregations.getValueCountAggregation("nid").getValueCount()
//            println(n)

            // 一层
            /*val map = result.aggregations.getTermsAggregation("fromUid").buckets.associateTo(LinkedHashMap()) { item ->
                item.key to item.count
            }
            println(map)*/
            /*val rows = result.extractAggRows("fromUid"){
                it.count
            }
            println(rows)*/

            println("----------------")

            // 二层
            /*val map2 = LinkedHashMap<String, Any?>()
            for(item1 in result.aggregations.getTermsAggregation("fromUid").buckets){
                for(item2 in item1.getTermsAggregation("toUid").buckets){
                    map2[item1.key + "-" + item2.key] = item2.count
                }
            }
            println(map2)
            */
            val rows = result.aggregations.flattenAggRows("fromUid.toUid")
            println(rows)
            for (row in rows){
                print(row["fromUid"].toString() + "-" + row["toUid"] + "=" + row["nid"] + ", ")
            }
        }
    }

}
