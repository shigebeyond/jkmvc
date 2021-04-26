package net.jkcode.jkmvc.tests.es

import net.jkcode.jkmvc.es.ESQueryBuilder
import net.jkcode.jkmvc.es.EsManager
import net.jkcode.jkutil.common.randomBoolean
import net.jkcode.jkutil.common.randomLong
import org.junit.Test
import java.util.*
import kotlin.collections.HashMap


class EsTests3 {

    private val index = "recent_order_index"

    private val type = "_doc"

    private val esmgr = EsManager.instance()

    private val cid = 100

    @Test
    fun testAll() {
        testDeleteIndex()
        testCreateIndex()
        testGetIndex()
        testBulkInsertDocs()
        testRefreshIndex()
        testSearch()
    }

    @Test
    fun testCreateIndex() {
        // gson还是必须用双引号
        var mapping = """{
    '_doc':{
        'properties':{
            'id': {
                'type': 'long'
              },
            'cargoId': {
                'type': 'long'
              },
              'driverUserName': {
                'type': 'keyword'
              },
              'loadAddress': {
                'type': 'text'
              },
              'searchable': {
                'type': 'boolean'
              },
              'companyId': {
                'type': 'long'
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
    fun testInsertDoc() {
        val e = buildEntity(1)
        val r = esmgr.insertDoc(index, type, e)
        println("插入单个文档: " + r)
    }

    @Test
    fun testBulkInsertDocs() {
        val dataList = ArrayList<RecentOrder>()

        for (i in 0 until 5) {
            val e = buildEntity(i)
            dataList.add(e)
        }

        esmgr.bulkInsertDocs(index, type, dataList)
        println("批量插入")
    }

    private fun buildEntity(i: Int): RecentOrder {
        val e = RecentOrder()
        e.id = i.toLong()
        e.cargoId = i.toLong()
        e.driverUserName = arrayOf("张三", "李四", "王五", "赵六", "钱七")[i]
        e.loadAddress = arrayOf("南京市玄武区", "南京市秦淮区", "南京市六合区", "南京市建邺区", "南京市鼓楼区")[i]
        e.searchable = randomBoolean()
        e.companyId = cid + i * 10
        return e
    }

    @Test
    fun testUpdateDoc() {
        val e = esmgr.getDoc(index, type, "1", RecentOrder::class.java)!!
        e.cargoId = randomLong(5)
        e.driverUserName = arrayOf("南京市玄武区", "南京市秦淮区", "南京市六合区", "南京市建邺区", "南京市鼓楼区").random()
        val r = esmgr.updateDoc(index, type, e, "1")
        println("更新文档: " + r)
    }

    // curl 'localhost:9200/esindex/message/1?pretty=true'
    @Test
    fun testGetDoc() {
        val id = "1"
        val entity = esmgr.getDoc(index, type, id, RecentOrder::class.java)
        System.out.println("testGetObject 返回值：" + entity.toString())
    }

    @Test
    fun testMultiGetDoc() {
        val ids = listOf("1", "2")
        val entities = esmgr.multGetDocs(index, type, ids, RecentOrder::class.java)
        System.out.println("testGetObject 返回值：" + entities)
    }

    /*
curl 'localhost:9200/esindex/message/_search?pretty=true'  -H "Content-Type: application/json" -d '
'
     */
    @Test
    fun testSearch() {
        val ts = cid + 120 // 2分钟前
        println("timestamp: $ts")
        val query = ESQueryBuilder()
//                .filter("cargoId", ">=", 7)
                .must("driverUserName", ">=", 8)
                .must("companyId", "<=", 120)
                .must("loadAddress", "like", "Welcome")
                /*.shouldWrap {
                    must("loadAddress", "like", "Welcome")
                    must("companyId", "<=", ts) // 两分钟前发的
                }
                .shouldWrap {
                    must("loadAddress", "like", "Goodbye")
                    must("companyId", ">", ts) // 两分钟内发的
                }*/
                /*.mustWrap {
                    should("companyId", "=", 120)
                    should("cargoId", "=", 8)
                }
                .must("driverUserName", ">=", 8)
                */
                .limit(10)
                .offset(0)
                .orderBy("id")

        val (list, size) = esmgr.searchDocs(index, type, query, RecentOrder::class.java)
        println("查到 $size 个文档")
        for (item in list)
            println(item)
    }

    @Test
    fun testSearch2() {
        val query = ESQueryBuilder()
                .index(index)
                .type(type)
                .mustWrap { // city
                    mustWrap { // start city
                        should("startDistrictId", "IN", arrayOf(684214, 981362))
                        should("startCityId", "IN", arrayOf(320705, 931125))
                    }
                    mustWrap { // end city
                        should("endDistrictId", "IN", arrayOf(95312, 931125))
                        should("endCityId", "IN", arrayOf(589421, 953652))
                    }
                }
                .must("updateTime", ">=", 1608285822239L) // range
                .must("cargoLabels", "IN", arrayOf("水果", "生鲜")) // cargoLabels

                .mustNot("cargoCategory", "IN", arrayOf("A", "B")) // cargoCategory
                .mustNot("featureSort", "=", "好货") // cargoCategory

                .shouldWrap {
                    mustNot("cargoChannel", "IN", arrayOf("长途货源", "一口价货源")) // cargoChannel
                    shouldWrap {
                        must("searchableSources", "=", "ALL") // searchableSources
                        mustWrap { // security
                            must("cargoChannel", "IN", arrayOf("No.1", "No.2", "No.3")) // cargoChannel
                            must("securityTran", "=", "平台保证") // securityTran
                        }
                    }
                }

        query.select("cargoId", "startDistrictId", "startCityId", "endDistrictId", "endCityId", "updateTime", "cargoLabels",
                "cargoCategory", "featureSort", "cargoChannel", "searchableSources", "securityTran")

        query.orderBy("duplicate", "ASC")

        query.toSearchSource()
    }

    @Test
    fun testScroll() {
        val pageSize = 5
        val c = ESQueryBuilder().index(index).type(type).scrollDocs(RecentOrder::class.java, pageSize, 100000)
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
                .addFieldScript("cargoId", "doc['cargoId']+1")
                .filter("cargoId", ">=", 1)
                .limit(10)
        println(query.toSearchSource())
    }

    @Test
    fun testStat() {
        val query = ESQueryBuilder()
        query.aggByAndWrapSubAgg("cargoId") {
            aggByAndWrapSubAgg("driverUserName", null, false) {
                aggBy("count(id)", "nid")
            }
        }
        val result = esmgr.searchDocs(index, type, query)
        println("返回结果:" + result.getJsonString())
        if (result.isSucceeded) {
            // 单值
//            val n = result.aggregations.getValueCountAggregation("nid").getValueCount()
//            println(n)

            // 多值
            val map = result.aggregations.getTermsAggregation("terms_cargoId").buckets.associateTo(LinkedHashMap()) { item ->
                item.key to item.count
            }
            println(map)

            // 二维
            val map2 = LinkedHashMap<String, Any?>()
            for (item1 in result.aggregations.getTermsAggregation("terms_cargoId").buckets) {
                for (item2 in item1.getTermsAggregation("terms_driverUserName").buckets) {
                    map2[item1.key + "-" + item2.key] = item2.count
                }
            }
            println(map2)
        }
    }

}
