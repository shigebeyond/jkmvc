package net.jkcode.jkmvc.es

import io.searchbox.action.Action
import io.searchbox.client.JestClient
import io.searchbox.client.JestClientFactory
import io.searchbox.client.JestResult
import io.searchbox.client.JestResultHandler
import io.searchbox.client.config.HttpClientConfig
import io.searchbox.core.*
import io.searchbox.core.search.aggregation.MetricAggregation
import io.searchbox.indices.*
import io.searchbox.indices.aliases.AddAliasMapping
import io.searchbox.indices.aliases.GetAliases
import io.searchbox.indices.aliases.ModifyAliases
import io.searchbox.indices.aliases.RemoveAliasMapping
import io.searchbox.indices.mapping.GetMapping
import io.searchbox.indices.mapping.PutMapping
import io.searchbox.indices.settings.GetSettings
import io.searchbox.indices.settings.UpdateSettings
import io.searchbox.params.Parameters
import io.searchbox.params.SearchType
import net.jkcode.jkmvc.orm.serialize.toJson
import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.IConfig
import net.jkcode.jkutil.common.esLogger
import org.apache.commons.collections4.map.HashedMap
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.SortBuilders
import java.sql.SQLException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class EsList<T>(
        val list: List<T> = ArrayList(),
        val scrollId: String? = null,
        val aggregations: MetricAggregation? = null
): List<T> by list

class EsCollection<T>(override val size: Int) : AbstractCollection<T>() {
    override fun iterator(): MutableIterator<T> {
        TODO("Not yet implemented")
    }

}

object ElasticSearchService {

    /**
     * es配置
     */
    public val config: IConfig = Config.instance("es")

    /**
     * es 客户端
     */
    private val client: JestClient by lazy {
        val esUrl: String = config["esUrl"]!!
        val maxTotal: Int? = config["maxTotal"]
        val perTotal: Int? = config["perTotal"]

        val urls = esUrl.split(",")
        val factory = JestClientFactory()
        factory.setHttpClientConfig(HttpClientConfig.Builder(urls)
                .multiThreaded(true)
                .defaultMaxTotalConnectionPerRoute(Integer.valueOf(maxTotal!!)!!)
                .maxTotalConnection(Integer.valueOf(perTotal!!)!!)
                .build())
        factory.getObject()
    }


    /**
     * 对执行es操作包一层try/catch以便打印日志
     */
    private inline fun <T : JestResult> tryExecute(actionBuilder: () -> Action<T>): T {
        val action = actionBuilder.invoke()
        try {
            // 同步执行
            val result = client.execute(action)
            logResult(action, result)
            return result
        } catch (e: SQLException) {
            esLogger.error("Error [{}] on action: {}", e.message, action)
            throw  e
        }
    }

    /**
     * 打印结果
     */
    private fun <T : JestResult> logResult(action: Action<T>, result: T) {
        if (esLogger.isDebugEnabled)
            esLogger.debug("Execute action: {}, result: {}", action, result.jsonString)

        if (!result.isSucceeded)
            esLogger.error("Error [{} -- {}] on action: {}", result.jsonString, result.errorMessage, action)
    }

    /**
     * 对执行es操作包一层try/catch以便打印日志
     */
    private inline fun <T : JestResult> tryExecuteReturnSucceeded(actionBuilder: () -> Action<T>): Boolean {
        val result = tryExecute(actionBuilder)
        return result.isSucceeded
    }

    /**
     * 对执行es操作包一层try/catch以便打印日志
     */
    private inline fun <T : JestResult> tryExecuteAsync(actionBuilder: () -> Action<T>): CompletableFuture<T> {
        val action = actionBuilder.invoke()
        val future = CompletableFuture<T>()
        // 异步执行
        client.executeAsync(action, object : JestResultHandler<T> {
            override fun completed(result: T) {
                logResult(action, result)
                future.complete(result)
            }

            override fun failed(e: Exception) {
                e.printStackTrace()
                esLogger.error("Error [{}] action: {}", e.message, action)
            }
        })
        return future
    }

