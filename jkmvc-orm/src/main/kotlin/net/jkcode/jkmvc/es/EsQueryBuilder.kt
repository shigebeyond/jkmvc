package net.jkcode.jkmvc.es

import io.searchbox.core.SearchResult
import io.searchbox.core.UpdateByQueryResult
import io.searchbox.params.SearchType
import net.jkcode.jkmvc.es.annotation.esDoc
import net.jkcode.jkutil.common.esLogger
import net.jkcode.jkutil.common.isArrayOrCollection
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.common.unit.DistanceUnit
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilder
import org.elasticsearch.search.aggregations.AggregatorFactories
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * 查询构建器
 *
 * @author shijianhang
 * @date 2021-4-21 下午5:16:59
 */
class EsQueryBuilder @JvmOverloads constructor(protected val esmgr: EsManager = EsManager.instance()) {

    @JvmOverloads
    constructor(index: String, type: String, esmgr: EsManager = EsManager.instance()):this(esmgr){
        this.index(index)
        this.type(type)
    }

    companion object {

        /**
         * Filter operators
         */
        protected val operators = arrayOf(
                "=", // term
                "IN", // terms
                "!=",
                ">",
                ">=",
                "<",
                "<=",
                "like", // like = match
                "match",
                "matchPhrase",
                "fuzzy",
                "wildcard",
                "prefix",
                "queryString",
                "exists"
        )

        /**
         * check if it"s a valid operator
         * @param op
         * @return bool
         */
        protected fun isOperator(op: String): Boolean {
            return operators.contains(op)
        }
    }

    /**************************** 拼接查询条件 ****************************/
    /**
     * Query index name
     */
    protected lateinit var index: String

    /**
     * Query type name
     */
    protected var type: String = "_doc"

    /**
     * Whether index is initial
     */
    protected var indexInited = false

    /**
     * 查询对象栈
     *   元素是 BoolQueryBuilder + 上一层的filter/must/mustNot/should对象(仅用于闭合校验)
     */
    protected val queryStack: Stack<Pair<BoolQueryBuilder, List<*>?>> by lazy{
        val s = Stack<Pair<BoolQueryBuilder, List<*>?>>()
        s.push(QueryBuilders.boolQuery() to null)
        s
    }

    /**
     * 当前查询对象
     */
    protected val currQuery: BoolQueryBuilder
        get() {
            return queryStack.peek().first
        }

    /**
     * 是否有设置BoolQueryBuilder
     */
    protected var hasQuery = false

    /**
     * Current query bool filter
     */
    protected val filter: MutableList<QueryBuilder>
        get() {
            hasQuery = true
            return currQuery.filter()
        }

    /**
     * Current query bool must
     */
    protected val must: MutableList<QueryBuilder>
        get() {
            hasQuery = true
            return currQuery.must()
        }

    /**
     * Current query bool must not
     */
    protected val mustNot: MutableList<QueryBuilder>
        get() {
            hasQuery = true
            return currQuery.mustNot()
        }

    /**
     * Current query bool should
     */
    protected val should: MutableList<QueryBuilder>
        get() {
            hasQuery = true
            return currQuery.should()
        }

    /**
     * 聚合组栈
     *   元素是 AggregatorFactories.Builder + 上一层的聚合组对象(仅用于闭合校验)
     *   TODO: 去掉元素的second
     *   因为不像queryStack有多种类型对象(filter/must/mustNot/should), 因此queryStack的close时可检查对象类型即可
     *   而aggsStack是只有一种类型对象, 前一个元素的first, 肯定是下一个元素的second, 没必要校验
     *
     */
    protected val aggsStack: Stack<Pair<AggregatorFactories.Builder, AggregatorFactories.Builder?>> by lazy{
        val s = Stack<Pair<AggregatorFactories.Builder, AggregatorFactories.Builder?>>()
        s.push(AggregatorFactories.builder() to null)
        s
    }

    /**
     * 当前聚合组
     *   SearchSourceBuilder.aggregation(AggregationBuilder) 与 AggregationBuilder.subAggregation(AggregationBuilder) 子聚合
     *   SearchSourceBuilder.aggregations 属性 与 AggregationBuilder.factoriesBuilder 属性都是 AggregatorFactories.Builder
     *   内部都使用 AggregatorFactories.Builder.addAggregator(AggregationBuilder)
     *   其中 AggregatorFactories.Builder.aggregationBuilders 属性是 AggregationBuilder列表
     */
    protected val currAggs: AggregatorFactories.Builder
        get() {
            return aggsStack.peek().first
        }

    /**
     * 父聚合
     *   无用
     */
    protected val parentAgg: AggregationBuilder
        get(){
            if(aggsStack.size < 2)
                throw IllegalStateException("Cannot find parent aggregation, because `aggsStack` only has 1 aggregation group")

            val parentAggs = aggsStack[aggsStack.size - 2].first
            return parentAggs.aggregatorFactories.last()
        }

    /**
     * 逐层往上找到满足条件的最近父聚合
     * @param predicate 匹配条件
     * @return
     */
    protected fun getClosetParentAgg(predicate: (AggregationBuilder) -> Boolean): AggregationBuilder? {
        travelParentAggs{ parentAgg ->
            if(predicate(parentAgg))
                return@getClosetParentAgg parentAgg // 找到直接return getClosetParentAgg()
        }

        return null
    }

    /**
     * 逐层往上遍历父聚合
     * @param action 迭代操作
     */
    protected inline fun travelParentAggs(action: (AggregationBuilder) -> Unit) {
        if(aggsStack.size < 2)
            throw IllegalStateException("Cannot find parent aggregation, because `aggsStack` only has 1 aggregation group")

        // 逐层往上遍历父聚合
        val reverseIndx = (aggsStack.size - 2).downTo(0)
        for (i in reverseIndx){
            val parentAggs = aggsStack[i].first // AggregatorFactories.Builder
            val parentAgg = parentAggs.aggregatorFactories.last() // AggregationBuilder
            // 迭代操作
            action(parentAgg)
        }
    }

