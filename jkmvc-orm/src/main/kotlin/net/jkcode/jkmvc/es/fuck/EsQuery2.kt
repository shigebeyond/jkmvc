package net.jkcode.jkmvc.es.fuck

import io.searchbox.client.JestClient
import org.elasticsearch.index.query.QueryBuilder

/**
 * 查询构建器
 */
class Query
{

    /**
     * Native elasticsearch client instance
     * @var Connection
     */
    public lateinit var client: JestClient;

    /**
     * Ignored HTTP errors
     * @var array
     */
    public val ignores = HashSet<String>();

    /**
     * Filter operators
     * @var array
     */
    protected val operators = arrayOf(
        "=",
        "!=",
        ">",
        ">=",
        "<",
        "<=",
        "like",
        "exists"
    )

    /**
     * Query array
     * @var
     */
    protected val query;

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
     * @var array
     */
    public val body = [];

    /**
     * Query bool filter
     * @var array
     */
    protected val filter = ArrayList<QueryBuilder>();

    /**
     * Query bool must
     * @var array
     */
    public val must = ArrayList<QueryBuilder>()

    /**
     * Query bool must not
     * @var array
     */
    public val mustNot = ArrayList<QueryBuilder>()

    /**
     * Query returned fields list
     * @var array
     */
    protected val source = mapOf(
        "include" to [],
        "exclude" to []
    );

    /**
     * Query sort fields
     * @var array
     */
    protected val sort = [];

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
     * Query search type
     * @var int
     */
    protected var searchType: Int;

    /**
     * Query limit
     * @var int
     */
    protected var take = 10;

    /**
     * Query offset
     * @var int
     */
    protected var skip = 0;

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
    public fun index(index: String): Query {
        this.index = index;
        return this
    }

    /**
     * Set the type name
     * @param type
     * @return this
     */
    public fun type(type: String): Query {
        this.type = type;
        return this;
    }

    /**
     * Set the query scroll
     * @param string scroll
     * @return this
     */
    public fun scroll(scroll: String): Query {
        this.scroll = scroll;
        return this;
    }

    /**
     * Set the query scroll ID
     * @param string scroll
     * @return this
     */
    public fun scrollID(scroll: Long): Query {
        this.scrollId = scroll;
        return this;
    }

    /**
     * Set the query search type
     * @param string type
     * @return this
     */
    public fun searchType(type: Int): Query {
        this.searchType = type;
        return this
    }

    /**
     * Set the query limit
     * @param int take
     * @return this
     */
    public fun take(take: Int = 10): Query {
        this.take = take;
        return this;
    }

    /**
     * Ignore bad HTTP response
     * @return this
     */
    public fun ignore(vararg args: String): Query {

        ignores.addAll(args)
        return this;
    }

    /**
     * Set the query offset
     * @param int skip
     * @return this
     */
    public fun skip(skip: Int = 0): Query {
        this.skip = skip;
        return this;
    }

    /**
     * Set the sorting field
     * @param        field
     * @param string direction
     * @return this
     */
    public fun orderBy(field: String, direction: String = "asc")
    {
        this.sort.add([field to direction])
        return this;
    }

    /**
     * check if it"s a valid operator
     * @param op
     * @return bool
     */
    protected fun isOperator(op: String)
    {
        return operators.contains(op)
    }

    /**
     * Set the query fields to return
     * @return this
     */
    public fun select(vararg fields: String)
    {
        this.source["include"] = array_unique(array_merge(this.source["include"], fields));
        this.source["exclude"] = array_values(array_filter(this.source["exclude"], function (field) {
            return !in_mapOf(field, this.source["include"]);
        }));

        return this;
    }

    /**
     * Set the ignored fields to not be returned
     * @return this
     */
    public fun unselect()
    {

        fields = [];

        for(arg in args) {
            if (is_mapOf(arg)) {
                fields = array_merge(fields, arg);
            } else {
                fields.add(arg)
            }
        }

        this.source["exclude"] = array_unique(array_merge(this.source["exclude"], fields));
        this.source["include"] = array_values(array_filter(this.source["include"], function (field) {
            return !in_mapOf(field, this.source["exclude"]);
        }));

        return this;
    }

    /**
     * Filter by _id
     * @param mixed|bool id
     * @return this
     */
    public fun id(id: String): Query {
        this.id = id;
        this.filter.add(["term" to ["_id" to id]])
        return this;
    }