    /**
     * 新建索引
     *
     * @param index 索引名
     */
    fun createIndex(index: String, nShards: Int = 1, nReplicas: Int = 1): Boolean {
        val settings = mapOf(
                "number_of_shards" to nShards,
                "number_of_replicas" to nReplicas
        )

        return tryExecuteReturnSucceeded {
            CreateIndex.Builder(index).settings(settings).build()
        }
    }

    /**
     * 刷新ｉｎｄｅｘ
     */
    fun refresh(index: String): Boolean {
        return tryExecuteReturnSucceeded {
            Refresh.Builder().addIndex(index).build()
        }
    }


    /**
     * 设置指定type 的 mapping
     *
     * @param index
     * @param type
     * @param mapping
     */
    fun putMapping(index: String, type: String, mapping: String): Boolean {
        return tryExecuteReturnSucceeded {
            PutMapping.Builder(index, type, mapping).build()
        }
    }

    /**
     * 获得指定type 的 mapping
     * @param index
     * @param type
     * @return
     */
    fun getMapping(index: String, type: String): Map<String, Any>? {
        val result = tryExecute {
            GetMapping.Builder().addIndex(index).addType(type).build()
        }

        if (!result.jsonObject.has(index)) {
            esLogger.info("Index {} did not exist when retrieving mappings for type {}.", index, type)
            return null
        }


        val index = result.jsonObject.get(index).asJsonObject
        val mappings = index?.get("mappings")?.asJsonObject
        val type = mappings?.get(type)?.asJsonObject
        if (type == null)
            return null

        return JsonToMap.toMap(type)
    }

    /**
     * 删除索引
     *
     * @param index 索引名
     */
    fun deleteIndex(index: String): Boolean {
        return tryExecuteReturnSucceeded {
            DeleteIndex.Builder(index).build()
        }
    }

    /**
     * 验证索引是否存在
     *
     * @param index 索引名
     */
    fun indexExist(index: String): Boolean {
        return tryExecuteReturnSucceeded {
            IndicesExists.Builder(listOf(index)).build()
        }
    }

    /**
     * 获取对象
     */
    fun <T> getDoc(index: String, type: String, _id: String, clazz: Class<T>): T? {
        val result = tryExecute {
            Get.Builder(index, _id).type(type).build()
        }

        if (result.isSucceeded)
            return result.getSourceAsObject(clazz)

        return null
    }

    /**
     * 获取json数据格式
     */
    fun getDoc(index: String, type: String, _id: String): String? {
        val result = tryExecute {
            Get.Builder(index, _id).type(type).build()
        }

        return result.jsonString
    }


    /**
     * 插入数据
     *
     * @param index 索引名
     * @param type  类型
     * @param source  数据, 可以是 json string/bean/map/list, 如果是bean/map/list, 最好有id属性，要不然会自动生成一个
     * @param _id   数据id
     */
    fun insertDoc(index: String, type: String, source: Any, _id: String? = null): Boolean {
        return tryExecuteReturnSucceeded {
            Index.Builder(source).index(index).type(type).id(_id).build()
        }
    }


    /**
     * 插入数据
     *
     * @param index 索引名
     * @param type  类型
     * @param source  数据, 可以是 json string/bean/map/list, 如果是bean/map/list, 最好有id属性，要不然会自动生成一个
     * @param _id   数据id
     * @return
     */
    fun <T> insertDocAsync(index: String, type: String, source: Any, _id: String? = null): CompletableFuture<DocumentResult> {
        return tryExecuteAsync {
            Index.Builder(source).index(index).type(type).id(_id).build()
        }
    }

    /**
     * 更新数据
     * String script = "{" +
     * "    \"doc\" : {" +
     * "        \"title\" : \""+ entity.getTitle()+"\"," +
     * "    }" +
     * "}";
     */
    fun <T> updateDoc(index: String, type: String, _id: String, script: String): Boolean {
        return tryExecuteReturnSucceeded {
            Update.Builder(script)
                    .id(_id)
                    .index(index)
                    .type(type)
                    .build()
        }
    }