    /**
     * 下一层聚合组
     *    当前聚合组最后一个聚合的子聚合组
     */
    protected val nextLevelAggs: AggregatorFactories.Builder
        get() {
            // 当前聚合组最后一个聚合
            // AggregatorFactories.Builder.aggregationBuilders 属性是 AggregationBuilder列表
            val nextAgg = currAggs.aggregatorFactories.last()
            // 子聚合组
            //AggregationBuilder.factoriesBuilder 属性是 AggregatorFactories.Builder
            return nextAgg.getFactoriesBuilder()
        }

    /**
     * 返回字段
     */
    protected var includeFields = HashSet<String>()

    /**
     * 不返回字段
     */
    protected val excludeFields = HashSet<String>()

    /**
     * 高亮字段
     */
    protected val highlightFields = ArrayList<String>();

    /**
     * 修改的字段脚本
     *  <字段 to 脚本>
     */
    protected val fieldScripts = HashMap<String, Script>()

    /**
     * Query sort fields
     */
    protected val sorts = ArrayList<SortBuilder<*>>();

    /**
     * 最低分
     */
    protected var minScore = 0f

    /**
     * post filter
     */
    protected var postFilter: QueryBuilder? = null

    /**
     * Query search type
     */
    internal var searchType = SearchType.DFS_QUERY_THEN_FETCH

    /**
     * Query limit
     */
    protected var limit = 0;

    /**
     * Query offset
     */
    protected var offset = 0;

    /**
     * timeout (seconds) to control how long search is allowed to take
     */
    protected var timeout = 0L;

    /**
     * 清空查询元素
     */
    fun clear() {
        index = ""
        type = ""
        if(hasQuery) {
            queryStack.clear()
            queryStack.push(QueryBuilders.boolQuery() to null)
        }
        aggsStack.clear()
        aggsStack.push(AggregatorFactories.builder() to null)
        includeFields.clear()
        excludeFields.clear()
        highlightFields.clear()
        fieldScripts.clear()
        sorts.clear()
        minScore = 0f
        postFilter = null
        searchType = SearchType.DFS_QUERY_THEN_FETCH
        limit = 0
        offset = 0
    }

    /**
     * Set the index name
     * @param index
     * @return this
     */
    public fun index(index: String): EsQueryBuilder {
        this.index = index;
        this.indexInited = true
        return this
    }

    /**
     * Set the type name
     * @param type
     * @return this
     */
    public fun type(type: String): EsQueryBuilder {
        this.type = type;
        return this;
    }

    /**
     * Set the query search type
     * @param type
     * @return this
     */
    public fun searchType(type: SearchType): EsQueryBuilder {
        this.searchType = type;
        return this
    }

    /**
     * Set the query limit and offset
     * @param int limit
     * @return this
     */
    @JvmOverloads
    public fun limit(limit: Int, offset: Int = 0): EsQueryBuilder {
        this.limit = limit;
        if(offset > 0)
            this.offset = offset;
        return this;
    }

    /**
     * Set timeout (seconds) to control how long search is allowed to take.
     */
    public fun timeout(timeout: Long): EsQueryBuilder {
        this.timeout = timeout
        return this
    }

    /**
     * Set the query fields to return
     * @param fields
     * @return this
     */
    public fun select(vararg fields: String): EsQueryBuilder {
        this.includeFields.addAll(fields)
        return this;
    }

    /**
     * Set the ignored fields to not be returned
     * @param fields
     * @return this
     */
    public fun unselect(vararg fields: String): EsQueryBuilder {
        this.excludeFields.addAll(fields)
        this.includeFields.removeAll(fields)
        return this;
    }

    // --------- orderby start ---------
    /**
     * Set the sorting field
     * @param field
     * @param direction
     * @return this
     */
    @JvmOverloads
    public fun orderByField(field: String, desc: Boolean = false): EsQueryBuilder {
        val order = if (desc) SortOrder.DESC else SortOrder.ASC
        val sort = SortBuilders.fieldSort(field).order(order)
        // 嵌套字段: 设置嵌套路径
        if(field.contains('.')){
            // 获得嵌套路径
            val path = field.substringBeforeLast('.')
            // 设置嵌套路径
            sort.setNestedPath(path)
        }
        this.sorts.add(sort)
        return this;
    }

    /**
     * Set the sorting field to `_score`
     *   _score为相关性得分
     * @param field
     * @param direction
     * @return this
     */
    @JvmOverloads
    public fun orderByScore(desc: Boolean = false): EsQueryBuilder {
        val order = if (desc) SortOrder.DESC else SortOrder.ASC
        val sort = SortBuilders.scoreSort().order(order)
        this.sorts.add(sort)
        return this;
    }

    /**
     * Set the sorting script
     * @param script
     * @param params
     * @param direction
     * @return this
     */
    @JvmOverloads
    public fun orderByScript(script: String, params: Map<String, Any?> = emptyMap(), desc: Boolean = false): EsQueryBuilder {
        val order = if (desc) SortOrder.DESC else SortOrder.ASC
        val script2 = Script(ScriptType.INLINE, "painless", script, params)
        val sort = SortBuilders.scriptSort(script2, ScriptSortBuilder.ScriptSortType.STRING).order(order)
        this.sorts.add(sort)
        return this;
    }

