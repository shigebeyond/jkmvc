package net.jkcode.jkmvc.es

import net.jkcode.jkutil.common.mapToArray
import net.jkcode.jkutil.validator.ArgsParser
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.sort.SortOrder

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
        public val exp: String, // 表达式
        public var alias: String? = null // 别名
) {

    /**
     * 函数
     */
    lateinit var func: String

    /**
     * 字段
     */
    lateinit var field: String

    /**
     * 参数
     */
    lateinit var args: List<String>

    init {
        // 表达式是函数调用, 格式为 avg(score)
        val i = exp.indexOf('(')
        if (i > -1) { // 包含()对
            func = exp.substring(0, i)
            val fieldArgs = ArgsParser.parse(exp.substring(i)) // 包含()
            if (fieldArgs.isEmpty())
                throw IllegalArgumentException("Miss field")
            field = (fieldArgs as MutableList).removeAt(0)
            args = fieldArgs
        } else {
            func = "terms" // 默认terms
            field = exp
            args = emptyList()
        }

        if (alias == null)
            alias = func + '_' + field
    }

    /**
     * 转为聚合对象
     *   参考es聚合详解
     *   https://www.cnblogs.com/candlia/p/11920034.html
     */
    fun toAggregation(): AbstractAggregationBuilder<*> {
        if (func == "terms")
            return AggregationBuilders.terms(alias).field(field)

        if (func == "nested")
            return AggregationBuilders.nested(alias, field) // field 是path

        // 总数聚合: value_count
        if (func == "count")
            return AggregationBuilders.count(alias).field(field)

        // 求和聚合
        if (func == "sum")
            return AggregationBuilders.sum(alias).field(field)

        // 平均聚合
        if (func == "avg")
            return AggregationBuilders.avg(alias).field(field)

        // 最小值聚合
        if (func == "min")
            return AggregationBuilders.min(alias).field(field)

        // 最大值聚合
        if (func == "max")
            return AggregationBuilders.max(alias).field(field)

        // 基数聚合——基于文档的某个值，计算文档非重复的个数（去重计数）
        if (func == "cardinality")
            return AggregationBuilders.cardinality(alias).field(field)

        // 统计聚合——基于文档的某个值，计算出一些统计信息（min、max、sum、count、avg）
        if (func == "stats")
            return AggregationBuilders.stats(alias).field(field)

        // 百分百聚合——基于聚合文档中某个数值类型的值，求指定比例中的值分布
        if (func == "percentiles") {
            val percentiles = DoubleArray(args.size)
            args.forEachIndexed { i, item ->
                percentiles[i] = item.toDouble()
            }
            return AggregationBuilders.percentiles(alias).percentiles(*percentiles).field(field)
        }

        // 地理边界聚合——基于文档的某个字段（geo-point类型字段），计算出该字段所有地理坐标点的边界（左上角/右下角坐标点）
        if (func == "geo_bounds") {
            val wrapLongitude = args.firstOrNull()?.toBoolean()
                    ?: true
            return AggregationBuilders.geoBounds(alias).wrapLongitude(wrapLongitude).field(field)
        }

        // 地理重心聚合——基于文档的某个字段（geo-point类型字段），计算所有坐标的加权重心
        if (func == "geo_centroid")
            return AggregationBuilders.geoCentroid(alias).field(field)

        // 多桶聚合后的请求如果使用了top_hits，返回结果会带上每个bucket关联的文档数据
        if (func == "top_hits") {
            val topHits = AggregationBuilders.topHits(alias)
            // from
            val from = args.firstOrNull()?.toInt()
            if(from != null)
                topHits.from(from)
            // size
            val size = args.getOrNull(1)?.toInt()
            if(size != null)
                topHits.size(size)
            // order
            for(i in 2 until args.size){
                // 每个排序=字段 方向
                val orderArgs = args[i].split(' ')
                val field = orderArgs[0]
                val direction = orderArgs.getOrNull(1)
                var order: SortOrder? = null
                if(direction != null)
                    order = SortOrder.valueOf(direction.toUpperCase())
                topHits.sort(field, order)
            }
            return topHits
        }

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