package net.jkcode.jkmvc.es

import org.elasticsearch.search.aggregations.AbstractAggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders

/**
 * 聚合表达式
 */
class AggExpr(
        public val exp:String, // 表达式
        public var alias:String? = null, // 别名
        public val asc: Boolean? = null // 升序
) {

    /**
     * 函数
     */
    lateinit var func: String

    /**
     * 参数
     */
    lateinit var args: List<String>

    init {
        // 表达式是函数调用, 格式为 avg(score)
        val i = exp.indexOf('(')
        if(i > -1){ // 包含()对
            func = exp.substring(0, i)
        }else{
            func = exp
            args = emptyList()
        }

        if(alias == null)
            alias = func + '_' + args.firstOrNull()
    }

    /**
     * 转为聚合对象
     */
    fun toAggregation(): AbstractAggregationBuilder<*> {
        val field = args.first()
        if(func == "terms")
            return AggregationBuilders.terms(alias).field(field)

        // todo
        /**
        val aggregationBuilder = AggregationBuilders.dateHistogram("dateagg")
        .field("createDate")
        .dateHistogramInterval(DateHistogramInterval.DAY)
        .timeZone(DateTimeZone.forID("Asia/Shanghai"))
         */

        throw IllegalArgumentException("Unknown aggregation function: " + func)
    }

}