    /**
     * Set the sorting geoDistance
     * @param field
     * @param params
     * @param direction
     * @param unit 距离单位
     * @return this
     */
    @JvmOverloads
    public fun orderByGeoDistance(field: String, lat: Double, lon: Double, desc: Boolean = false, unit: DistanceUnit = DistanceUnit.DEFAULT): EsQueryBuilder {
        val order = if (desc) SortOrder.DESC else SortOrder.ASC
        val sort = SortBuilders.geoDistanceSort(field, lat, lon).order(order).unit(unit)
        this.sorts.add(sort)
        return this;
    }
    // --------- orderby end ---------

    // --------- where start ---------
    /**
     * 构建单个条件
     */
    protected fun build1Condition(name: String, operator: String, value: Any?): QueryBuilder {
        if (operator == "=") { // term
            val v = if(name == "_id") value?.toString() else value // id转字符串
            return QueryBuilders.termQuery(name, v)
        }

        if (operator.equals("IN", true)) { // terms
            return when (value) {
                is Array<*> -> QueryBuilders.termsQuery(name, *value)
                is IntArray -> QueryBuilders.termsQuery(name, *value)
                is LongArray -> QueryBuilders.termsQuery(name, *value)
                is FloatArray -> QueryBuilders.termsQuery(name, *value)
                is DoubleArray -> QueryBuilders.termsQuery(name, *value)
                is Collection<*> -> QueryBuilders.termsQuery(name, value)
                else -> throw IllegalArgumentException("Value [$value] is not array or collection for terms query")
            }
        }

        if (operator == ">")
            return QueryBuilders.rangeQuery(name).gt(value);

        if (operator == ">=")
            return QueryBuilders.rangeQuery(name).gte(value);

        if (operator == "<")
            return QueryBuilders.rangeQuery(name).lt(value);

        if (operator == "<=")
            return QueryBuilders.rangeQuery(name).lte(value);

        if (operator.equals("between", true)) {
            val (from, to) = value as Pair<Any, Any>
            return QueryBuilders.rangeQuery(name).gte(from).lte(to)
        }

        if (operator.equals("like", true) || operator.equals("match", true))
            return QueryBuilders.matchQuery(name, value)

        if (operator.equals("matchPhrase", true))
            return QueryBuilders.matchPhraseQuery(name, value)

        if(operator.equals("fuzzy", true))
            return QueryBuilders.fuzzyQuery(name, value)

        if(operator.equals("wildcard", true))
            return QueryBuilders.wildcardQuery(name, value.toString())

        if(operator.equals("prefix", true))
            return QueryBuilders.prefixQuery(name, value.toString())

        if(operator.equals("queryString", true))
            return QueryBuilders.queryStringQuery(value.toString())

        if (operator.equals("exist", true))
            return QueryBuilders.existsQuery(name)

        throw IllegalArgumentException("Unkown operator")
    }

    /**
     * Prepare operator
     *
     * @param   value   column value
     * @return
     */
    protected fun prepareOperator(value: Any?): String {
        if (value != null && value.isArrayOrCollection()) // 数组/集合
            return "IN"

        return "=";
    }

    /**
     * Set the query filter/must/mustNot/should clause
     * @param name
     * @param operator
     * @param value
     * @return this
     */
    protected inline fun where(wheres: MutableList<QueryBuilder>, name: String, operator: String, value: Any?): EsQueryBuilder {
        if (!isOperator(operator))
            throw IllegalArgumentException("Unkown operator: $operator")

        val condition = build1Condition(name, operator, value)
        wheres.add(condition)
        return this
    }

    /**
     * Set the query filter/must/mustNot/should between clause
     * @param name
     * @param from
     * @param to
     * @return this
     */
    protected inline fun whereBetween(wheres: MutableList<QueryBuilder>, name: String, from: Any, to: Any): EsQueryBuilder {
        val query = QueryBuilders.rangeQuery(name).gte(from).lte(to)
        wheres.add(query)
        return this;
    }

    /**
     * Add a condition to find documents which are some distance away from the given geo point.
     *
     * @see https://www.elastic.co/guide/en/elasticsearch/reference/2.4/query-dsl-geo-distance-query.html
     *
     * @param name A name of the field.
     * @param lat geo point
     * @param lon geo point
     * @param distance A distance from the starting geo point. It can be for example "20km".
     * @return this
     */
    protected inline fun whereGeoDistance(wheres: MutableList<QueryBuilder>, name: String, lat: Double, lon: Double, distance: String): EsQueryBuilder {
        val query = QueryBuilders.geoDistanceQuery(name).point(lat, lon).distance(distance);
        wheres.add(query)
        return this;
    }

    /**
     * 搜索矩形(top,left,bottom,right)范围内的坐标点
     * @param wheres
     * @param name
     * @param top
     * @param left
     * @param bottom
     * @param right
     * @return
     */
    protected inline fun whereGeoBoundingBox(wheres: MutableList<QueryBuilder>, name: String, top: Double, left: Double, bottom: Double, right: Double): EsQueryBuilder {
        val query = QueryBuilders.geoBoundingBoxQuery(name).setCorners(top, left, bottom, right);
        wheres.add(query)
        return this
    }

    /**
     * 搜索与指定点距离在给定最小距离和最大距离之间的点
     * @param name A name of the field.
     * @param lat geo point
     * @param lon geo point
     * @param distanceFrom A distance from the starting geo point. It can be for example "20km".
     * @param distanceTo A distance to the starting geo point. It can be for example "20km".
     * @return
     */
    protected inline fun whereGeoDistanceRange(wheres: MutableList<QueryBuilder>, name: String, lat: Double, lon: Double, distanceFrom: String, distanceTo: String): EsQueryBuilder {
        val query = QueryBuilders.geoDistanceRangeQuery(name, lat, lon).from(distanceFrom).to(distanceTo)
        wheres.add(query)
        return this
    }

