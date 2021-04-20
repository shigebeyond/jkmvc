package net.jkcode.jkmvc.es

import com.google.gson.JsonObject
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
import net.jkcode.jkutil.common.*
import org.elasticsearch.common.xcontent.XContentFactory
import java.io.Closeable
import java.sql.SQLException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * es管理者
 *    JestClient实现JestHttpClient, 使用的是apache-httpComponents中的HttpClient, 配置`HttpClientConfig.multiThreaded(true)`支持多线程, 那么连接管理使用的是PoolingHttpClientConnectionManager, 是线程安全的连接池, 发送前requestConnection()调用leaseConnection()获得/复用连接池中的连接, 发送后releaseConnection()归还连接到池里, 获得与归还连接会加锁, 但他是线程安全的
 *    JestHttpClient单例+线程安全, spring jest template也是这么实现
 *
 * @author shijianhang
 * @date 2021-4-21 下午5:16:59
 */
class EsManager protected constructor(protected val client: JestClient) {

    companion object {

        /**
         * EsManager池
         */
        private val insts: ConcurrentHashMap<String, EsManager> = ConcurrentHashMap();

        /**
         * 获得EsManager实例
         */
        @JvmOverloads
        @JvmStatic
        public fun instance(name: String = "default"): EsManager {
            return insts.getOrPutOnce(name) {
                EsManager(buildClient(name))
            }
        }

        /**
         * 创建es client
         */
        private fun buildClient(name: String): JestClient {
            // es配置
            val config: IConfig = Config.instance("es.$name", "yaml")
            val esUrl: String = config["esUrl"]!!
            val maxTotal: Int? = config["maxTotal"]
            val perTotal: Int? = config["perTotal"]

            val urls = esUrl.split(",")

            // es client工厂
            val factory = JestClientFactory()
            val hcConfig = HttpClientConfig.Builder(urls)
                    .multiThreaded(true)
                    .defaultMaxTotalConnectionPerRoute(Integer.valueOf(maxTotal!!)!!)
                    .maxTotalConnection(Integer.valueOf(perTotal!!)!!)
                    .build()
            factory.setHttpClientConfig(hcConfig)

            // 创建es client
            return factory.getObject()
        }
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
     * @param nShards 分区数
     * @param nReplicas 复制数
     * @param readonly 是否只读
     */
    fun createIndex(index: String, nShards: Int = 1, nReplicas: Int = 1, readonly: Boolean = false): Boolean {
        val settings = mapOf(
                "number_of_shards" to nShards,
                "number_of_replicas" to nReplicas,
                "blocks.read_only_allow_delete" to readonly
        )

        return createIndex(index, settings)
    }

    /**
     * 新建索引
     *
     * @param index 索引名
     * @param settings 配置
     */
    fun createIndex(index: String, settings: Map<String, *>): Boolean {
        return tryExecuteReturnSucceeded {
            CreateIndex.Builder(index).settings(settings).build()
        }
    }

    /**
     * 删除索引
     *
     * @param index 索引名
     * @return
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
     * @return
     */
    fun indexExist(index: String): Boolean {
        return tryExecuteReturnSucceeded {
            IndicesExists.Builder(listOf(index)).build()
        }
    }

    /**
     * 刷新index
     * @param index
     * @return
     */
    fun refreshIndex(index: String): Boolean {
        return tryExecuteReturnSucceeded {
            Refresh.Builder().addIndex(index).build()
        }
    }

    /**
     * 获得index配置
     * @param index
     * @return
     */
    fun getSetting(index: String): Map<String, Any?> {
        val result = tryExecute {
            GetSettings.Builder()
                    .addIndex(index)
                    .build()
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
    fun updateSettings(index: String): Boolean {
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
     * 设置指定type 的 mapping
     *
     * @param index
     * @param type
     * @param mapping
     * @return
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
    fun getMapping(index: String, type: String): Map<String, Any?>? {
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
     * 获取对象
     * @param index
     * @param type
     * @param _id
     * @param clazz
     * @return
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
     * 获取对象
     * @param index
     * @param type
     * @param ids
     * @param clazz
     * @return
     */
    fun <T> multGetDocs(index: String, type: String, ids: List<String>, clazz: Class<T>): List<T> {
        val result = tryExecute {
            MultiGet.Builder.ById(index, type).addId(ids).build()
        }

        if (result.isSucceeded)
            return result.getSourceAsObjectList(clazz)

        return emptyList()
    }

    /**
     * 获取json格式的文档
     * @param index
     * @param type
     * @param _id
     * @return
     */
    fun getDoc(index: String, type: String, _id: String): String? {
        val result = tryExecute {
            Get.Builder(index, _id).type(type).build()
        }

        return result.jsonString
    }


    /**
     * 插入文档
     *
     * @param index 索引名
     * @param type  类型
     * @param source  文档, 可以是 json string/bean/map/list, 如果是bean/map/list, 最好有id属性，要不然会自动生成一个
     * @param _id   文档id
     */
    fun insertDoc(index: String, type: String, source: Any, _id: String? = null): Boolean {
        return tryExecuteReturnSucceeded {
            Index.Builder(source).index(index).type(type).id(_id).build()
        }
    }

    /**
     * 插入文档
     *
     * @param index 索引名
     * @param type  类型
     * @param source  文档, 可以是 json string/bean/map/list, 如果是bean/map/list, 最好有id属性，要不然会自动生成一个
     * @param _id   文档id
     * @return
     */
    fun <T> insertDocAsync(index: String, type: String, source: Any, _id: String? = null): CompletableFuture<DocumentResult> {
        return tryExecuteAsync {
            Index.Builder(source).index(index).type(type).id(_id).build()
        }
    }

    /**
     * 更新文档
     * String script = "{" +
     * "    \"doc\" : {" +
     * "        \"title\" : \""+ entity.getTitle()+"\"," +
     * "    }" +
     * "}";
     *
     * @param index 索引名
     * @param type  类型
     * @param _id   文档id
     * @param script 更新脚本
     * @return
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
     * 更新文档
     *
     * @param index 索引名
     * @param type  类型
     * @param _id   文档id
     * @param entity 文档
     * @return
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
     * 异步更新文档
     *
     * @param index 索引名
     * @param type  类型
     * @param _id   文档id
     * @param entity 文档
     * @return
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
     * 删除文档
     *
     * @param index 索引名
     * @param type  类型
     * @param _id   文档id
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
     * 删除文档
     *
     * @param index 索引名
     * @param type  类型
     * @param queryBuilder
     * @param idField id字段
     * @param pageSize
     * @param scrollTimeInMillis
     * @return 被删除的id
     */
    fun deleteDocs(index: String, type: String, queryBuilder: ESQueryBuilder, pageSize: Int = 1000, scrollTimeInMillis: Long = 3000): Collection<String> {
        val ids = scrollDocs(index, type, queryBuilder, pageSize, scrollTimeInMillis){ result ->
            when(result) {
                is SearchResult ->
                    // result.getHits(JsonObject::class.java).map { it.id }
                    decorateCollection(result.getHits(JsonObject::class.java)) { it.id }
                is ScrollSearchResult ->
                    decorateCollection(result.getHits(JsonObject::class.java)) { it.id }
                else ->
                    throw IllegalStateException()
            }
        }

        bulkDeleteDocs(index, type, ids)
        return ids
    }

    /**
     * 批量插入文档
     * @param index 索引名
     * @param type  类型
     * @param items  (_id主键, 文档), 文档可能是json/bean/map
     */
    fun bulkInsertDocs(index: String, type: String, items: Map<String, Any>) {
        if(items.isEmpty())
            return

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
     * 批量插入文档
     *
     * @param index    索引名
     * @param type     类型
     * @param items 批量文档
     */
    fun <T> bulkInsertDocs(index: String, type: String, items: List<T>) {
        if(items.isEmpty())
            return

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
     * 批量更新文档
     * @param index 索引名
     * @param type  类型
     * @param items  (_id主键, 文档), 文档可能是json/bean/map
     */
    fun bulkUpdateDocs(index: String, type: String, items: Map<String, Any>) {
        if(items.isEmpty())
            return

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
     * 批量删除文档
     * @param index 索引名
     * @param type  类型
     * @param ids
     */
    fun bulkDeleteDocs(index: String, type: String, ids: Collection<String>) {
        if(ids.isEmpty())
            return

        val result = tryExecute {
            val actions = ids.map { id ->
                Delete.Builder(id).build()
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
     * 创建查询构建器
     * @return
     */
    public fun queryBuilder(): ESQueryBuilder{
        return ESQueryBuilder(this)
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
     * 搜索文档, 并返回有游标的结果集合
     * @param index
     * @param type
     * @param queryBuilder
     * @param pageSize
     * @param scrollTimeInMillis
     * @param resultMapper 结果转换器
     * @return
     */
    fun <T> scrollDocs(index: String, type: String, queryBuilder: ESQueryBuilder, pageSize: Int = 1000, scrollTimeInMillis: Long = 3000, resultMapper:(JestResult)->Collection<T>): EsScrollCollection<T> {
        val result = startScroll(index, type, queryBuilder, pageSize, scrollTimeInMillis)

        // 封装为有游标的结果集合, 在迭代中查询下一页
        return EsScrollCollection(result, scrollTimeInMillis, resultMapper)
    }

    /**
     * 搜索文档, 并返回有游标的结果集合
     * @param index
     * @param type
     * @param queryBuilder
     * @param clazz bean类, 可以是HashMap, 但字段类型不可控, 如long主键值居然被查出为double
     * @param pageSize
     * @param scrollTimeInMillis
     * @return
     */
    fun <T> scrollDocs(index: String, type: String, queryBuilder: ESQueryBuilder, clazz: Class<T>, pageSize: Int = 1000, scrollTimeInMillis: Long = 3000): EsScrollCollection<T> {
        return scrollDocs(index, type, queryBuilder, pageSize, scrollTimeInMillis) { result ->
            result.getSourceAsObjectList(clazz)
        }
    }

    /**
     * 开始搜索文档, 并返回有游标的结果集合
     * @param index
     * @param type
     * @param queryBuilder
     * @param pageSize
     * @param scrollTimeInMillis
     * @return
     */
    private fun startScroll(index: String, type: String, queryBuilder: ESQueryBuilder, pageSize: Int, scrollTimeInMillis: Long): SearchResult {
        val query = queryBuilder.toSearchSource()

        // 执行查询
        return tryExecute {
            Search.Builder(query)
                    .addIndex(index)
                    .addType(type)
                    .setParameter(Parameters.SIZE, pageSize)
                    .setParameter(Parameters.SCROLL, scrollTimeInMillis.toString() + "ms")
                    .build()
        }
    }

    /**
     * 根据游标获得下一页结果
     */
    private fun continueScroll(scrollId: String, scrollTimeInMillis: Long = 3000): ScrollSearchResult {
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
     * 有游标的结果集合, 在迭代中查询下一页
     */
    inner class EsScrollCollection<T>(
            protected val result: SearchResult,
            protected val scrollTimeInMillis: Long,
            protected val resultMapper:(JestResult)->Collection<T> // 结果转换器
    ) : AbstractCollection<T>() {

        override val size: Int = result.total.toInt()

        override fun iterator(): MutableIterator<T> {
            return ScrollIterator(result)
        }

        /**
         * 有游标的迭代器
         */
        inner class ScrollIterator(protected var currResult: JestResult) : MutableIterator<T>, Closeable {

            // 当前结果hit, 每天切换结果会改变
            protected var currHits: Iterator<T>? = null

            // 游标id, 每天切换结果会改变
            protected var scrollId: String? = null

            // 迭代完成, 每天切换结果会改变
            private var finished = true

            init {
                onToggleResult()
            }

            /**
             * 切换下一页的结果的处理
             */
            protected fun onToggleResult() {
                currHits = resultMapper(currResult!!).iterator()
                finished = !currHits!!.hasNext()
                scrollId = currResult.scrollId
                if (finished)
                    close()
            }

            override fun close() {
                try {
                    // 虽然es 会有自动清理机制，但是 scroll_id 的存在会耗费大量的资源来保存一份当前查询结果集映像，并且会占用文件描述符。所以用完之后要及时清理
                    if (scrollId != null)
                        clearScroll(scrollId!!)
                } finally {
                    currHits = null
                    scrollId = null
                }
            }

            override operator fun hasNext(): Boolean {
                // 检查是否结束
                if (finished)
                    return false

                // 当前页迭代完, 就查询下一页
                if (currHits != null && !currHits!!.hasNext()) {
                    // 查询下一页
                    currResult = continueScroll(scrollId!!, scrollTimeInMillis)
                    // Save hits and scroll id
                    onToggleResult()
                }

                // 迭代
                return currHits?.hasNext() ?: false
            }

            override operator fun next(): T {
                if (hasNext())
                    return currHits!!.next()

                throw NoSuchElementException()
            }

            override fun remove() {
                throw UnsupportedOperationException("remove")
            }
        }

    }
}