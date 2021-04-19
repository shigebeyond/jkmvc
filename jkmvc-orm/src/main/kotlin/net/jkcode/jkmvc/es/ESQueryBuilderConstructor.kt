package net.jkcode.jkmvc.es

import io.netty.handler.codec.smtp.SmtpRequests.data
import io.searchbox.client.JestClient
import io.searchbox.params.SearchType
import org.codehaus.groovy.ast.tools.GeneralUtils.params
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder
import org.elasticsearch.search.sort.SortOrder


/**
 * 查询构建器
 */
class ESQueryBuilderConstructor {

    /**
     * Native elasticsearch client instance
     * @var Connection
     */
    public lateinit var client: JestClient;

    /**
     * Ignored HTTP errors
     * @var List
     */
    public val ignores = HashSet<String>();

    /**
     * Filter operators
     * @var List
     */
    protected val operators = arrayOf(
            "=",
            "IN", // api等同于=, 就是 term 兼容多值
            "!=",
            ">",
            ">=",
            "<",
            "<=",
            "like",
            "exists"
    )

    /**
     * Query index name
     * @var
     */
    protected var index: String;

    /**
     * Query type name
     * @var
     */
    protected var type: String

    /**
     * Query type key
     * @var
     */
    protected var id: String

    /**
     * Query body
     * @var List
     */
    public val body = [];

    /**
     * Query bool filter
     * @var List
     */
    protected val filter = ArrayList<QueryBuilder>();

    /**
     * Query bool must
     * @var List
     */
    public val must = ArrayList<QueryBuilder>()

    /**
     * Query bool must not
     * @var List
     */
    public val mustNot = ArrayList<QueryBuilder>()

    /**
     * Query bool should
     * @var List
     */
    val should = ArrayList<QueryBuilder>()

    /**
     * Query returned fields list
     * @var List
     */
    var includeFields = HashSet<String>()

    /**
     * Query not returned fields list
     * @var List
     */
    var excludeFields = HashSet<String>()


    /**
     * 高亮字段
     */
    internal val highlightFields: ArrayList<String>();

    /**
     * Query sort fields
     * @var List
     */
    internal val sorts = ArrayList<Pair<String, SortOrder>>();

    internal var minScore = 0f

    internal val aggregations = ArrayList<AbstractAggregationBuilder<*>>()

    internal val postFilter: QueryBuilder? = null

    /**
     * Query search type
     * @var int
     */
    internal var searchType = SearchType.DFS_QUERY_THEN_FETCH

    /**
     * Query scroll time
     * @var string
     */
    protected val scroll: String;

    /**
     * Query scroll id
     * @var string
     */
    protected var scrollId: Long;

    /**
     * Query limit
     * @var int
     */
    internal var limit = 10;

    /**
     * Query offset
     * @var int
     */
    internal var offset = 0;

    /**
     * The key that should be used when caching the query.
     * @var string
     */
    protected val cacheKey: String;

    /**
     * The number of minutes to cache the query.
     * @var int
     */
    protected val cacheMinutes: Int;

    /**
     * The cache driver to be used.
     * @var string
     */
    protected val cacheDriver: String;

    /**
     * A cache prefix.
     * @var string
     */
    protected val cachePrefix = "es";

    /**
     * Elastic model instance
     * @var \Basemkhirat\Elasticsearch\Model
     */
    public val model;

    /**
     * Use model global scopes
     * @var bool
     */
    public val useGlobalScopes = true;

    /**
     * Set the index name
     * @param index
     * @return this
     */
    public fun index(index: String): ESQueryBuilderConstructor {
        this.index = index;
        return this
    }

    /**
     * Set the type name
     * @param type
     * @return this
     */
    public fun type(type: String): ESQueryBuilderConstructor {
        this.type = type;
        return this;
    }

    /**
     * Set the query scroll
     * @param string scroll
     * @return this
     */
    public fun scroll(scroll: String): ESQueryBuilderConstructor {
        this.scroll = scroll;
        return this;
    }

    /**
     * Set the query scroll ID
     * @param string scroll
     * @return this
     */
    public fun scrollID(scroll: Long): ESQueryBuilderConstructor {
        this.scrollId = scroll;
        return this;
    }

    /**
     * Set the query search type
     * @param string type
     * @return this
     */
    public fun searchType(type: Int): ESQueryBuilderConstructor {
        this.searchType = type;
        return this
    }