    /**
     * Open a filter/must/mustNot/should sub clauses
     * @return
     */
    protected inline fun whereOpen(wheres: MutableList<QueryBuilder>): EsQueryBuilder {
        val query = QueryBuilders.boolQuery()
        wheres.add(query)
        queryStack.push(query to wheres)
        return this
    }

    /**
     * Opens a filter/must/mustNot/should nested sub clauses
     * @param path
     * @return
     */
    protected inline fun whereNestedOpen(wheres: MutableList<QueryBuilder>, path: String): EsQueryBuilder {
        val subquery = QueryBuilders.boolQuery()
        val nestedContainer = QueryBuilders.nestedQuery(path, subquery, ScoreMode.Total)
        wheres.add(nestedContainer)
        // 入栈的是 subquery, 而非 nestedContainer
        // https://blog.csdn.net/lsq_401/article/details/100983450
        queryStack.push(subquery to wheres)
        return this
    }

    /**
     * Close a filter/must/mustNot/should sub clauses
     * @return
     */
    protected inline fun whereClose(wheresGetter: EsQueryBuilder.()->MutableList<QueryBuilder>): EsQueryBuilder {
        // 出栈, 获得上一层的where对象
        val (_, preWheres) = queryStack.pop()
        // 出栈后的当前层的where对象
        val currWheres = this.wheresGetter()
        // 两者应该相等
        if(currWheres != preWheres)
            throw EsException("Close not match last open")

        return this
    }
    // --------- where end ---------

    // --------- filter start ---------
    /**
     * Set the query filter clause
     * @param condition
     * @return this
     */
    public fun filter(condition: QueryBuilder): EsQueryBuilder {
        filter.add(condition)
        return this
    }

    /**
     * Set the query filter clause
     * @param name
     * @param value
     * @return this
     */
    public fun filter(name: String, value: Any?): EsQueryBuilder {
        return filter(name, prepareOperator(value), value)
    }

    /**
     * Set the query filter clause
     * @param name
     * @param operator
     * @param value
     * @return this
     */
    public fun filter(name: String, operator: String, value: Any?): EsQueryBuilder {
        return where(this.filter, name, operator, value)
    }

    /**
     * Set the query filter exists clause
     * @param name
     * @return this
     */
    public fun filterExists(name: String): EsQueryBuilder {
        return filter(name, "exists", null)
    }

    /**
     * Set the query filter between clause
     * @param name
     * @param from
     * @param to
     * @return this
     */
    public fun filterBetween(name: String, from: Any, to: Any): EsQueryBuilder {
        return whereBetween(this.filter, name, from, to)
    }

    /**
     * Add a condition to find documents which are some distance away from the given geo point.
     *
     * @see https://www.elastic.co/guide/en/elasticsearch/reference/2.4/query-dsl-geo-distance-query.html
     *
     * @param name A name of the field.
     * @param lat geo point
     * @param lon geo point
     * @param distance A distance from the starting geo point. It can be for example "20km".
     * @return this
     */
    public fun filterGeoDistance(name: String, lat: Double, lon: Double, distance: String): EsQueryBuilder {
        return whereGeoDistance(this.filter, name, lat, lon, distance)
    }

    /**
     * 搜索矩形(top,left,bottom,right)范围内的坐标点
     * @param name
     * @param top
     * @param left
     * @param bottom
     * @param right
     * @return
     */
    public fun filterGeoBoundingBox(wheres: MutableList<QueryBuilder>, name: String, top: Double, left: Double, bottom: Double, right: Double): EsQueryBuilder {
        return whereGeoBoundingBox(this.filter, name, top, left, bottom, right)
    }

    /**
     * 搜索与指定点距离在给定最小距离和最大距离之间的点
     * @param name A name of the field.
     * @param lat geo point
     * @param lon geo point
     * @param distanceFrom A distance from the starting geo point. It can be for example "20km".
     * @param distanceTo A distance to the starting geo point. It can be for example "20km".
     * @return
     */
    public fun filterGeoDistanceRange(wheres: MutableList<QueryBuilder>, name: String, lat: Double, lon: Double, distanceFrom: String, distanceTo: String): EsQueryBuilder {
        return whereGeoDistanceRange(this.filter, name, lat, lon, distanceFrom, distanceTo)
    }

    /**
     * Opens a filter sub clauses
     * @return
     */
    public fun filterOpen(): EsQueryBuilder {
        return whereOpen(this.filter)
    }

    /**
     * Opens a filter sub clauses
     * @return
     */
    public fun filterClose(): EsQueryBuilder {
        return whereClose(EsQueryBuilder::filter)
    }

    /**
     * Wrap a filter sub clauses
     * @param action
     * @return
     */
    public fun filterWrap(action: EsQueryBuilder.() -> Unit): EsQueryBuilder {
        filterOpen()
        this.action()
        filterClose()
        return this
    }

    /**
     * Opens a filter nested sub clauses
     * @param path
     * @return
     */
    public fun filterNestedOpen(path: String): EsQueryBuilder {
        return whereNestedOpen(this.filter, path)
    }

    /**
     * Opens a filter nested sub clauses
     * @return
     */
    public fun filterNestedClose(): EsQueryBuilder {
        return whereClose(EsQueryBuilder::filter)
    }

    /**
     * Wrap a filter nested sub clauses
     * @param path
     * @param action
     * @return
     */
    public fun filterNestedWrap(path: String, action: EsQueryBuilder.() -> Unit): EsQueryBuilder {
        filterNestedOpen(path)
        this.action()
        filterNestedClose()
        return this
    }
    // --------- filter end ---------


    // --------- must start ---------
    /**
     * Set the query must clause
     * @param condition
     * @return this
     */
    public fun must(condition: QueryBuilder): EsQueryBuilder {
        must.add(condition)
        return this
    }

