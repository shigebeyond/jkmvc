package net.jkcode.jkmvc.es

import io.searchbox.annotations.JestId
import io.searchbox.client.JestResult
import net.jkcode.jkutil.common.getAccessibleField
import net.jkcode.jkutil.common.getCachedAnnotation
import org.elasticsearch.search.aggregations.AggregationBuilder
import org.elasticsearch.search.aggregations.AggregatorFactories
import org.elasticsearch.search.builder.SearchSourceBuilder
import java.lang.reflect.Field

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