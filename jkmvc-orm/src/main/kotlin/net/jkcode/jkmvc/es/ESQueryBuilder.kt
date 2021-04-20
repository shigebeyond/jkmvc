package net.jkcode.jkmvc.es

import io.searchbox.params.SearchType
import net.jkcode.jkmvc.db.DbException
import net.jkcode.jkutil.common.esLogger
import net.jkcode.jkutil.common.isArrayOrCollection
import net.jkcode.jkutil.common.isArrayOrCollectionEmpty
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import java.util.concurrent.TimeUnit


/**
 * 查询构建器
 *
 * @author shijianhang
 * @date 2021-4-21 下午5:16:59
 */
class ESQueryBuilder private constructor(
        protected val parent: ESQueryBuilder? = null,
        protected val path: String? = null
) {

    companion object{

        /**
         * Filter operators
         * @var List
         */
        protected val operators = arrayOf(
                "=", // term
                "IN", // terms
                "!=",
                ">",
                ">=",
                "<",
                "<=",
                "like",
                "exists"
        )
    }


    public constructor() : this(null, null)

    /**
     * Query index name
     */
    protected lateinit var index: String;

    /**
     * Query type name
     */
    protected lateinit var type: String

    /**
     * Query type key
     */
    protected lateinit var id: String

    /**
     * Query bool filter
     */
    protected val filter = ArrayList<QueryBuilder>();

    /**
     * Query bool must
     */
    protected val must = ArrayList<QueryBuilder>()

    /**
     * Query bool must not
     * @var List
     */
    protected val mustNot = ArrayList<QueryBuilder>()

    /**
     * Query bool should
     */
    protected val should = ArrayList<QueryBuilder>()
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
     * Query sort fields
     * @var List
     */
    protected val sorts = ArrayList<Pair<String, SortOrder>>();

    /**
     * 最低分
     */
    protected var minScore = 0f

    /**
     * 聚合
     */
    protected val aggExprs = ArrayList<AggExpr>()

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
     * @var int
     */
    protected var offset = 0;

    /**
     * 清空查询元素
     */
    fun clear(){
        index = ""
        type = ""
        id = ""
        filter.clear()
        must.clear()
        mustNot.clear()
        should.clear()
        includeFields.clear()
        excludeFields.clear()
        highlightFields.clear()
        sorts.clear()
        minScore = 0f
        aggExprs.clear()
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
    public fun index(index: String): ESQueryBuilder {
        this.index = index;
        return this
    }

    /**
     * Set the type name
     * @param type
     * @return this
     */
    public fun type(type: String): ESQueryBuilder {
        this.type = type;
        return this;
    }

    /**
     * Set the query search type
     * @param type
     * @return this
     */
    public fun searchType(type: SearchType): ESQueryBuilder {
        this.searchType = type;
        return this
    }

    /**
     * Set the query limit
     * @param int limit
     * @return this
     */
    public fun limit(limit: Int = 10): ESQueryBuilder {
        this.limit = limit;
        return this;
    }

    /**
     * Set the query offset
     * @param int offset
     * @return this
     */
    public fun offset(offset: Int = 0): ESQueryBuilder {
        this.offset = offset;
        return this;
    }

    /**
     * Set the sorting field
     * @param field
     * @param direction 方向: ASC/DESC
     * @return this
     */
    public fun orderBy(field: String, direction: String): ESQueryBuilder {
        this.sorts.add(field to SortOrder.valueOf(direction.toUpperCase()))
        return this;
    }

    /**
     * Set the sorting field
     * @param field
     * @param string direction
     * @return this
     */
    public fun orderBy(field: String, desc: Boolean = false): ESQueryBuilder {
        this.sorts.add(field to if (desc) SortOrder.DESC else SortOrder.ASC)
        return this;
    }

    /**
     * check if it"s a valid operator
     * @param op
     * @return bool
     */
    protected fun isOperator(op: String): Boolean {
        return operators.contains(op)
    }

    /**
     * Set the query fields to return
     * @param fields
     * @return this
     */
    public fun select(vararg fields: String): ESQueryBuilder {
        this.includeFields.addAll(fields)
        return this;
    }

    /**
     * Set the ignored fields to not be returned
     * @param fields
     * @return this
     */
    public fun unselect(vararg fields: String): ESQueryBuilder {
        this.excludeFields.addAll(fields)
        this.includeFields.removeAll(fields)
        return this;
    }

    /**
     * Filter by _id
     * @param id
     * @return this
     */
    public fun id(id: String): ESQueryBuilder {
        this.id = id;

        val query = QueryBuilders.termQuery("_id", id)
        this.filter.add(query)
        return this;
    }

    protected fun build1Condition(name: String, operator: String, value: Any?): QueryBuilder {
        if (operator == "=") // term
            return QueryBuilders.termQuery(name, value)

        if (operator.equals("IN", true)) { // terms
            return when(value){
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

        if (operator.equals("like", true))
            return QueryBuilders.matchQuery(name, value)

        throw IllegalArgumentException("Unkown operator")
    }

    /**
     * Prepare operator
     *
     * @param   value   column value
     * @return
     */
    protected fun prepareOperator(value: Any?): String {
        if(value != null && value.isArrayOrCollection()) // 数组/集合
            return "IN"

        return "=";
    }

    /**
     * Set the query where clause
     * @param name
     * @param value
     * @return this
     */
    public fun where(name: String, value: Any?): ESQueryBuilder {
        return where(name, prepareOperator(value), value)
    }

    /**
     * Set the query where clause
     * @param name
     * @param operator
     * @param value
     * @return this
     */
    public fun where(name: String, operator: String, value: Any?): ESQueryBuilder {
        if(!isOperator(operator))
            throw IllegalArgumentException("Unkown operator: $operator")

        if (operator == "=" && name == "_id")
            return this.id(value.toString())

        val condition = build1Condition(name, operator, value)
        if (operator == "like")
            this.must.add(condition)
        else
            this.filter.add(condition)

        if (operator == "exists")
            this.whereExists(name, value as Boolean);

        return this;
    }


    /**
     * Set the query inverse where clause
     * @param        name
     * @param string operator
     * @param null value
     * @return this
     */
    public fun whereNot(name: String, value: Any?): ESQueryBuilder {
        return whereNot(name, prepareOperator(value), value)
    }

    /**
     * Set the query inverse where clause
     * @param        name
     * @param string operator
     * @param null value
     * @return this
     */
    public fun whereNot(name: String, operator: String, value: Any?): ESQueryBuilder {
        val condition = build1Condition(name, operator, value)
        this.mustNot.add(condition)
        if (operator == "exists")
            this.whereExists(name, !(value as Boolean));

        return this;
    }

    /**
     * Set the query where between clause
     * @param name
     * @param from
     * @param to
     * @return this
     */
    public fun whereBetween(name: String, from: Any, to: Any): ESQueryBuilder {
        val query = QueryBuilders.rangeQuery(name).gte(from).lte(to)
        this.filter.add(query)
        return this;
    }

    /**
     * Set the query where not between clause
     * @param name
     * @param from
     * @param to
     * @return this
     */
    public fun whereNotBetween(name: String, from: Any, to: Any): ESQueryBuilder {
        val query = QueryBuilders.rangeQuery(name).gte(from).lte(to)
        this.mustNot.add(query)
        return this;
    }

    /**
     * Set the query where exists clause
     * @param      name
     * @param bool exists
     * @return this
     */
    public fun whereExists(name: String, exists: Boolean = true): ESQueryBuilder {
        val query = QueryBuilders.existsQuery(name)
        if (exists)
            this.must.add(query)
        else
            this.mustNot.add(query)
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
    public fun distance(name: String, lat: Double, lon: Double, distance: String): ESQueryBuilder {
        val query = QueryBuilders.geoDistanceQuery(name).point(lat, lon).distance(distance);
        this.filter.add(query)
        return this;
    }

    /**
     * 聚合
     * @param expr 聚合表达式, 如 count(name), sum(age)
     * @param alias 别名
     * @param asc 是否升序
     */
    public fun aggBy(expr: String, alias: String? = null, asc: Boolean? = null): ESQueryBuilder {
        this.aggExprs.add(AggExpr(expr, alias, asc))
        return this
    }

    /**
     * 嵌套进-
     * @return 返回下一层query builder
     */
    public fun nestedDown(path: String): ESQueryBuilder {
        return ESQueryBuilder(this, path)
    }

    /**
     * 嵌套退-
     * @return 返回上一层query builder
     */
    public fun nestedUp(): ESQueryBuilder {
        val nestedQuery = QueryBuilders.nestedQuery(path, toQuery(), ScoreMode.Total)
        parent!!.must.add(nestedQuery)
        return parent!!
    }

    /**
     * Get highlight result
     * @return this
     */
    public fun highlight(vararg fields: String): ESQueryBuilder {
        highlightFields.addAll(fields)
        return this;
    }

    /**
     * 转查询对象
     */
    protected fun toQuery(): QueryBuilder? {
        val query = QueryBuilders.boolQuery()

        //filter容器
        for (condition in filter) {
            query.filter(condition)
        }
        //must容器
        for (condition in must) {
            query.must(condition)
        }
        //should容器
        for (condition in should) {
            query.should(condition)
        }
        //must not 容器
        for (condition in mustNot) {
            query.mustNot(condition)
        }
        return query
    }

    /**
     * 转搜索参数
     */
    public fun toSearchSource(): String {
        val sourceBuilder = SearchSourceBuilder()

        // 前置过滤
        sourceBuilder.query(this.toQuery())

        // 分页
        if(this.limit > 0) {
            sourceBuilder.from(this.offset)
            sourceBuilder.size(this.limit)
        }

        // 后置过滤
        if (this.postFilter != null)
            sourceBuilder.postFilter(this.postFilter)

        sourceBuilder.timeout(TimeValue(60, TimeUnit.SECONDS))

        //增加多个值排序
        for ((field, order) in this.sorts) {
            val sort = SortBuilders.fieldSort(field).order(order)
            sourceBuilder.sort(sort)
        }

        //属性
        if (this.includeFields.isNotEmpty() || this.excludeFields.isNotEmpty()) {
            sourceBuilder.fetchSource(this.includeFields.toTypedArray(), this.excludeFields.toTypedArray())
        }

        // 分数
        if (this.minScore > 0)
            sourceBuilder.minScore(this.minScore)

        // 聚合
        var termAgg: TermsAggregationBuilder? = null
        for (expr in this.aggExprs) {
            val agg = expr.toAggregation()
            if (expr.func == "terms") { // 第一个terms聚合, 挂在sourceBuilder下
                if (termAgg == null)
                    sourceBuilder.aggregation(agg)
                else
                    termAgg!!.subAggregation(agg)
                termAgg = agg as TermsAggregationBuilder
            } else { // 其他聚合, 要挂在上一个terms聚合下
                termAgg!!.subAggregation(agg)
            }
        }

        // 聚合的排序
        for (expr in this.aggExprs) {
            if (expr.asc != null)
                termAgg!!.order(Terms.Order.aggregation(expr.alias, expr.asc))
        }

        // 高亮字段
        val highlighter = SearchSourceBuilder.highlight()
        for (f in this.highlightFields) {
            highlighter.field(f)
        }
        sourceBuilder.highlighter(highlighter)

        // 转查询字符串
        val query = sourceBuilder.toString()
        esLogger.debug("查询条件:{}", query)
        return query
    }
}