    /**
     * Set the query must clause
     * @param name
     * @param value
     * @return this
     */
    public fun must(name: String, value: Any?): EsQueryBuilder {
        return must(name, prepareOperator(value), value)
    }

    /**
     * Set the query must clause
     * @param name
     * @param operator
     * @param value
     * @return this
     */
    public fun must(name: String, operator: String, value: Any?): EsQueryBuilder {
        return where(this.must, name, operator, value)
    }

    /**
     * Set the query must exists clause
     * @param name
     * @return this
     */
    public fun mustExists(name: String): EsQueryBuilder {
        return must(name, "exists", null)
    }

    /**
     * Set the query must between clause
     * @param name
     * @param from
     * @param to
     * @return this
     */
    public fun mustBetween(name: String, from: Any, to: Any): EsQueryBuilder {
        return whereBetween(this.must, name, from, to)
    }

    /**
     * Add a condition to find documents which are some distance away from the given geo point.
     *
     * @see https://www.elastic.co/guide/en/elasticsearch/reference/2.4/query-dsl-geo-distance-query.html
     *
     * @param name A name of the field.
     * @param lat geo point
     * @param lon geo point
     * @param distance A distance from the starting geo point. It can be for example "20km".
     * @return this
     */
    public fun mustGeoDistance(name: String, lat: Double, lon: Double, distance: String): EsQueryBuilder {
        return whereGeoDistance(this.must, name, lat, lon, distance)
    }

    /**
     * 搜索矩形(top,left,bottom,right)范围内的坐标点
     * @param name
     * @param top
     * @param left
     * @param bottom
     * @param right
     * @return
     */
    public fun mustGeoBoundingBox(wheres: MutableList<QueryBuilder>, name: String, top: Double, left: Double, bottom: Double, right: Double): EsQueryBuilder {
        return whereGeoBoundingBox(this.must, name, top, left, bottom, right)
    }

    /**
     * 搜索与指定点距离在给定最小距离和最大距离之间的点
     * @param name A name of the field.
     * @param lat geo point
     * @param lon geo point
     * @param distanceFrom A distance from the starting geo point. It can be for example "20km".
     * @param distanceTo A distance to the starting geo point. It can be for example "20km".
     * @return
     */
    public fun mustGeoDistanceRange(wheres: MutableList<QueryBuilder>, name: String, lat: Double, lon: Double, distanceFrom: String, distanceTo: String): EsQueryBuilder {
        return whereGeoDistanceRange(this.must, name, lat, lon, distanceFrom, distanceTo)
    }

    /**
     * Opens a must sub clauses
     * @return
     */
    public fun mustOpen(): EsQueryBuilder {
        return whereOpen(this.must)
    }

    /**
     * Opens a must sub clauses
     * @return
     */
    public fun mustClose(): EsQueryBuilder {
        return whereClose(EsQueryBuilder::must)
    }

    /**
     * Wrap a must sub clauses
     * @param action
     * @return
     */
    public fun mustWrap(action: EsQueryBuilder.() -> Unit): EsQueryBuilder {
        mustOpen()
        this.action()
        mustClose()
        return this
    }

    /**
     * Opens a must nested sub clauses
     * @param path
     * @return
     */
    public fun mustNestedOpen(path: String): EsQueryBuilder {
        return whereNestedOpen(this.must, path)
    }

    /**
     * Opens a must nested sub clauses
     * @return
     */
    public fun mustNestedClose(): EsQueryBuilder {
        return whereClose(EsQueryBuilder::must)
    }

    /**
     * Wrap a must nested sub clauses
     * @param path
     * @param action
     * @return
     */
    public fun mustNestedWrap(path: String, action: EsQueryBuilder.() -> Unit): EsQueryBuilder {
        mustNestedOpen(path)
        this.action()
        mustNestedClose()
        return this
    }
    // --------- must end ---------



    // --------- mustNot start ---------
    /**
     * Set the query mustNot clause
     * @param condition
     * @return this
     */
    public fun mustNot(condition: QueryBuilder): EsQueryBuilder {
        mustNot.add(condition)
        return this
    }

    /**
     * Set the query mustNot clause
     * @param name
     * @param value
     * @return this
     */
    public fun mustNot(name: String, value: Any?): EsQueryBuilder {
        return mustNot(name, prepareOperator(value), value)
    }

    /**
     * Set the query mustNot clause
     * @param name
     * @param operator
     * @param value
     * @return this
     */
    public fun mustNot(name: String, operator: String, value: Any?): EsQueryBuilder {
        return where(this.mustNot, name, operator, value)
    }

    /**
     * Set the query mustNot exists clause
     * @param name
     * @return this
     */
    public fun mustNotExists(name: String): EsQueryBuilder {
        return mustNot(name, "exists", null)
    }

    /**
     * Set the query mustNot between clause
     * @param name
     * @param from
     * @param to
     * @return this
     */
    public fun mustNotBetween(name: String, from: Any, to: Any): EsQueryBuilder {
        return whereBetween(this.mustNot, name, from, to)
    }

    /**
     * Add a condition to find documents which are some distance away from the given geo point.
     *
     * @see https://www.elastic.co/guide/en/elasticsearch/reference/2.4/query-dsl-geo-distance-query.html
     *
     * @param name A name of the field.
     * @param lat geo point
     * @param lon geo point
     * @param distance A distance from the starting geo point. It can be for example "20km".
     * @return this
     */
    public fun mustNotGeoDistance(name: String, lat: Double, lon: Double, distance: String): EsQueryBuilder {
        return whereGeoDistance(this.mustNot, name, lat, lon, distance)
    }

