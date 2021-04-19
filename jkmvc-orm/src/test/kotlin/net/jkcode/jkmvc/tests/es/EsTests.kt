package net.jkcode.jkmvc.tests.es

import com.alibaba.fastjson.JSON
import net.jkcode.jkmvc.es.ESQueryBuilder
import net.jkcode.jkmvc.es.EsManager
import net.jkcode.jkutil.common.randomString
import org.joda.time.DateTime
import org.junit.Test
import java.util.*

class EsTests {

    private val index = "index-workorder"

    private val type = "worksheet"



    @Test
    fun createIndex() {
        val mapping = "{\"worksheet\": {\"properties\": {\"call_record_id\": {\"type\": \"long\"}, \"city_id\": {\"type\": \"long\"}, \"commit_user_id\": {\"type\": \"keyword\"}, \"commit_user_name\": {\"type\": \"keyword\"}, \"confirm_sheet_type_four\": {\"type\": \"long\"}, \"confirm_sheet_type_one\": {\"type\": \"long\"}, \"confirm_sheet_type_three\": {\"type\": \"long\"}, \"confirm_sheet_type_two\": {\"type\": \"long\"}, \"contact\": {\"type\": \"keyword\"}, \"create_date\": {\"type\": \"date\"}, \"current_deal_user_id\": {\"type\": \"keyword\"}, \"current_deal_user_name\": {\"type\": \"keyword\"}, \"current_status\": {\"type\": \"long\"}, \"dept_id\": {\"type\": \"long\"}, \"driver_id\": {\"type\": \"long\"}, \"driver_name\": {\"type\": \"keyword\"}, \"driver_phone\": {\"type\": \"keyword\"}, \"duty_dept\": {\"type\": \"long\"}, \"handle_time\": {\"type\": \"long\"}, \"id\": {\"type\": \"long\"}, \"license_plates\": {\"type\": \"keyword\"}, \"memo\": {\"type\": \"keyword\"}, \"order_no\": {\"type\": \"keyword\"}, \"order_type\": {\"type\": \"long\"}, \"reopen_times\": {\"type\": \"long\"}, \"rider_name\": {\"type\": \"keyword\"}, \"rider_phone\": {\"type\": \"keyword\"}, \"service_type_id\": {\"type\": \"long\"}, \"sheet_classify\": {\"type\": \"long\"}, \"sheet_priority\": {\"type\": \"long\"}, \"sheet_source\": {\"type\": \"long\"}, \"sheet_tag\": {\"type\": \"long\"}, \"sheet_tag_sort\": {\"type\": \"long\"}, \"sheet_type_four\": {\"type\": \"long\"}, \"sheet_type_one\": {\"type\": \"long\"}, \"sheet_type_three\": {\"type\": \"long\"}, \"sheet_type_two\": {\"type\": \"long\"}, \"update_date\": {\"type\": \"date\"}, \"urge_times\": {\"type\": \"long\"}, \"weight\": {\"type\": \"long\"}, \"work_sheet_no\": {\"type\": \"keyword\"} } } } "
        EsManager.createIndex(index)
        EsManager.putMapping(index, type, mapping)
    }

    @Test
    fun deleteIndex() {
        //删除
        EsManager.deleteIndex(index)
    }

    @Test
    fun testInsertData() {
        //按照mapping 创建 index
        val dataList = ArrayList<WorkSheet>()

        for(i in 1..10){
            val e = WorkSheet()
            e.id = i
            e.workSheetNo = randomString(5)
            e.sheetTypeTwo = 2
            e.attentionUserIds = randomString(5)
            e.createDate = Date(System.currentTimeMillis())
            dataList.add(e)
        }

        EsManager.bulkInsertDocs(index, type, dataList)
        //EsManager.insertData(index, type, list);
    }

    @Test
    fun testUpdate() {
        val workSheet = EsManager.getDoc(index, type, "101328", WorkSheet::class.java)!!
        workSheet.updateDate = Date(System.currentTimeMillis())
        workSheet.urgeTimes = 2
        workSheet.reopenTimes = 3
        EsManager.updateDoc(index, type, "101328", workSheet)
    }

    @Test
    fun testIndexExist() {
        System.out.println("索引是否存在：" + EsManager.indexExist(index))
    }

    @Test
    fun testAddAliases() {
        val list = Arrays.asList(index)
        EsManager.addIndexAlias(list, "index-workorder_alias")
        this.testGetIndexAliases()
    }

    @Test
    fun testGetIndexAliases() {
        EsManager.getIndexAliases(index)
    }

    @Test
    fun testGetObject() {
        val id = "AWq5jSn0MCnKwL3pKsoK"
        val entity = EsManager.getDoc(index, type, id, WorkSheet::class.java)
        System.out.println("testGetObject 返回值：" + entity.toString())
    }

    @Test
    fun testSearch() {
        val query = ESQueryBuilder()
        query.select("deptId", "sheetSource", "licensePlates", "sheet_source", "createDate")
                .where("deptId", "IN", listOf(94, 93))
                .where("sheetSource", "3")
                .where("licensePlates", "京BJM00测")
                .whereBetween("sheet_source", 0, 6 )
        //date
        val start = DateTime().plusDays(-35)
        query.whereBetween("createDate", start.toDate().time, DateTime().toDate().time)

        query.limit(10).offset(0).orderBy("id")


        val (list, size) = EsManager.searchDocs(index, type, query, WorkSheet::class.java)
        for (item in list)
            println(item)
    }


    @Test
    fun testStat() {
        val query = ESQueryBuilder()
        query.where("currentStatus", 1)
                .where("currentDealUserId", "1500")
                .aggBy("deptId")
                .aggBy("count(id)", "nid")

        val result = EsManager.searchDocs(index, type, query)
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
    fun testStat3() {
        //多级别统计,这种情况，只能返回 result 自己处理了
        val query = ESQueryBuilder()
        //加上时间过滤
        val start = DateTime().plusDays(-10)
        query.whereBetween("createDate", start.toDate().time, DateTime().toDate().time)

        query.aggBy("sheetTypeOne").aggBy("sheetTypeTwo", "agg")
        val result = EsManager.searchDocs(index, type, query)
        println("返回结果:" + JSON.toJSONString(result))
        if (result.isSucceeded) {
            // 多值
            val map = result.aggregations.getTermsAggregation("nid").buckets.forEach{ item ->
                println("两级分组统计返回：" + item.key + "---" + JSON.toJSONString(item)) }
            }
        }
    }


}