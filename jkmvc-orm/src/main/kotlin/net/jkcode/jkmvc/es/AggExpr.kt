package net.jkcode.jkmvc.es

import net.jkcode.jkutil.validator.ArgsParser
import org.elasticsearch.common.geo.GeoDistance
import org.elasticsearch.common.geo.GeoPoint
import org.elasticsearch.common.unit.DistanceUnit
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder
import org.elasticsearch.search.aggregations.bucket.range.date.DateRangeAggregationBuilder
import org.elasticsearch.search.aggregations.bucket.range.geodistance.GeoDistanceAggregationBuilder
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsAggregationBuilder
import org.elasticsearch.search.sort.SortOrder
import kotlin.math.min

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
        public var alias: String? = null // 别名, 如果别名省略, 则自动生成, 会是`函数名_字段名`, 如 count_name/sum_age, 但对于 terms/nested 函数则还是使用字段名作为别名
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

        if (alias == null) {
            alias = field
            if(alias!!.contains('.'))
                alias = alias!!.replace('.', '_')
            // terms/nested 的别名还是字段名, 但其他的需要加上函数名前缀
            if(func != "terms" && func != "nested")
                alias = func + '_' + alias
        }
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
        // count(field)
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

        // 地理重心聚合——基于文档的某个字段（geo-point类型字段），计算所有坐标的加权重心
        if (func == "geo_centroid")
            return AggregationBuilders.geoCentroid(alias).field(field)

        // geohash_grid 按照你定义的精度计算每一个点的 geohash 值而将附近的位置聚合在一起作为一个区域(单元格)
        // 高精度的geohash字符串长度越长, 代表的区域越小, 低精度的字符串越短, 代表的区域越大
        // 精度为 5 大约是 5km x 5km
        // https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-geohashgrid-aggregation.html
        if(func == "geohash_grid"){
            val precision = args[0].toInt()
            return AggregationBuilders.geohashGrid(alias).field(field).precision(precision)
        }

        // 地理边界聚合——基于文档的某个字段（geo-point类型字段），计算出该字段所有地理坐标点的边界（左上角/右下角坐标点）
        // geo_bounds(field,wrapLongitude)
        if (func == "geo_bounds") {
            val wrapLongitude = args.firstOrNull()?.toBoolean()
                    ?: true
            return AggregationBuilders.geoBounds(alias).field(field)
                    .wrapLongitude(wrapLongitude)
        }

        // 地理距离聚合——基于文档的某个字段（geo-point类型字段），计算出该字段的指定距离范围内的个数
        // geo_distance(field,lat,lon,unit,from1-to1,from2-to2,from3-...)
        if (func == "geo_distance")
            return toGeoDistance()

        // 多桶聚合后的请求如果使用了top_hits，返回结果会带上每个bucket关联的文档数据
        // top_hits(from,size,orderField1 orderDirection1,orderField2 orderDirection2...)
        if (func == "top_hits")
            return toTopHits()

        // 计算指定范围集的文档的个数, 针对数值/日期/ip等, 如统计2011以前/2011-2019/2019及以后的文档数
        // date_range(field,format,from1-to1,from2-to2...)
        if (func == "date_range")
            return toDateRange()

        // 百分百聚合——基于聚合文档中某个数值类型的值，求指定比例中的值分布
        // percentiles(field,percentiles1,percentiles2...)
        if (func == "percentiles") {
            val percentiles = DoubleArray(args.size)
            args.forEachIndexed { i, item ->
                percentiles[i] = item.toDouble()
            }
            return AggregationBuilders.percentiles(alias).field(field)
                    .percentiles(*percentiles)
        }

        // 对字段值按间隔统计建立直方图, 针对数值型和日期型字段。
        // 比如我们以5为间隔，统计不同区间的，现在想每隔5就创建一个桶，统计每隔区间都有多少个文档
        // histogram(field,interval,minBound-maxBound,orderField orderDirection)
        // 其中orderField只有count/key
        // https://www.cnblogs.com/xing901022/p/4954823.html
        if (func == "histogram") {
            return toHistogram()
        }

        // 对日期字段值按间隔统计建立直方图, 针对日期型字段。
        // date_histogram(field,interval,format)
        // interval时间间隔: 1s/1m/1h/1d/1w/1M/1q/1y
        if (func == "date_histogram") {
            val interval = args[0]
            val format = args[1]
            return AggregationBuilders.dateHistogram(alias).field(field)
                    .dateHistogramInterval(DateHistogramInterval(interval))
                    .format(format)
        }

        throw IllegalArgumentException("Unknown aggregation function: " + func)
    }

    /**
     * 对字段值按间隔统计建立直方图, 针对数值型和日期型字段。
     * 比如我们以5为间隔，统计不同区间的，现在想每隔5就创建一个桶，统计每隔区间都有多少个文档
     * histogram(field,interval,minBound-maxBound,orderField orderDirection)
     * 其中orderField只有count/key
     * https://www.cnblogs.com/xing901022/p/4954823.html
     */
    protected fun toHistogram(): HistogramAggregationBuilder {
        val interval = args[0].toDouble()
        val histogram = AggregationBuilders.histogram(alias).field(field)
                .interval(interval)
        val bounds = args.getOrNull(1)?.split('-')
        if (bounds != null) {
            val minBound = bounds[0].toDouble()
            val maxBound = bounds[1].toDouble()
            histogram.extendedBounds(minBound, maxBound)
        }
        // order, 排序=字段 方向
        if (args.size > 3) {
            val order = when (args[2]) {
                "key desc" -> Histogram.Order.KEY_DESC
                "count asc" -> Histogram.Order.COUNT_ASC
                "count desc" -> Histogram.Order.COUNT_DESC
                "key asc" -> Histogram.Order.KEY_ASC
                else -> Histogram.Order.KEY_ASC
            }
            histogram.order(order)
        }
        return histogram
    }

    /**
     * 计算指定范围集的文档的个数, 针对数值/日期/ip等, 如统计2011以前/2011-2019/2019及以后的文档数
     * date_range(field,format,from1-to1,from2-to2...)
     */
    protected fun toDateRange(): DateRangeAggregationBuilder {
        val format = args[0]
        val dateRange = AggregationBuilders.dateRange(alias).field(field).format(format)

        for (i in 3 until args.size) {
            // 每个range=from-to
            val key = args[i]
            val (from, to) = key.split('-')
            if (from != "") {
                if (to != "")
                    dateRange.addRange(key, from, to)
                else
                    dateRange.addUnboundedFrom(key, from)
            } else {
                if (to != "")
                    dateRange.addUnboundedTo(key, to)
                else
                    throw IllegalArgumentException("Aggregation function date_range(field,format,from-to) fail, when from and to is empty")
            }
        }
        return dateRange
    }

    /**
     * 地理距离聚合——基于文档的某个字段（geo-point类型字段），计算出该字段的指定距离范围内的个数
     * geo_distance(field,lat,lon,unit,from1-to1,from2-to2,from3-...)
     */
    protected fun toGeoDistance(): GeoDistanceAggregationBuilder {
        val lat = args[0].toDouble()
        val lon = args[1].toDouble()
        val unit = args[2]
        val dist = AggregationBuilders.geoDistance(alias, GeoPoint(lat, lon))
                .field(field)
                .unit(DistanceUnit.fromString(unit))
                .distanceType(GeoDistance.ARC)
        for (i in 3 until args.size) {
            // 每个range=from-to
            val key = args[i]
            val range = key.split('-')
            val from = range[0].toDoubleOrNull()
            val to = range[1].toDoubleOrNull()
            if (from != null) {
                if (to != null)
                    dist.addRange(key, from, to)
                else
                    dist.addUnboundedFrom(key, from)
            } else {
                if (to != null)
                    dist.addUnboundedTo(key, to)
                else
                    throw IllegalArgumentException("Aggregation function geo_distance(field,lat,lon,unit,from-to) fail, when from and to is empty")
            }
        }
        return dist
    }

    /**
     * 多桶聚合后的请求如果使用了top_hits，返回结果会带上每个bucket关联的文档数据
     *   top_hits(from,size,orderField1 orderDirection1,orderField2 orderDirection2...)
     */
    protected fun toTopHits(): TopHitsAggregationBuilder {
        val topHits = AggregationBuilders.topHits(alias)
        // from
        val from = args.firstOrNull()?.toInt()
        if (from != null)
            topHits.from(from)
        // size
        val size = args.getOrNull(1)?.toInt()
        if (size != null)
            topHits.size(size)
        // order
        for (i in 2 until args.size) {
            // 每个排序=字段 方向
            val orderArgs = args[i].split(' ')
            val field = orderArgs[0]
            val direction = orderArgs.getOrNull(1)
            var order: SortOrder? = null
            if (direction != null)
                order = SortOrder.valueOf(direction.toUpperCase())
            topHits.sort(field, order)
        }
        return topHits
    }

}