    /**
     * 搜索矩形(top,left,bottom,right)范围内的坐标点
     * @param name
     * @param top
     * @param left
     * @param bottom
     * @param right
     * @return
     */
    public fun mustNotGeoBoundingBox(wheres: MutableList<QueryBuilder>, name: String, top: Double, left: Double, bottom: Double, right: Double): EsQueryBuilder {
        return whereGeoBoundingBox(this.mustNot, name, top, left, bottom, right)
    }

    /**
     * 搜索与指定点距离在给定最小距离和最大距离之间的点
     * @param name A name of the field.
     * @param lat geo point
     * @param lon geo point
     * @param distanceFrom A distance from the starting geo point. It can be for example "20km".
     * @param distanceTo A distance to the starting geo point. It can be for example "20km".
     * @return
     */
    public fun mustNotGeoDistanceRange(wheres: MutableList<QueryBuilder>, name: String, lat: Double, lon: Double, distanceFrom: String, distanceTo: String): EsQueryBuilder {
        return whereGeoDistanceRange(this.mustNot, name, lat, lon, distanceFrom, distanceTo)
    }

    /**
     * Opens a mustNot sub clauses
     * @return
     */
    public fun mustNotOpen(): EsQueryBuilder {
        return whereOpen(this.mustNot)
    }

    /**
     * Opens a mustNot sub clauses
     * @return
     */
    public fun mustNotClose(): EsQueryBuilder {
        return whereClose(EsQueryBuilder::mustNot)
    }

    /**
     * Wrap a mustNot sub clauses
     * @param action
     * @return
     */
    public fun mustNotWrap(action: EsQueryBuilder.() -> Unit): EsQueryBuilder {
        mustNotOpen()
        this.action()
        mustNotClose()
        return this
    }

    /**
     * Opens a mustNot nested sub clauses
     * @param path
     * @return
     */
    public fun mustNotNestedOpen(path: String): EsQueryBuilder {
        return whereNestedOpen(this.mustNot, path)
    }

    /**
     * Opens a mustNot nested sub clauses
     * @return
     */
    public fun mustNotNestedClose(): EsQueryBuilder {
        return whereClose(EsQueryBuilder::mustNot)
    }

    /**
     * Wrap a mustNot nested sub clauses
     * @param path
     * @param action
     * @return
     */
    public fun mustNotNestedWrap(path: String, action: EsQueryBuilder.() -> Unit): EsQueryBuilder {
        mustNotNestedOpen(path)
        this.action()
        mustNotNestedClose()
        return this
    }
    // --------- mustNot end ---------


    // --------- should start ---------
    /**
     * Set the query should clause
     * @param condition
     * @return this
     */
    public fun should(condition: QueryBuilder): EsQueryBuilder {
        should.add(condition)
        return this
    }

    /**
     * Set the query should clause
     * @param name
     * @param value
     * @return this
     */
    public fun should(name: String, value: Any?): EsQueryBuilder {
        return should(name, prepareOperator(value), value)
    }

    /**
     * Set the query should clause
     * @param name
     * @param operator
     * @param value
     * @return this
     */
    public fun should(name: String, operator: String, value: Any?): EsQueryBuilder {
        return where(this.should, name, operator, value)
    }

    /**
     * Set the query should exists clause
     * @param name
     * @return this
     */
    public fun shouldExists(name: String): EsQueryBuilder {
        return should(name, "exists", null)
    }

    /**
     * Set the query should between clause
     * @param name
     * @param from
     * @param to
     * @return this
     */
    public fun shouldBetween(name: String, from: Any, to: Any): EsQueryBuilder {
        return whereBetween(this.should, name, from, to)
    }

    /**
     * Add a condition to find documents which are some distance away from the given geo point.
     *
     * @see https://www.elastic.co/guide/en/elasticsearch/reference/2.4/query-dsl-geo-distance-query.html
     *
     * @param name A name of the field.
     * @param lat geo point
     * @param lon geo point
     * @param distance A distance from the starting geo point. It can be for example "20km".
     * @return this
     */
    public fun shouldGeoDistance(name: String, lat: Double, lon: Double, distance: String): EsQueryBuilder {
        return whereGeoDistance(this.should, name, lat, lon, distance)
    }

    /**
     * 搜索矩形(top,left,bottom,right)范围内的坐标点
     * @param name
     * @param top
     * @param left
     * @param bottom
     * @param right
     * @return
     */
    public fun shouldGeoBoundingBox(wheres: MutableList<QueryBuilder>, name: String, top: Double, left: Double, bottom: Double, right: Double): EsQueryBuilder {
        return whereGeoBoundingBox(this.should, name, top, left, bottom, right)
    }

    /**
     * 搜索与指定点距离在给定最小距离和最大距离之间的点
     * @param name A name of the field.
     * @param lat geo point
     * @param lon geo point
     * @param distanceFrom A distance from the starting geo point. It can be for example "20km".
     * @param distanceTo A distance to the starting geo point. It can be for example "20km".
     * @return
     */
    public fun shouldGeoDistanceRange(wheres: MutableList<QueryBuilder>, name: String, lat: Double, lon: Double, distanceFrom: String, distanceTo: String): EsQueryBuilder {
        return whereGeoDistanceRange(this.should, name, lat, lon, distanceFrom, distanceTo)
    }

    /**
     * Opens a should sub clauses
     * @return
     */
    public fun shouldOpen(): EsQueryBuilder {
        return whereOpen(this.should)
    }

    /**
     * Opens a should sub clauses
     * @return
     */
    public fun shouldClose(): EsQueryBuilder {
        return whereClose(EsQueryBuilder::should)
    }

    /**
     * Wrap a should sub clauses
     * @param action
     * @return
     */
    public fun shouldWrap(action: EsQueryBuilder.() -> Unit): EsQueryBuilder {
        shouldOpen()
        this.action()
        shouldClose()
        return this
    }

