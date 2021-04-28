package net.jkcode.jkmvc.es

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import io.searchbox.annotations.JestId
import io.searchbox.client.JestResult
import io.searchbox.core.SearchResult
import io.searchbox.core.search.aggregation.Aggregation
import io.searchbox.core.search.aggregation.AggregationField
import io.searchbox.core.search.aggregation.Bucket
import io.searchbox.core.search.aggregation.MetricAggregation
import net.jkcode.jkutil.common.getAccessibleField
import net.jkcode.jkutil.common.getCachedAnnotation
import org.elasticsearch.search.aggregations.AggregationBuilder
import org.elasticsearch.search.aggregations.AggregatorFactories
import org.elasticsearch.search.builder.SearchSourceBuilder
import java.lang.IllegalArgumentException
import java.lang.reflect.Field
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * 从结果中获得游标id
 *   兼容 SearchResult/ScrollSearchResult
 */
val JestResult.scrollId: String?
    get() {
        return jsonObject["_scroll_id"]?.asString
    }


/**
 * 获得@JestId注解的java属性
 */
public fun Class<*>.getJestIdField(): Field {
    return this.fields.first {
        // 有 @JestId 注解的属性
        it.getCachedAnnotation<JestId>() != null
    }
}

//AggregationBuilder.factoriesBuilder 属性是 AggregatorFactories.Builder
private val factoriesBuilderField = AggregationBuilder::class.java.getAccessibleField("factoriesBuilder")!!

/**
 * 获得AggregationBuilder.factoriesBuilder 属性
 * @return
 */
public fun AggregationBuilder.getFactoriesBuilder():AggregatorFactories.Builder {
    return factoriesBuilderField.get(this) as AggregatorFactories.Builder
}

// SearchSourceBuilder.aggregations 属性 是 AggregatorFactories.Builder
private val aggregationsField = SearchSourceBuilder::class.java.getAccessibleField("aggregations")!!

/**
 * 设置SearchSourceBuilder.aggregations 属性
 */
public fun SearchSourceBuilder.setAggregations(aggs: AggregatorFactories.Builder){
    aggregationsField.set(this, aggs)
}

// Aggregation 的 `JsonObject jsonRoot` 属性
private val jsonRootField = Aggregation::class.java.getAccessibleField("jsonRoot")!!
// JsonPrimitive 的 `Object value` 属性
private val valueField = JsonPrimitive::class.java.getAccessibleField("value")!!

/**
 * 处理简单聚合对象的值
 *    只能处理 count/sum/avg/max/min 等的结构简单的聚合对象, 一般是有 value 属性
 */
private val valueKey = AggregationField.VALUE.toString() // value 属性
public fun handleSimpleAggValue(bucket: Bucket, row: MutableMap<String, Any>){
    val json = jsonRootField.get(bucket) as JsonObject
    for ((key, value) in json.entrySet()) {
        if (key == "key") {
            // donth
        } else if (key == "doc_count") {
            row["count"] = valueField.get(value as JsonPrimitive) // 获得原始值
        } else if (value is JsonObject && value.has(valueKey)) {
            row[key] = valueField.get(value.get(valueKey) as JsonPrimitive) // 获得原始值
        }
    }
}

/**
 * 扁平化聚合结果为多行
 * @param path 前面几列(除了最后一列)的路径, 用逗号分割
 * @param aggValueHandler 处理最后一列的聚合对象的值, 参数: 1 聚合对象 2 结果行对象
 * @return
 */
public fun MetricAggregation.flattenAggRows(path: String, aggValueHandler: (bucket: Bucket, row: MutableMap<String, Any>) -> Unit = ::handleSimpleAggValue): ArrayList<Map<String, Any>> {
    val names: List<String> = path.split('.')
    val result = ArrayList<Map<String, Any>>()
    travelAggTree(this, names, 0, Stack()) { keys, agg ->
        if (names.size != keys.size)
            throw IllegalArgumentException("keys size not right")

        val row = HashMap<String, Any>()
        // 取前面几列的值
        for (i in 0 until names.size) {
            row[names[i]] = keys[i]
        }
        // 取最后一列的聚合对象的值
        aggValueHandler(agg, row)
        result.add(row)
    }
    return result
}

/**
 * 遍历聚合树, 整合聚合结果为多行(Map), 行(Map)类似于db的列名->值
 * @param aggs 当前级聚合
 * @param names 聚合名(类似于db的列名)
 * @param iName 当前聚合名序号(层级)
 * @param keyStack 聚合字段值(列值)
 * @param rowHandler 处理一行数据, 参数: 1 前面几列的值 2 最后一列的聚合对象
 */
private fun travelAggTree(aggs: MetricAggregation, names: List<String>, iName: Int, keyStack: Stack<String>, rowHandler: (Stack<String>, Bucket)->Unit) {
    val buckets = aggs.getTermsAggregation(names[iName]).buckets
    for (bucket in buckets) {
        keyStack.push(bucket.key) // key入栈
        if(iName < names.size - 1) { // 不是最后一个
            travelAggTree(bucket, names, iName + 1, keyStack, rowHandler) // 递归
        }else{ // 最后一个
            rowHandler(keyStack, bucket)
        }
        keyStack.pop() // key出栈
    }
}