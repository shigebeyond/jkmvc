package net.jkcode.jkmvc.tests.es

import net.jkcode.jkmvc.es.ESQueryBuilder
import net.jkcode.jkmvc.es.EsManager
import net.jkcode.jkutil.common.randomBoolean
import net.jkcode.jkutil.common.randomLong
import org.junit.Test
import java.util.*


class EsQueryBuilderTests {

    private val index = "recent_order_index"

    private val type = "_doc"

    private val esmgr = EsManager.instance()

    private val cid = 100

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
    fun testBulkIndexDocs() {
        val dataList = ArrayList<RecentOrder>()

        for (i in 0 until 5) {
            val e = buildEntity(i)
            dataList.add(e)
        }

        esmgr.bulkIndexDocs(index, type, dataList)
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

    /*
curl 'localhost:9200/recent_order_index/_doc/_search?pretty=true'  -H "Content-Type: application/json" -d '
'
     */
    @Test
    fun testSearch() {
        val query = ESQueryBuilder()
                .index(index)
                .type(type)
                .must("searchable", "=", true)
                .must("driverUserName", "=", "张三")
                .limit(10)
                .offset(0)
                .orderByField("id")

        val (list, size) = query.searchDocs(RecentOrder::class.java)
        println("查到 $size 个文档")
        for (item in list)
            println(item)
    }

    /**
     * 复杂查询
     *  对比 https://mp.weixin.qq.com/s/GFRiiQEk-JLpPnCi_WrRqw
     *      https://www.infoq.cn/article/u4Xhw5Q3jfLE1brGhtbR
     */
    @Test
    fun testComplexQuery() {
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

        query.orderByField("duplicate", "ASC")
                .orderByScript("searchCargo-script", mapOf("searchColdCargoTop" to 0))

        query.toSearchSource()
    }

}