    /**
     * Opens a should nested sub clauses
     * @param path
     * @return
     */
    public fun shouldNestedOpen(path: String): EsQueryBuilder {
        return whereNestedOpen(this.should, path)
    }

    /**
     * Opens a should nested sub clauses
     * @return
     */
    public fun shouldNestedClose(): EsQueryBuilder {
        return whereClose(EsQueryBuilder::should)
    }

    /**
     * Wrap a should nested sub clauses
     * @param path
     * @param action
     * @return
     */
    public fun shouldNestedWrap(path: String, action: EsQueryBuilder.() -> Unit): EsQueryBuilder {
        shouldNestedOpen(path)
        this.action()
        shouldNestedClose()
        return this
    }
    // --------- should end ---------

    // --------- agg start ---------
    /**
     * 聚合
     * @param expr 聚合表达式, 如 count(name) / sum(age)
     * @param alias 别名, 如果别名省略, 则自动生成, 会是`函数名_字段名`, 如 count_name/sum_age, 但对于 terms/nested 函数则还是使用字段名作为别名
     * @param asc 是否升序
     * @return
     */
    @JvmOverloads
    public fun aggBy(expr: String, alias: String? = null, asc: Boolean? = null): EsQueryBuilder {
        val exp = AggExpr(expr, alias)
        val agg = exp.toAggregation()
        return aggBy(agg, asc)
    }

    /**
     * 聚合
     * @param agg 聚合对象
     * @param asc 是否升序
     * @return
     */
    @JvmOverloads
    public fun aggBy(agg: AbstractAggregationBuilder<*>, asc: Boolean? = null): EsQueryBuilder {
        // 添加聚合
        this.currAggs.addAggregator(agg)

        // 排序
        if (asc != null)
            aggOrderBy(agg, asc)

        return this
    }

    /**
     *
     */
    protected fun aggOrderBy(agg: AbstractAggregationBuilder<*>, asc: Boolean) {
        // 嵌套字段的全路径 = 父路径 + 本字段名
        var path = agg.getName()
        
        // 逐层往上找 能挂排序的父聚合, 随便收集全路径
        val orderHoldingAgg = getClosetParentAgg { parentAgg ->
            // 1 能挂排序的父聚合
            // Only single-bucket or metrics aggregation(TermsAggregationBuilder/DateHistogramAggregationBuilder/HistogramAggregationBuilder) can hold order
            val orderHolding = (parentAgg is TermsAggregationBuilder
                    || parentAgg is DateHistogramAggregationBuilder
                    || parentAgg is HistogramAggregationBuilder)

            // 2 对中间的嵌套父聚合, 要收集其路径
            if (parentAgg is NestedAggregationBuilder)
                // 全路径 = 父路径 + 本字段名
                path = parentAgg.path() + '>' + path
            
            orderHolding
        }

        // 预警错误: Invalid aggregation order path [xxx]. Buckets can only be sorted on a sub-aggregator path that is built out of zero or more single-bucket aggregations within the path and a final single-bucket or a metrics aggregation at the path end.
        if(orderHoldingAgg == null)
            throw EsException("Cannot find single-bucket or metrics aggregation(TermsAggregationBuilder/DateHistogramAggregationBuilder/HistogramAggregationBuilder) to hold order [" + agg.getName() + "," + (if(asc) "ASC" else "DESC") + "]")

        // 挂排序字段
        when (orderHoldingAgg) {
            is TermsAggregationBuilder ->
                orderHoldingAgg.order(Terms.Order.aggregation(path, asc))
            is DateHistogramAggregationBuilder ->
                orderHoldingAgg.order(Histogram.Order.aggregation(path, asc))
            is HistogramAggregationBuilder ->
                orderHoldingAgg.order(Histogram.Order.aggregation(path, asc))
        }
    }

    /**
     * 聚合+包含子聚合回调
     * @param expr 聚合表达式, 如 count(name), sum(age)
     * @param alias 别名
     * @param asc 是否升序
     * @param subAggAction
     * @return
     */
    @JvmOverloads
    public fun aggByAndWrapSubAgg(expr: String, alias: String? = null, asc: Boolean? = null, subAggAction: EsQueryBuilder.() -> Unit): EsQueryBuilder {
        aggBy(expr, alias, asc)
        subAggWrap(subAggAction)
        return this
    }

    /**
     * Open a agg sub clauses
     * @return
     */
    public fun subAggOpen(): EsQueryBuilder {
        aggsStack.push(nextLevelAggs to currAggs)
        return this
    }

    /**
     * Close a agg sub clauses
     * @return
     */
    public fun subAggClose(): EsQueryBuilder {
        // 出栈, 获得上一层的agg对象
        val (_, preAggs) = aggsStack.pop()
        // 出栈后的当前层的agg对象
        //this.currAggs
        // 两者应该相等
        if(currAggs != preAggs)
            throw EsException("Close not match last open")

        return this
    }

    /**
     * Wrap a agg sub clauses
     * @param action
     * @return
     */
    public fun subAggWrap(action: EsQueryBuilder.() -> Unit): EsQueryBuilder {
        subAggOpen()
        this.action()
        subAggClose()
        return this
    }
    // --------- agg end ---------

    /**
     * Get highlight result
     * @return this
     */
    public fun highlight(vararg fields: String): EsQueryBuilder {
        highlightFields.addAll(fields)
        return this;
    }

    /**
     * add field script
     *   官方说明 http://blog.bootsphp.com/elasticsearch_use_script_fields_with_source
     *   保留_source字段的+返回脚本字段(script_fields) http://blog.bootsphp.com/elasticsearch_use_script_fields_with_source
     *
     * @param field
     * @param script
     * @param params
     * @return
     */
    @JvmOverloads
    fun addFieldScript(field: String, script: String, params: Map<String, Any?> = emptyMap()): EsQueryBuilder {
        fieldScripts[field] = Script(ScriptType.INLINE, "painless", script, params)
        return this
    }