    /**
     * 更新数据
     *
     * @param index 索引名
     * @param type  类型
     * @param _id   数据id
     */
    fun <T> updateDoc(index: String, type: String, _id: String, entity: T): Boolean {
        return tryExecuteReturnSucceeded {
            val script = HashMap<String, Any?>()
            script["doc"] = entity

            Update.Builder(script.toJson()).id(_id)
                    .index(index)
                    .type(type)
                    .build()
        }
    }

    /**
     * 更新数据
     *
     * @param index 索引名
     * @param type  类型
     * @param _id   数据id
     */
    fun <T> updateDocAsync(index: String, type: String, _id: String, entity: T): CompletableFuture<DocumentResult> {
        return tryExecuteAsync {
            val script = HashMap<String, T>()
            script["doc"] = entity

            Update.Builder(script.toJson())
                    .id(_id)
                    .index(index)
                    .type(type)
                    .build()
        }
    }

    /**
     * 删除数据
     *
     * @param index 索引名
     * @param type  类型
     * @param _id   数据id
     */
    fun deleteDoc(index: String, type: String, _id: String): Boolean {
        return tryExecuteReturnSucceeded {
            Delete.Builder(_id)
                    .index(index)
                    .type(type)
                    .build()
        }
    }

    /**
     * 批量插入数据
     * @param index 索引名
     * @param type  类型
     * @param items  (_id主键, 数据), 数据可能是json/bean/map
     */
    fun bulkInsertDocs(index: String, type: String, items: Map<String, Any>) {
        val result = tryExecute {
            val actions = items.map { (_id, item) ->
                Index.Builder(item).id(_id).build()
            }

            Bulk.Builder()
                    .defaultIndex(index)
                    .defaultType(type)
                    .addAction(actions)
                    .build()
        }

        handleBulkResult(result)
    }

    /**
     * 批量插入数据
     *
     * @param index    索引名
     * @param type     类型
     * @param items 批量数据
     */
    fun <T> bulkInsertDocs(index: String, type: String, items: List<T>) {
        val result = tryExecute {
            val actions = items.map { item ->
                Index.Builder(item).build()
            }

            Bulk.Builder()
                    .defaultIndex(index)
                    .defaultType(type)
                    .addAction(actions)
                    .build()
        }
        handleBulkResult(result)
    }

    /**
     * 处理批量结果
     */
    private fun handleBulkResult(result: BulkResult?) {
        val bulkResult = BulkResult(result)
        if (!bulkResult.isSucceeded) {
            val failedDocs: MutableMap<String, String> = HashMap()
            for (item in bulkResult.failedItems) {
                failedDocs[item.id] = item.error
            }
            throw EsException("Bulk indexing has failures. Use EsException.getFailedDocs() for detailed messages [$failedDocs]", failedDocs)
        }
    }

    /**
     * 批量更新数据
     * @param index 索引名
     * @param type  类型
     * @param items  (_id主键, 数据), 数据可能是json/bean/map
     */
    fun bulkUpdateDocs(index: String, type: String, items: Map<String, Any>) {
        val result = tryExecute {
            val actions = items.map { (_id, item) ->
                Update.Builder(item).id(_id).build()
            }

            Bulk.Builder()
                    .defaultIndex(index)
                    .defaultType(type)
                    .addAction(actions)
                    .build()
        }

        handleBulkResult(result)
    }

    /**
     * 获取索引对应的别名
     *
     * @param index
     * @return
     */
    fun getIndexAliases(index: String): Boolean {
        return tryExecuteReturnSucceeded {
            GetAliases.Builder().addIndex(index).build()
        }
    }

    /**
     * 添加索引别名
     *
     * @param index
     * @param alias
     */
    fun addIndexAlias(index: List<String>, alias: String): Boolean {
        return tryExecuteReturnSucceeded {
            val action = AddAliasMapping.Builder(index, alias).build()
            ModifyAliases.Builder(action).build()
        }
    }

    /**
     * 删除索引别名
     *
     * @param index
     * @param alias
     */
    fun removeIndexAlias(index: List<String>, alias: String): Boolean {
        return tryExecuteReturnSucceeded {
            val action = RemoveAliasMapping.Builder(index, alias).build()
            ModifyAliases.Builder(action).build()
        }
    }