    /**
     * Set the query limit
     * @param int take
     * @return this
     */
    public fun take(take: Int = 10): ESQueryBuilderConstructor {
        this.limit = take;
        return this;
    }

    /**
     * Ignore bad HTTP response
     * @return this
     */
    public fun ignore(vararg args: String): ESQueryBuilderConstructor {
        ignores.addAll(args)
        return this;
    }

    /**
     * Set the query offset
     * @param int offset
     * @return this
     */
    public fun offset(offset: Int = 0): ESQueryBuilderConstructor {
        this.offset = offset;
        return this;
    }

    /**
     * Set the sorting field
     * @param field
     * @param string direction
     * @return this
     */
    public fun orderBy(field: String, direction: String = "ASC"): ESQueryBuilderConstructor {
        this.sorts.add(field to SortOrder.valueOf(direction.toUpperCase()))
        return this;
    }

    /**
     * Set the sorting field
     * @param field
     * @param string direction
     * @return this
     */
    public fun orderBy(field: String, desc: Boolean = false): ESQueryBuilderConstructor {
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
    public fun select(vararg fields: String): ESQueryBuilderConstructor {
        this.includeFields.addAll(fields)
        return this;
    }

    /**
     * Set the ignored fields to not be returned
     * @param fields
     * @return this
     */
    public fun unselect(vararg fields: String): ESQueryBuilderConstructor {
        this.excludeFields.addAll(fields)
        this.includeFields.removeAll(fields)
        return this;
    }

    /**
     * Filter by _id
     * @param id
     * @return this
     */
    public fun id(id: String): ESQueryBuilderConstructor {
        this.id = id;

        val query = QueryBuilders.termQuery("_id", id)
        this.filter.add(query)
        return this;
    }

    protected fun build1Condition(name: String, operator: String = "=", value: Any? = null): QueryBuilder {
        if (operator == "=")
            return QueryBuilders.termQuery(name, value)

        if (operator == "IN")
            return QueryBuilders.termQuery(name, value)

        if (operator == ">")
            return QueryBuilders.rangeQuery(name).gt(value);

        if (operator == ">=")
            return QueryBuilders.rangeQuery(name).gte(value);

        if (operator == "<")
            return QueryBuilders.rangeQuery(name).lt(value);

        if (operator == "<=")
            return QueryBuilders.rangeQuery(name).lte(value);

        if (operator == "like")
            return QueryBuilders.matchQuery(name, value)

        throw IllegalArgumentException("Unkown operator")
    }

    /**
     * Set the query where clause
     * @param        name
     * @param string operator
     * @param null value
     * @return this
     */
    public fun where(name: String, operator: String = "=", value: Any? = null): ESQueryBuilderConstructor {
        if (operator == "=" && name == "_id")
            return this.id(value as String)

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
    public fun whereNot(name: String, operator: String = "=", value: Any? = null): ESQueryBuilderConstructor {
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
    public fun whereBetween(name: String, from: Any, to: Any): ESQueryBuilderConstructor {
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
    public fun whereNotBetween(name: String, from: Any, to: Any): ESQueryBuilderConstructor {
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
    public fun whereExists(name: String, exists: Boolean = true): ESQueryBuilderConstructor {
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
    public fun distance(name: String, lat: Double, lon: Double, distance: String): ESQueryBuilderConstructor {
        val query = QueryBuilders.geoDistanceQuery("name").point(lat, lon).distance(distance);
        this.filter.add(query)
        return this;
    }

    public fun nested(path, query) {
        this.body = [
            "query" to [
                "nested" to [
                    "path" to path
                ]
            ]
        ];
    }

    /**
     * Get highlight result
     * @return this
     */
    public fun highlight(vararg fields: String): ESQueryBuilderConstructor {
        highlightFields.addAll(fields)
        return this;
    }

    /**
     * Generate the query to be executed
     * @return array
     */
    public fun query() {
        val query = [];

        query["index"] = this.index;

        if (this.type) {
            query["type"] = this.type;
        }

        if (this.model && this.useGlobalScopes) {
            this.model.boot(this);
        }

        query["body"] = this.getBody();

        query["from"] = this.offset;

        query["size"] = this.limit;

        if (count(this.ignores)) {
            query["client"] = ["ignore" to this.ignores];
        }

        searchType = this.searchType;

        if (searchType) {
            query["search_type"] = searchType;
        }

        val scroll = this.scroll;
        if (scroll) {
            query["scroll"] = scroll;
        }

        return query;
    }

    /**
     * Clear scroll query id
     * @param string scrollId
     * @return array|Collection
     */
    public fun clear(scrollId: Long = null) {

        scrollId = !is_null(scrollId) ? scrollId : this.scrollId;

        return this.client.clearScroll([
            "scroll_id" to scrollId,
            "client" to ["ignore" to this.ignores]
        ]);
    }

    /**
     * Get the collection of results
     * @param string scrollId
     * @return array|Collection
     */
    public fun get(scrollId = null) {

        scrollId = null;

        result = this.getResult(scrollId);

        return this.getAll(result);
    }

    /**
     * Get the first object of results
     * @param string scrollId
     * @return Model|object
     */
    public fun first(scrollId: Long = null) {

        this.take(1);
        val result = this.getResult(scrollId);
        return this.getFirst(result);
    }

    /**
     * Get query result
     * @param scrollId
     * @return mixed
     */
    protected fun getResult(scrollId) {

        if (is_null(this.cacheMinutes)) {
            result = this.response(scrollId);
        } else {

            result = app("cache").driver(this.cacheDriver).get(this.getCacheKey());

            if (is_null(result)) {
                result = this.response(scrollId);
            }
        }

        return result;
    }


    /**
     * Get non cached results
     * @param null scrollId
     * @return mixed
     */
    public fun response(scrollId = null) {

        scrollId = !is_null(scrollId) ? scrollId : this.scrollId;

        if (scrollId) {

            result = this.client.scroll([
                "scroll" to this.scroll,
                "scroll_id" to scrollId
            ]);

        } else {
            result = this.client.search(this.query());
        }

        if (!is_null(this.cacheMinutes)) {
            app("cache").driver(this.cacheDriver).put(this.getCacheKey(), result, this.cacheMinutes);
        }

        return result;
    }

    /**
     * Get the count of result
     * @return mixed
     */
    public fun count() {

        query = this.query();

        // Remove unsupported count query keys

        unset(
                query["size"],
                query["from"],
                query["body"]["_source"],
                query["body"]["sort"]
        );

        return this.client.count(query)["count"];
    }

    fun build(): QueryBuilder? {
        val boolQueryBuilder = QueryBuilders.boolQuery()
        var queryBuilder: QueryBuilder? = null

        //must容器
        for (condition in must) {
            boolQueryBuilder.must(condition)
        }
        //should容器
        for (condition in should) {
            boolQueryBuilder.should(condition)

        }
        //must not 容器
        for (condition in mustNot) {
            boolQueryBuilder.mustNot(condition)
        }
        return queryBuilder
    }

    /**
     * Retrieve all records
     * @param array result
     * @return array|Collection
     */
    protected fun getAll(result = []) {

        if (array_key_exists("hits", result)) {

            = [];

            for (row in result["hits"]["hits"]) {

                model = this.model ? this.model(row["_source"], true) : Model(row["_source"], true);

                model.setConnection(model.getConnection());
                model.setIndex(row["_index"]);
                model.setType(row["_type"]);

                // match earlier version

                model.index = row["_index"];
                model.type = row["_type"];
                model.id = row["_id"];
                model.score = row["_score"];
                model.highlight = isset(row["highlight"]) ? row["highlight"] : [];

                new.add(model)
            }

            = Collection(new);

            total = result["hits"]["total"];

            new.total = is_mapOf(total) ? total["value"] : total;
            new.maxScore = result["hits"]["max_score"];
            new.took = result["took"];
            new.timedOut = result["timed_out"];
            new.scrollId = isset(result["_scroll_id"]) ? result["_scroll_id"] : null;
            new.shards = (object) result ["_shards"];

            return new;

        } else {
            return Collection([]);
        }
    }

    /**
     * Retrieve only first record
     * @param array result
     * @return Model|object
     */
    protected fun getFirst(result = []) {

        if (array_key_exists("hits", result) && count(result["hits"]["hits"])) {

            data = result["hits"]["hits"];

            if (this.model) {
                model = this.model(data[0]["_source"], true);
            } else {
                model = Model(data[0]["_source"], true);
                model.setConnection(model.getConnection());
                model.setIndex(data[0]["_index"]);
                model.setType(data[0]["_type"]);
            }

            // match earlier version

            model.index = data[0]["_index"];
            model.type = data[0]["_type"];
            model.id = data[0]["_id"];
            model.score = data[0]["_score"];
            model.highlight = isset(data[0]["highlight"]) ? data[0]["highlight"] : [];


            = model;

        } else {
            = null;
        }

        return new;
    }

    /**
     * Paginate collection of results
     * @param int perPage
     * @param      pageName
     * @param null page
     * @return Pagination
     */
    public fun paginate(perPage = 10, pageName = "page", page = null) {

        // Check if the request from PHP CLI
        if (php_sapi_name() == "cli") {
            this.take(perPage);
            page = page ?: 1;
            this.offset((page * perPage) - perPage);
            objects = this.get();
            return Pagination(objects, objects.total, perPage, page);
        }

        this.take(perPage);

        page = page ?: Request.get(pageName, 1);

        this.offset((page * perPage) - perPage);

        objects = this.get();

        return Pagination(objects, objects.total, perPage, page, ["path" to Request.url(), "query" to Request.query()]);
    }

    /**
     * Insert a document
     * @param      data
     * @param null id
     * @return object
     */
    public fun insert(data, id: String = null) {

        if (id) {
            this.id = id;
        }

        parameters = [
            "body" to data,
            "client" to ["ignore" to this.ignores]
        ];

        if (index = this.index) {
            parameters["index"] = index;
        }

        if (type = this.type) {
            parameters["type"] = type;
        }

        if (this.id) {
            parameters["id"] = this.id;
        }

        return (object)this.client.index(parameters);
    }

    /**
     * Insert a bulk of documents
     * @param data multidimensional array of [id to data] pairs
     * @return object
     */
    public fun bulk(data) {

        if (is_callback_function(data)) {

            bulk = Bulk(this);

            data(bulk);

            params = bulk.body();

        } else {

            params = [];

            for ((key, value) in data) {

                params["body"][] = [

                    "index" to [
                        "_index" to this.index,
                        "_type" to this.type,
                        "_id" to key
                    ]

                ];

                params["body"].add(value)

            }

        }

        return (object)this.client.bulk(params);
    }

    /**
     * Update a document
     * @param      data
     * @param null id
     * @return object
     */
    public fun update(data, id = null) {
        if (id) {
            this.id = id;
        }

        val parameters = mapOf(
                "id" to this.id,
                "body" to ["doc" to data],
                "client" to ["ignore" to this.ignores]
        )

        if (index = this.index) {
            parameters["index"] = index;
        }

        if (type = this.type) {
            parameters["type"] = type;
        }

        return (object)this.client.update(parameters);
    }


    /**
     * Increment a document field
     * @param     field
     * @param int count
     * @return object
     */
    public fun increment(field: String, count: Int = 1) {

        return this.script("ctx._source.${field} += params.count", [
            "count" to count
        ]);
    }

    /**
     * Increment a document field
     * @param     field
     * @param int count
     * @return object
     */
    public fun decrement(field: String, count: Int = 1) {

        return this.script("ctx._source.${field} -= params.count", [
            "count" to count
        ]);
    }

    /**
     * Update by script
     * @param       script
     * @param array params
     * @return object
     */
    public fun script(script, params = []) {

        val parameters = mapOf(
                "id" to this.id,
                "body" to [
                    "script" to [
                        "inline" to script,
                        "params" to params
                    ]
                ],
                "client" to ["ignore" to this.ignores]
        )

        if (index = this.index) {
            parameters["index"] = index;
        }

        if (type = this.type) {
            parameters["type"] = type;
        }

        return (object)this.client.update(parameters);
    }

    /**
     * Delete a document
     * @param null id
     * @return object
     */
    public fun delete(id = null) {
        if (id) {
            this.id = id;
        }

        val parameters = mapOf(
                "id" to this.id,
                "client" to ["ignore" to this.ignores]
        )

        if (index = this.index) {
            parameters["index"] = index;
        }

        if (type = this.type) {
            parameters["type"] = type;
        }

        return (object)this.client.delete(parameters);
    }


    /**
     * @return this
     */
    public fun withoutGlobalScopes(): ESQueryBuilderConstructor {
        this.useGlobalScopes = false;
        return this;
    }
}