    /**
     * Set the query where clause
     * @param        name
     * @param string operator
     * @param null value
     * @return this
     */
    public fun where(name: String, operator: String = "=", value: Any? = null): Query {
        if (!this.isOperator(operator)) {
            value = operator;
            operator = "=";
        }

        if (operator == "=") {
            if (name == "_id") {
                return this.id(value);
            }

            this.filter.add(["term" to [name to value]])
        }

        if (operator == ">") {
            this.filter.add(["range" to [name to ["gt" to value]]])
        }

        if (operator == ">=") {
            this.filter.add(["range" to [name to ["gte" to value]]])
        }

        if (operator == "<") {
            this.filter.add(["range" to [name to ["lt" to value]]])
        }

        if (operator == "<=") {
            this.filter.add(["range" to [name to ["lte" to value]]])
        }

        if (operator == "like") {
            this.must.add(["match" to [name to value]])
        }

        if (operator == "exists") {
            this.whereExists(name, value);
        }

        return this;
    }

    /**
     * Set the query inverse where clause
     * @param        name
     * @param string operator
     * @param null value
     * @return this
     */
    public fun whereNot(name, operator = "=", value = null): Query {

        if (!this.isOperator(operator)) {
            value = operator;
            operator = "=";
        }

        if (operator == "=") {
            this.mustNot.add(["term" to [name to value]])
        }

        if (operator == ">") {
            this.mustNot.add(["range" to [name to ["gt" to value]]])
        }

        if (operator == ">=") {
            this.mustNot.add(["range" to [name to ["gte" to value]]])
        }

        if (operator == "<") {
            this.mustNot.add(["range" to [name to ["lt" to value]]])
        }

        if (operator == "<=") {
            this.mustNot.add(["range" to [name to ["lte" to value]]])
        }

        if (operator == "like") {
            this.mustNot.add(["match" to [name to value]])
        }

        if (operator == "exists") {
            this.whereExists(name, !value);
        }

        return this;
    }

    /**
     * Set the query where between clause
     * @param name
     * @param firstValue
     * @param lastValue
     * @return this
     */
    public fun whereBetween(name, firstValue, lastValue = null): Query {
        if (is_mapOf(firstValue) && count(firstValue) == 2) {
            lastValue = firstValue[1];
            firstValue = firstValue[0];
        }

        this.filter.add(["range" to [name to ["gte" to firstValue, "lte" to lastValue]]])
        return this;
    }

    /**
     * Set the query where not between clause
     * @param name
     * @param firstValue
     * @param lastValue
     * @return this
     */
    public fun whereNotBetween(name, firstValue, lastValue = null): Query {

        if (is_mapOf(firstValue) && count(firstValue) == 2) {
            lastValue = firstValue[1];
            firstValue = firstValue[0];
        }

        this.mustNot.add(["range" to [name to ["gte" to firstValue, "lte" to lastValue]]])

        return this;
    }

    /**
     * Set the query where in clause
     * @param       name
     * @param array value
     * @return this
     */
    public fun whereIn(name, value = []): Query {
        this.filter.add(["terms" to [name to value]])
        return this;
    }

    /**
     * Set the query where not in clause
     * @param       name
     * @param array value
     * @return this
     */
    public fun whereNotIn(name, value = []): Query {

        if (is_callback_function(name)) {
            name(this);
            return this;
        }

        this.mustNot.add(["terms" to [name to value]])

        return this;
    }


    /**
     * Set the query where exists clause
     * @param      name
     * @param bool exists
     * @return this
     */
    public fun whereExists(name, exists = true): Query {
        if (exists) {
            this.must.add(["exists" to ["field" to name]])
        } else {
            this.mustNot.add(["exists" to ["field" to name]])
        }

        return this;
    }

    /**
     * Add a condition to find documents which are some distance away from the given geo point.
     *
     * @see https://www.elastic.co/guide/en/elasticsearch/reference/2.4/query-dsl-geo-distance-query.html
     *
     * @param        name
     *   A name of the field.
     * @param mixed value
     *   A starting geo point which can be represented by a string "lat,lon",
     *   an object {"lat": lat, "lon": lon} or an array [lon,lat].
     * @param string distance
     *   A distance from the starting geo point. It can be for example "20km".
     *
     * @return this
     */
    public fun distance(name, value, distance): Query {
        this.filter[] = [
            "geo_distance" to [
                name to value,
                "distance" to distance,
            ]
        ];

        return this;
    }

    /**
     * Search the entire document fields
     * @param null q
     * @return this
     */
    public fun search(q = null, settings = null): Query {
        if (q) {
            search = Search(this, q, settings);
            if (!is_callback_function(settings)) {
                search.boost(settings ? settings : 1);
            }

            search.build();

        }

        return this;
    }