    /**
     * 获得index配置
     * @param index
     * @return
     */
    fun getIndexSetting(index: String): Map<String, Any> {
        val result = tryExecute {
            GetSettings.Builder()
                    .addIndex(index).build()
        }

        val setting = result.jsonObject
                .get(index).asJsonObject
                .get("settings").asJsonObject
                .get("index").asJsonObject

        return JsonToMap.toMap(setting)
    }

    /**
     * 更改索引index设置setting
     *
     * @param index
     * @return
     */
    fun updateIndexSettings(index: String): Boolean {
        return tryExecuteReturnSucceeded {
            val mapBuilder = XContentFactory.jsonBuilder()
            mapBuilder!!.startObject().startObject("index")
                    .field("max_result_window", "1000000")
                    .endObject()
                    .endObject()
            val source = mapBuilder.string()
            UpdateSettings.Builder(source).build()
        }
    }

    /**
     * 索引优化
     */
    fun optimizeIndex(): CompletableFuture<JestResult> {
        return tryExecuteAsync {
            Optimize.Builder().build()
        }
    }

    /**
     * 清理缓存
     */
    fun clearCache(): Boolean {
        return tryExecuteReturnSucceeded {
            ClearCache.Builder().build()
        }
    }

    /**
     * 统计行数
     */
    public fun count(index: String, type: String, query: QueryBuilder?): Long {
        val result: CountResult = tryExecute {
            val count = Count.Builder()
                    .addIndex(index)
                    .addType(type)

            if (query != null) {
                count.query(SearchSourceBuilder().query(query).toString())
            }
            count.build()
        }

        return result.count.toLong()
    }


    /**
     * 查询
     *
     * @param index       索引名
     * @param type        类型
     * @param constructor 查询构造
     */
    fun <T> search(index: String, type: String, clazz: Class<T>, constructor: ESQueryBuilderConstructor): Page<T>? {
        val result: SearchResult = doSearch(index, type, constructor)

        if (result.isSucceeded) {
            result.getHits(clazz).map { item ->
                item.source
            }
            page = Page()
            page.setList(list).setCount(result.total)
        } else {
            esLogger.error("index search result: {}", result.jsonString)
        }

        return page
    }

    private fun doSearch(index: String, type: String, constructor: ESQueryBuilderConstructor): SearchResult {
        val query = buildSearchQuery(constructor)

        // 执行查询
        return tryExecute {
            Search.Builder(query)
                    .addIndex(index)
                    .addType(type)
                    .setSearchType(constructor.searchType)
                    .build()
        }
    }

    private fun buildSearchQuery(constructor: ESQueryBuilderConstructor): String {
        val sourceBuilder = SearchSourceBuilder()

        // 前置过滤
        sourceBuilder.query(constructor.build())

        // 分页
        sourceBuilder.from(constructor.offset)
        sourceBuilder.size(constructor.limit)

        // 后置过滤
        if (constructor.postFilter != null)
            sourceBuilder.postFilter(constructor.postFilter)

        sourceBuilder.timeout(TimeValue(60, TimeUnit.SECONDS))

        //增加多个值排序
        for ((field, order) in constructor.sorts) {
            val sort = SortBuilders.fieldSort(field).order(order)
            sourceBuilder.sort(sort)
        }

        //属性
        if (constructor.includeFields.isNotEmpty() || constructor.excludeFields.isNotEmpty()) {
            sourceBuilder.fetchSource(constructor.includeFields.toTypedArray(), constructor.excludeFields.toTypedArray())
        }

        // 分数
        if (constructor.minScore > 0)
            sourceBuilder.minScore(constructor.minScore)

        // 聚合
        for (aggregationBuilder in constructor.aggregations) {
            sourceBuilder.aggregation(aggregationBuilder)
        }

        // 高亮字段
        val highlighter = SearchSourceBuilder.highlight()
        for(f in constructor.highlightFields){
            highlighter.field(f)
        }
        sourceBuilder.highlighter(highlighter)

        // 转查询字符串
        val query = sourceBuilder.toString()
        esLogger.debug("查询条件:{}", query)
        return query
    }


