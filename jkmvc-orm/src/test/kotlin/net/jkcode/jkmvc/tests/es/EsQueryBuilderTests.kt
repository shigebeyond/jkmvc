package net.jkcode.jkmvc.tests.es

import com.google.common.collect.Lists
import net.jkcode.jkmvc.es.ESQueryBuilder
import net.jkcode.jkmvc.es.EsManager
import net.jkcode.jkmvc.tests.entity.RecentOrder
import net.jkcode.jkutil.common.randomBoolean
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.ScriptSortBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
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
        val items = ArrayList<RecentOrder>()

        for (i in 0 until 5) {
            val e = buildEntity(i)
            items.add(e)
        }

        esmgr.bulkIndexDocs(index, type, items)
        println("批量插入")
    }

    private fun buildEntity(i: Int): RecentOrder {
        val e = RecentOrder()
        e.id = i.toLong() + 1
        e.cargoId = i.toLong() + 1
        e.driverUserName = arrayOf("张三", "李四", "王五", "赵六", "钱七")[i]
        e.loadAddress = arrayOf("南京市玄武区", "南京市秦淮区", "南京市六合区", "南京市建邺区", "南京市鼓楼区")[i]
        e.searchable = if(e.driverUserName == "张三") true else randomBoolean()
        e.companyId = cid + i * 10
        return e
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
                /**
                 * http://blog.bootsphp.com/elasticsearch_use_script_fields_with_source
                 * 保留_source字段的+返回脚本字段(script_fields)
                 */
                .select("*")
                .addFieldScript("nextCargoId", "doc['cargoId'].value+params.step", mapOf("step" to 1)) // 带参数的脚本字段
                .limit(10)
        //println(query.toSearchSource())
        // 由于响应的脚本字段在 fields 属性中, 而不是在 _source 中, 因此 searchDocs() 解析出来的字段并不包含脚本字段
        println(query.searchDocs(HashMap::class.java))
    }

    /*
curl 'localhost:9200/recent_order_index/_doc/_search?pretty=true'  -H "Content-Type: application/json" -d '
'
     */
    @Test
    fun testSearch() {
        val query = ESQueryBuilder()
                //.index(index) // 可省略, 因为在 searchDocs(clazz) 会从目标类clazz注解中解析index/type
                //.type(type)
                .must("searchable", "=", true)
                .must("driverUserName", "=", "张三")
                .select("cargoId", "driverUserName", "loadAddress", "companyId")
                .orderByField("id")
                //.limit(10, 30) //
                .limit(10)


        val (list, size) = query.searchDocs(RecentOrder::class.java)
        println("查到 $size 个文档")
        for (item in list)
            println(item)
    }


    @Test
    fun testSearchNative() {
        // 构建query builder
        val queryBuilder = QueryBuilders.boolQuery()
        val searchable = QueryBuilders.termQuery("searchable", true)
        val driverUserName = QueryBuilders.termQuery("driverUserName", "张三")
        queryBuilder.must(searchable).must(driverUserName)

        val searchSource = SearchSourceBuilder()
        searchSource.query(queryBuilder)
        searchSource.fetchSource(arrayOf("cargoId", "driverUserName", "loadAddress", "companyId"), arrayOfNulls(0))
        searchSource.sort("id", SortOrder.ASC)
        //searchSource.from(30)
        searchSource.size(10)

        println(searchSource)
    }

    /**
     * 复杂查询
     *  对比 https://mp.weixin.qq.com/s/GFRiiQEk-JLpPnCi_WrRqw
     *      https://www.infoq.cn/article/u4Xhw5Q3jfLE1brGhtbR
     */
    @Test
    fun testComplexSearch() {
        val query = ESQueryBuilder()
                .index(index)
                .type(type)
                .mustWrap { // city
                    mustWrap { // start city
                        should("startCityId", "IN", arrayOf(320705, 931125))
                        should("startDistrictId", "IN", arrayOf(684214, 981362))
                    }
                    mustWrap { // end city
                        should("endCityId", "IN", arrayOf(589421, 953652))
                        should("endDistrictId", "IN", arrayOf(95312, 931125))
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

        query.orderByField("duplicate")
                .orderByScript("searchCargo-script", mapOf("searchColdCargoTop" to 0))

        query.toSearchSource()
    }

    @Test
    fun testComplexSearchNative() {
        val queryBuilder = QueryBuilders.boolQuery()
        val startCityId = QueryBuilders.termsQuery("startCityId", Lists.newArrayList(320705L, 931125L))
        val startDistrictId = QueryBuilders.termsQuery("startDistrictId", Lists.newArrayList(684214L, 981362L))
        val endCityId = QueryBuilders.termsQuery("endCityId", Lists.newArrayList(589421L, 953652L))
        val endDistrictId = QueryBuilders.termsQuery("endDistrictId", Lists.newArrayList(95312L, 931125L))
        val startBuilder = QueryBuilders.boolQuery()
        startBuilder.should(startCityId).should(startDistrictId)
        val endBuilder = QueryBuilders.boolQuery()
        endBuilder.should(endCityId).should(endDistrictId)
        val cityBuilder = QueryBuilders.boolQuery()
        cityBuilder.must(startBuilder)
        cityBuilder.must(endBuilder)
        queryBuilder.must(cityBuilder)
        val rangeBuilder = QueryBuilders.rangeQuery("updateTime")
        queryBuilder.must(rangeBuilder.from(1608285822239L))
        val cargoLabelsBuilder = QueryBuilders.termsQuery("cargoLabels", Lists.newArrayList("水果", "生鲜"))
        queryBuilder.must(cargoLabelsBuilder)
        val cargoCategoryBuilder = QueryBuilders.termsQuery("cargoCategory", Lists.newArrayList("A", "B"))
        val featureSortBuilder = QueryBuilders.termQuery("featureSort", "好货")
        queryBuilder.mustNot(cargoCategoryBuilder)
        queryBuilder.mustNot(featureSortBuilder)
        val cargoChannelBuilder = QueryBuilders.boolQuery()
        queryBuilder.should(cargoChannelBuilder)
        val channelBuilder = QueryBuilders.termsQuery("cargoChannel", Lists.newArrayList("长途货源", "一口价货源"))
        cargoChannelBuilder.mustNot(channelBuilder)
        val searchableSourcesBuilder = QueryBuilders.boolQuery()
        cargoChannelBuilder.should(searchableSourcesBuilder)
        val sourceBuilder = QueryBuilders.termQuery("searchableSources", "ALL")
        searchableSourcesBuilder.must(sourceBuilder)
        val securityTranBuilder = QueryBuilders.boolQuery()
        searchableSourcesBuilder.must(securityTranBuilder)
        securityTranBuilder.must(QueryBuilders.termsQuery("cargoChannel", "No.1", "No.2", "No.3"))
        securityTranBuilder.must(QueryBuilders.termQuery("securityTran", "平台保证"))

        val searchSource = SearchSourceBuilder()
        searchSource.query(queryBuilder)
        searchSource.fetchSource(arrayOf("cargoId", "startDistrictId", "startCityId", "endDistrictId", "endCityId", "updateTime", "cargoLabels",
                "cargoCategory", "featureSort", "cargoChannel", "searchableSources", "securityTran"), arrayOfNulls(0))
        searchSource.sort("duplicate", SortOrder.ASC)
        val sortBuilder = SortBuilders.scriptSort(Script(ScriptType.INLINE,
                "painless", "searchCargo-script", Collections.emptyMap(), mapOf("searchColdCargoTop" to 0)),
                ScriptSortBuilder.ScriptSortType.STRING).order(SortOrder.ASC)
        searchSource.sort(sortBuilder)

        println(searchSource)
    }

}
