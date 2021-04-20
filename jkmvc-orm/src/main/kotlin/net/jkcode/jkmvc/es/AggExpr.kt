package net.jkcode.jkmvc.es

import net.jkcode.jkutil.validator.ArgsParser
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders

/**
 * 聚合表达式
 *    `AggExpr("terms(dep_id)", "dep_id")`, 类似于 select * group by dep_id
 *        其中函数 terms 可省, 即等价于`AggExpr("dep_id", "dep_id")`
 *    `AggExpr("count(name)", "count_name")`, 类似于 select count(name) as count_name
 *    `AggExpr("sum(age)", "sum_age", true)`, 类似于 select sum(age) as sum_age order by sum_age asc
 *
 * @author shijianhang
 * @date 2021-4-21 下午5:16:59
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
            args = ArgsParser.parse(exp.substring(i)) // 包含()
        }else{
            func = "terms" // 默认terms
            args = listOf(exp)
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

        if(func == "count")
            return AggregationBuilders.count(alias).field(field)

        if(func == "sum")
            return AggregationBuilders.sum(alias).field(field)

        if(func == "avg")
            return AggregationBuilders.avg(alias).field(field)

        if(func == "min")
            return AggregationBuilders.min(alias).field(field)

        if(func == "max")
            return AggregationBuilders.max(alias).field(field)

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