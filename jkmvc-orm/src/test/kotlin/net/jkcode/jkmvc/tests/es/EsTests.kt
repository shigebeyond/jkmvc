package net.jkcode.jkmvc.tests.es

import com.alibaba.fastjson.JSON
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import net.jkcode.jkmvc.es.ESQueryBuilder
import net.jkcode.jkmvc.es.EsManager
import net.jkcode.jkutil.common.randomString
import org.joda.time.DateTime
import org.junit.Test
import java.util.*
import kotlin.collections.HashMap


class EsTests {

    private val index = "index-workorder"

    private val type = "worksheet"

    private val esmgr = EsManager.instance()

    @Test
    fun testAll() {
        testDeleteIndex()
        testCreateIndex()
        testGetIndex()
        testBulkInsertDocs()
        testUpdateDoc()
    }

    @Test
    fun testGson() {
        val gsonBuilder = GsonBuilder()
        // es字段命名为: 下划线
        // 生成的json中的字段名, 都是下划线的
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        val gson = gsonBuilder.create()
        val e = buildEntity(1)
        val json = gson.toJson(e)
        println(json)

        val e2 = gson.fromJson<WorkSheet>(json, WorkSheet::class.java)
        println(e2)
    }

    @Test
    fun testCreateIndex() {
        // gson还是必须用双引号
        var mapping = """{
    'worksheet':{
        'properties':{
            'call_record_id':{
                'type':'long'
            },
            'city_id':{
                'type':'long'
            },
            'commit_user_id':{
                'type':'keyword'
            },
            'commit_user_name':{
                'type':'keyword'
            },
            'confirm_sheet_type_four':{
                'type':'long'
            },
            'confirm_sheet_type_one':{
                'type':'long'
            },
            'confirm_sheet_type_three':{
                'type':'long'
            },
            'confirm_sheet_type_two':{
                'type':'long'
            },
            'contact':{
                'type':'keyword'
            },
            'create_date':{
                'type':'date',
                'format':'yyyy-MM-dd HH:mm:ss'
            },
            'current_deal_user_id':{
                'type':'keyword'
            },
            'current_deal_user_name':{
                'type':'keyword'
            },
            'current_status':{
                'type':'long'
            },
            'dept_id':{
                'type':'long'
            },
            'driver_id':{
                'type':'long'
            },
            'driver_name':{
                'type':'keyword'
            },
            'driver_phone':{
                'type':'keyword'
            },
            'duty_dept':{
                'type':'long'
            },
            'handle_time':{
                'type':'long'
            },
            'id':{
                'type':'long'
            },
            'license_plates':{
                'type':'keyword'
            },
            'memo':{
                'type':'keyword'
            },
            'order_no':{
                'type':'keyword'
            },
            'order_type':{
                'type':'long'
            },
            'reopen_times':{
                'type':'long'
            },
            'rider_name':{
                'type':'keyword'
            },
            'rider_phone':{
                'type':'keyword'
            },
            'service_type_id':{
                'type':'long'
            },
            'sheet_classify':{
                'type':'long'
            },
            'sheet_priority':{
                'type':'long'
            },
            'sheet_source':{
                'type':'long'
            },
            'sheet_tag':{
                'type':'long'
            },
            'sheet_tag_sort':{
                'type':'long'
            },
            'sheet_type_four':{
                'type':'long'
            },
            'sheet_type_one':{
                'type':'long'
            },
            'sheet_type_three':{
                'type':'long'
            },
            'sheet_type_two':{
                'type':'long'
            },
            'update_date':{
                'type':'date',
                'format':'yyyy-MM-dd HH:mm:ss'
            },
            'urge_times':{
                'type':'long'
            },
            'weight':{
                'type':'long'
            },
            'work_sheet_no':{
                'type':'keyword'
            }
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
        esmgr.addIndexAlias(list, "index-workorder_alias")
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
        val dataList = ArrayList<WorkSheet>()

        for (i in 1..10) {
            val e = buildEntity(i)
            dataList.add(e)
        }

        esmgr.bulkInsertDocs(index, type, dataList)
        println("批量插入")
    }

    private fun buildEntity(i: Int): WorkSheet {
        val e = WorkSheet()
        e.id = i
        e.workSheetNo = randomString(5)
        e.sheetTypeTwo = 2
        e.attentionUserIds = randomString(5)
        e.createDate = Date(System.currentTimeMillis())
        return e
    }

    @Test
    fun testUpdateDoc() {
        val e = esmgr.getDoc(index, type, "1", WorkSheet::class.java)!!
        e.workSheetNo = randomString(5)
        e.updateDate = Date(System.currentTimeMillis())
        e.urgeTimes = 2
        e.reopenTimes = 3
        val r = esmgr.updateDoc(index, type, "1", e)
        println("更新文档: " + r)
    }

    // curl 'localhost:9200/index-workorder/worksheet/1?pretty=true'
    @Test
    fun testGetDoc() {
        val id = "1"
        val entity = esmgr.getDoc(index, type, id, WorkSheet::class.java)
        System.out.println("testGetObject 返回值：" + entity.toString())
    }

    @Test
    fun testMultiGetDoc() {
        val ids = listOf("1", "2")
        val entities = esmgr.multGetDocs(index, type, ids, WorkSheet::class.java)
        System.out.println("testGetObject 返回值：" + entities)
    }

    /*
curl 'localhost:9200/index-workorder/worksheet/_search?pretty=true'  -H "Content-Type: application/json" -d '
{
  "from" : 0,
  "size" : 10,
  "timeout" : "60s",
  "query" : {
    "bool" : {
      "must" : [
        {
          "match" : {
            "licensePlates" : {
              "query" : "京BJM00测",
              "operator" : "OR",
              "prefix_length" : 0,
              "max_expansions" : 50,
              "fuzzy_transpositions" : true,
              "lenient" : false,
              "zero_terms_query" : "NONE",
              "boost" : 1.0
            }
          }
        }
      ],
      "filter" : [
        {
          "terms" : {
            "deptId" : [
              94,
              93
            ],
            "boost" : 1.0
          }
        },
        {
          "term" : {
            "sheetSource" : {
              "value" : "3",
              "boost" : 1.0
            }
          }
        },
        {
          "range" : {
            "sheet_source" : {
              "from" : 0,
              "to" : 6,
              "include_lower" : true,
              "include_upper" : true,
              "boost" : 1.0
            }
          }
        },
        {
          "range" : {
            "createDate" : {
              "from" : 1615868974174,
              "to" : 1618892974205,
              "include_lower" : true,
              "include_upper" : true,
              "boost" : 1.0
            }
          }
        }
      ],
      "disable_coord" : false,
      "adjust_pure_negative" : true,
      "boost" : 1.0
    }
  },
  "_source" : {
    "includes" : [
      "sheet_source",
      "deptId",
      "licensePlates",
      "sheetSource",
      "createDate"
    ],
    "excludes" : [ ]
  },
  "sort" : [
    {
      "id" : {
        "order" : "asc"
      }
    }
  ],
  "highlight" : { }
}'
     */
    @Test
    fun testSearch() {
        val query = ESQueryBuilder()
        query.select("_id", "deptId", "sheetSource", "licensePlates", "sheet_source", "createDate")
//                .where("deptId", "IN", listOf(94, 93))
//                .where("sheetSource", "3")
//                .where("licensePlates", "like", "京BJM00测")
//                .whereBetween("sheet_source", 0, 6)
        //date
        val start = DateTime().plusDays(-35)
        query.whereBetween("createDate", start.toDate().time, DateTime().toDate().time)

        query.limit(10).offset(0).orderBy("id")

        val (list, size) = esmgr.searchDocs(index, type, query, WorkSheet::class.java)
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
        val c = ESQueryBuilder().index(index).type(type).scrollDocs(WorkSheet::class.java, pageSize, 100000)
        val times = c.size / pageSize + 1
        println("记录数=${c.size}, 每次取=$pageSize, 取次数=$times")
        for (item in c)
            println(item)
    }

    @Test
    fun testDeleteDoc() {
        val pageSize = 5
        val ids = ESQueryBuilder().index(index).type(type).deleteDocs(pageSize, 100000)
        println("删除" + ids.size + "个文档: id in " + ids)
    }

    @Test
    fun testStat() {
        val query = ESQueryBuilder()
        query.where("currentStatus", 1)
                .where("currentDealUserId", "1500")
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
        query.whereBetween("createDate", start.toDate().time, DateTime().toDate().time)

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
