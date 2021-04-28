package net.jkcode.jkmvc.es

import com.google.gson.JsonObject
import io.searchbox.core.search.aggregation.AggregationField
import io.searchbox.core.search.aggregation.Bucket

/**
 * 嵌套文档的聚合
 *
 *
 * @author shijianhang
 * @date 2021-4-21 下午5:16:59
 */
class NestedAggregation(name: String, bucket: JsonObject) : Bucket(name, bucket, bucket.getAsJsonPrimitive(AggregationField.DOC_COUNT.toString()).asLong /* doc_count 属性 */) {
}