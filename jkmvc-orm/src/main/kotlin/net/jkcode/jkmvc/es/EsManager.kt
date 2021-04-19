package net.jkcode.jkmvc.es

import io.searchbox.action.Action
import io.searchbox.client.JestClient
import io.searchbox.client.JestClientFactory
import io.searchbox.client.JestResult
import io.searchbox.client.JestResultHandler
import io.searchbox.client.config.HttpClientConfig
import io.searchbox.core.*
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
import net.jkcode.jkmvc.orm.serialize.toJson
import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.IConfig
import net.jkcode.jkutil.common.esLogger
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.search.aggregations.AggregationBuilders
import java.io.Closeable
import java.sql.SQLException
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * es管理者
 */
object EsManager {

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
    public fun count(index: String, type: String, queryBuilder: ESQueryBuilder): Long {
        val query = queryBuilder.toSearchSource()

        val result: CountResult = tryExecute {
            Count.Builder()
                    .addIndex(index)
                    .addType(type)
                    .query(query)
                    .build()
        }

        return result.count.toLong()
    }

    /**
     * 搜索文档
     * @param index 索引名
     * @param type 类型
     * @param queryBuilder 查询构造
     * @param clazz
     * @return
     */
    fun <T> searchDocs(index: String, type: String, queryBuilder: ESQueryBuilder, clazz: Class<T>): Pair<List<T>, Long> {
        val result: SearchResult = searchDocs(index, type, queryBuilder)
        val list = result.getHits(clazz).map { hit ->
            hit.source
        }
        return list to result.total
    }

    /**
     * 搜索文档
     * @param index 索引名
     * @param type 类型
     * @param queryBuilder 查询构造
     * @return
     */
    public fun searchDocs(index: String, type: String, queryBuilder: ESQueryBuilder): SearchResult {
        val query = queryBuilder.toSearchSource()

        // 执行查询
        val result: SearchResult = tryExecute {
            Search.Builder(query)
                    .addIndex(index)
                    .addType(type)
                    .setSearchType(queryBuilder.searchType)
                    .build()
        }
        if (!result.isSucceeded)
            throw IllegalStateException("Fail to search: " + result.jsonString)

        return result
    }

    /**
     * 开始搜索, 并返回有游标的结果集合
     * @param index
     * @param type
     * @param queryBuilder
     * @param pageSize
     * @param scrollTimeInMillis
     * @param clazz
     */
    fun <T> startScroll(index: String, type: String, queryBuilder: ESQueryBuilder, pageSize: Int, scrollTimeInMillis: Long, clazz: Class<T>): EsScrollCollection<T> {
        val query = queryBuilder.toSearchSource()

        // 执行查询
        val result = tryExecute {
            Search.Builder(query)
                    .addIndex(index)
                    .addType(type)
                    .setParameter(Parameters.SIZE, pageSize)
                    .setParameter(Parameters.SCROLL, scrollTimeInMillis.toString() + "ms")
                    .build()
        }

        // 封装为有游标的结果集合
        return EsScrollCollection(clazz, result, scrollTimeInMillis)
    }

    /**
     * 根据游标获得下一页结果
     */
    private fun continueScroll(scrollId: String, scrollTimeInMillis: Long): ScrollSearchResult {
        val result = tryExecute {
            SearchScroll.Builder(scrollId, scrollTimeInMillis.toString() + "ms").build()
        }

        return ScrollSearchResult(result)
    }

    /**
     * 清理游标
     */
    fun clearScroll(scrollId: String) {
        tryExecute {
            ClearScroll.Builder().addScrollId(scrollId).build()
        }
    }

    /**
     * 有游标的结果集合
     */
    class EsScrollCollection<T>(
            protected var sourceType: Class<T>,
            protected val result: SearchResult,
            protected val scrollTimeInMillis: Long
    ) : AbstractCollection<T>() {

        override val size: Int = result.total.toInt()

        override fun iterator(): MutableIterator<T> {
            return ScrollIterator(result)
        }

        /**
         * 有游标的迭代器
         */
        inner class ScrollIterator(protected var currResult: JestResult) : MutableIterator<T>, Closeable {

            protected var currentHits: Iterator<T>? = currResult.getSourceAsObjectList(sourceType).iterator()

            protected var scrollId: String? = currResult.scrollId

            /** If stream is finished (ie: cluster returns no results.  */
            private var finished = !currentHits!!.hasNext()

            override fun close() {
                try {
                    // Clear scroll on cluster only in case of error (cause elasticsearch auto clear scroll when it's done)
                    if (!finished && scrollId != null && currentHits != null && currentHits!!.hasNext()) {
                        clearScroll(scrollId!!)
                    }
                } finally {
                    currentHits = null
                    scrollId = null
                }
            }

            override operator fun hasNext(): Boolean {
                // Test if stream is finished
                if (finished)
                    return false

                // Test if it remains hits
                if (currentHits == null || !currentHits!!.hasNext()) {
                    // Do a new request
                    currResult = continueScroll(scrollId!!, scrollTimeInMillis, sourceType)
                    // Save hits and scroll id
                    currentHits = currResult.getSourceAsObjectList(sourceType).iterator()
                    finished = !currentHits!!.hasNext()
                    scrollId = currResult.scrollId
                }

                return currentHits!!.hasNext()
            }

            override operator fun next(): T {
                if (hasNext())
                    return currentHits!!.next()

                throw NoSuchElementException()
            }

            override fun remove() {
                throw UnsupportedOperationException("remove")
            }
        }

    }
}