    /**************************** 转换查询条件 ****************************/
    /**
     * 转查询对象
     */
    internal fun toQuery(): QueryBuilder {
        if(queryStack.size > 1)
            throw EsException("No close for " + currQuery)

        return currQuery
    }

    /**
     * 转搜索参数
     * @param logging 是否打印日志, 仅测试时用
     * @return
     */
    public fun toSearchSource(logging: Boolean = true): String {
        val sourceBuilder = SearchSourceBuilder()

        // 前置过滤
        if(hasQuery)
            sourceBuilder.query(this.toQuery())

        // 分页
        if (this.limit > 0)
            sourceBuilder.size(this.limit)
        if(this.offset > 0)
            sourceBuilder.from(this.offset)

        // 后置过滤
        if (this.postFilter != null)
            sourceBuilder.postFilter(this.postFilter)

        // 请求超时
        if(this.timeout > 0)
            sourceBuilder.timeout(TimeValue(this.timeout, TimeUnit.SECONDS))

        // 排序
        for (sort in this.sorts) {
            sourceBuilder.sort(sort)
        }

        // 属性
        if (this.includeFields.isNotEmpty() || this.excludeFields.isNotEmpty()) {
            sourceBuilder.fetchSource(this.includeFields.toTypedArray(), this.excludeFields.toTypedArray())
        }

        // 分数
        if (this.minScore > 0)
            sourceBuilder.minScore(this.minScore)

        // 聚合: 反射设置属性
        // SearchSourceBuilder.aggregations 属性 是 AggregatorFactories.Builder
        if(aggsStack.size > 1)
            throw EsException("No close for " + currAggs)
        if(currAggs.count() > 0) // 有才设置
            sourceBuilder.setAggregations(currAggs)

        // 高亮字段
        if(highlightFields.isNotEmpty()) {
            val highlighter = SearchSourceBuilder.highlight()
            for (f in this.highlightFields) {
                highlighter.field(f)
            }
            sourceBuilder.highlighter(highlighter)
        }

        // 字段脚本
        for ((field, script) in fieldScripts) {
            sourceBuilder.scriptField(field, script)
        }

        // 转查询字符串
        val query = sourceBuilder.toString()
        if(logging)
            esLogger.debug("查询条件:{}", query)
        return query
    }

    /**************************** 查询 ****************************/
    /**
     * 统计行数
     */
    public fun count(): Long {
        return esmgr.count(index, type, this)
    }

    /**
     * 从model类中注解获得并设置index/type
     */
    protected fun initIndexTypeFromClass(clazz: Class<*>){
        if(indexInited)
            return

        val adoc = clazz.kotlin.esDoc // @EsDoc 注解
        if(adoc == null)
            return

        index = adoc.index
        type = adoc.type
    }

    /**
     * 搜索单个文档
     * @param clazz
     * @return
     */
    fun <T> searchDoc(clazz: Class<T>): T? {
        initIndexTypeFromClass(clazz)
        return esmgr.searchDoc(index, type, this, clazz)
    }

    /**
     * 搜索多个文档
     * @param clazz
     * @return
     */
    fun <T> searchDocs(clazz: Class<T>): Pair<List<T>, Long> {
        initIndexTypeFromClass(clazz)
        return esmgr.searchDocs(index, type, this, clazz)
    }

    /**
     * 搜索多个文档
     * @return
     */
    public fun searchDocs(): SearchResult {
        return esmgr.searchDocs(index, type, this)
    }

    /**
     * 开始搜索文档, 并返回有游标的结果集合
     *   分页无效
     *
     * @param clazz bean类, 可以是HashMap
     * @param pageSize
     * @param scrollTimeInMillis
     * @return
     */
    @JvmOverloads
    fun <T> scrollDocs(clazz: Class<T>, pageSize: Int = 1000, scrollTimeInMillis: Long = 3000): EsScrollCollection<T> {
        initIndexTypeFromClass(clazz)
        return esmgr.scrollDocs(index, type, this, clazz, pageSize, scrollTimeInMillis)
    }

    /**
     * 通过查询批量删除文档
     *
     * @param pageSize
     * @param scrollTimeInMillis
     * @return 被删除的id
     */
    @JvmOverloads
    fun deleteDocs(pageSize: Int = 1000, scrollTimeInMillis: Long = 3000): Collection<String> {
        return esmgr.deleteDocsByQuery2(index, type, this, pageSize, scrollTimeInMillis)
    }

    /**
     * 通过查询来批量更新
     *    参考 https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update-by-query.html
     *
     * @param script 脚本
     * @param params 脚本参数
     * @param pageSize
     * @param scrollTimeInMillis
     * @return
     */
    @JvmOverloads
    fun updateDocs(script: String, params: Map<String, Any?> = emptyMap(), pageSize: Int = 1000, scrollTimeInMillis: Long = 3000): UpdateByQueryResult {
        return esmgr.updateDocsByQuery(index, type, script, this, params, pageSize, scrollTimeInMillis)
    }

    /**
     * 通过查询来自增
     *
     * @param field 字段
     * @param step 自增步长
     * @return
     */
    @JvmOverloads
    fun increment(field: String, step: Int = 1): UpdateByQueryResult {
        return this.updateDocs("ctx._source.${field} += params.step", mapOf(
            "step" to step
        ));
    }

    /**
     * 通过查询来自减
     *
     * @param field 字段
     * @param step 自减步长
     * @return
     */
    @JvmOverloads
    fun decrement(field: String, step: Int = 1): UpdateByQueryResult {
        return this.updateDocs("ctx._source.${field} -= params.step", mapOf(
            "step" to step
        ));
    }
}