    public fun nested(path, query)
    {
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
    public fun highlight(vararg fields: String): Query {
        val newFields = [];

        for(field in fields) {
            newFields[field] = \stdClass();
        }

        this.body["highlight"] = [
            "fields" to newFields
        ];

        return this;
    }

    /**
     * Generate the query body
     * @return array
     */
    protected fun getBody()
    {

        body = this.body;

        if (count(this.source)) {
            source = array_key_exists("_source", body) ? body["_source"] : [];
            body["_source"] = array_merge(source, this.source);
        }

        body["query"] = isset(body["query"]) ? body["query"]: [];

        if (count(this.must)) {
            body["query"]["bool"]["must"] = this.must;
        }

        if (count(this.mustNot)) {
            body["query"]["bool"]["must_not"] = this.mustNot;
        }

        if (count(this.filter)) {
            body["query"]["bool"]["filter"] = this.filter;
        }

        if(count(body["query"]) == 0){
            unset(body["query"]);
        }

//        body = [
//            "query" to [
//                "nested" to [
//                    "path" to "pages",
//                    "query" to body["query"],
//                    "inner_hits" to [
//                        "highlight" to [
//                            "fields" to [
//                                "pages.content" to (object) []
//                            ]
//                        ]
//                    ]
//                ]
//            ]
//        ];

        if (count(this.sort)) {
            sortFields = array_key_exists("sort", body) ? body["sort"] : [];
            body["sort"] = array_unique(array_merge(sortFields, this.sort), SORT_REGULAR);
        }

        this.body = body;

        return body;
    }

    /**
     * set the query body array
     * @param array body
     * @return this
     */
    fun body(body = [])
    {

        this.body = body;

        return this;
    }

    /**
     * Generate the query to be executed
     * @return array
     */
    public fun query()
    {
        val query = [];

        query["index"] = this.index;

        if (this.type) {
            query["type"] = this.type;
        }

        if (this.model && this.useGlobalScopes) {
            this.model.boot(this);
        }

        query["body"] = this.getBody();

        query["from"] = this.skip;

        query["size"] = this.take;

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
    public fun clear(scrollId: Long = null)
    {

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
    public fun get(scrollId = null)
    {

        scrollId = null;

        result = this.getResult(scrollId);

        return this.getAll(result);
    }

    /**
     * Get the first object of results
     * @param string scrollId
     * @return Model|object
     */
    public fun first(scrollId: Long = null)
    {

        this.take(1);
        val result = this.getResult(scrollId);
        return this.getFirst(result);
    }

    /**
     * Get query result
     * @param scrollId
     * @return mixed
     */
    protected fun getResult(scrollId)
    {

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
    public fun response(scrollId = null)
    {

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
    public fun count()
    {

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

    /**
     * Set the query model
     * @param model
     * @return this
     */
    fun setModel(model)
    {
        this.model = model;
        return this;
    }

    /**
     * Retrieve all records
     * @param array result
     * @return array|Collection
     */
    protected fun getAll(result = [])
    {

        if (array_key_exists("hits", result)) {

            = [];

            for(row in result["hits"]["hits"]) {

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
            new.shards = (object)result["_shards"];

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
    protected fun getFirst(result = [])
    {

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
    public fun paginate(perPage = 10, pageName = "page", page = null)
    {

        // Check if the request from PHP CLI
        if (php_sapi_name() == "cli") {
            this.take(perPage);
            page = page ?: 1;
            this.skip((page * perPage) - perPage);
            objects = this.get();
            return Pagination(objects, objects.total, perPage, page);
        }

        this.take(perPage);

        page = page ?: Request.get(pageName, 1);

        this.skip((page * perPage) - perPage);

        objects = this.get();

        return Pagination(objects, objects.total, perPage, page, ["path" to Request.url(), "query" to Request.query()]);
    }

    /**
     * Insert a document
     * @param      data
     * @param null id
     * @return object
     */
    public fun insert(data, id = null)
    {

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
    public fun bulk(data)
    {

        if (is_callback_function(data)) {

            bulk = Bulk(this);

            data(bulk);

            params = bulk.body();

        } else {

            params = [];

            for((key, value) in data) {

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
    public fun update(data, id = null)
    {
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
    public fun increment(field: String, count: Int = 1)
    {

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
    public fun decrement(field: String, count:Int = 1)
    {

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
    public fun script(script, params = [])
    {

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
    public fun delete(id = null)
    {
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
    public fun withoutGlobalScopes(): Query {
        this.useGlobalScopes = false;
        return this;
    }
}