    /**
     * 单个域值的聚合
     * @param index       索引名
     * @param type        类型
     * @param constructor 查询构造
     * @param groupBy     统计分组字段
     */
    fun statSearch(index: String, type: String, constructor: ESQueryBuilderConstructor, groupBy: String): Map<String, Any> {
        val map = HashedMap()

        val result = stat(index, type, constructor, AggregationBuilders.terms("agg").field(groupBy))
        esLogger.debug("result:{}", result.jsonString)

        if (result.isSucceeded) {
            result.aggregations.getTermsAggregation("agg").buckets.forEach { item ->
                map.put(item.key, item.count)
            }
        } else {
            esLogger.error("error, result: {}", result.jsonString)
        }
        return map
    }


    open fun <T> doStream(scrollTimeInMillis: Long, page: ScrolledPage<T>, clazz: Class<T>, mapper: JestResultsMapper): CloseableIterator<T>? {
        return object : CloseableIterator<T>() {
            /** As we couldn't retrieve single result with scroll, store current hits.  */
            @Volatile
            private var currentHits: Iterator<T>? = page.iterator()

            /** The scroll id.  */
            @Volatile
            private var scrollId: String? = page.getScrollId()

            /** If stream is finished (ie: cluster returns no results.  */
            @Volatile
            private var finished = !currentHits!!.hasNext()

            fun close() {
                try {
                    // Clear scroll on cluster only in case of error (cause elasticsearch auto clear scroll when it's done)
                    if (!finished && scrollId != null && currentHits != null && currentHits!!.hasNext()) {
                        clearScroll(scrollId)
                    }
                } finally {
                    currentHits = null
                    scrollId = null
                }
            }

            operator fun hasNext(): Boolean {
                // Test if stream is finished
                if (finished) {
                    return false
                }
                // Test if it remains hits
                if (currentHits == null || !currentHits!!.hasNext()) {
                    // Do a new request
                    val scroll: ScrolledPage<T> = continueScroll(scrollId, scrollTimeInMillis, clazz, mapper) as ScrolledPage<T>
                    // Save hits and scroll id
                    currentHits = scroll.iterator()
                    finished = !currentHits!!.hasNext()
                    scrollId = scroll.getScrollId()
                }
                return currentHits!!.hasNext()
            }

            operator fun next(): T {
                if (hasNext())
                    return currentHits!!.next()

                throw NoSuchElementException()
            }

            fun remove() {
                throw UnsupportedOperationException("remove")
            }
        }
    }

    private fun doScroll(index: String, type: String, constructor: ESQueryBuilderConstructor, pageSize: Int, scrollTimeInMillis: Long): SearchScrollResult {
        val query = buildSearchQuery(constructor)

        // 执行查询
        val result = tryExecute {
            Search.Builder(query)
                    .addIndex(index)
                    .addType(type)
                    .setParameter(Parameters.SIZE, pageSize)
                    .setParameter(Parameters.SCROLL, scrollTimeInMillis.toString() + "ms")
                    .build()
        }
        return SearchScrollResult(result)
    }

    fun <T> startScroll(index: String, type: String, constructor: ESQueryBuilderConstructor, pageSize: Int, scrollTimeInMillis: Long, clazz: Class<T>): Page<T>? {
        val response = doScroll(index, type, constructor, pageSize, scrollTimeInMillis)
        return resultsMapper.mapResults(response, clazz, null)
    }


    fun <T> continueScroll(scrollId: String, scrollTimeInMillis: Long, clazz: Class<T>?): Page<T>? {
        val result = tryExecute {
            SearchScroll.Builder(scrollId, scrollTimeInMillis.toString() + "ms").build()
        }

        val result2 = SearchScrollResult(result)

        return resultsMapper.mapResults(response, clazz)
    }

    fun clearScroll(scrollId: String) {
        tryExecute {
            ClearScroll.Builder().addScrollId(scrollId).build()
        }
    }

}