package net.jkcode.jkmvc.es

import com.google.gson.JsonObject
import io.searchbox.core.search.aggregation.AggregationField
import io.searchbox.core.search.aggregation.Bucket

/**
 * 嵌套文档的聚合
 *    jest没有对应的类, 因此实现一个
 *    仅仅做嵌套文档聚合的中转
 *
 * @author shijianhang
 * @date 2021-4-21 下午5:16:59
 */
class NestedAggregation(name: String, bucket: JsonObject) : Bucket(name, bucket, bucket.getAsJsonPrimitive(AggregationField.DOC_COUNT.toString()).asLong /* doc_count 属性 */) {
}