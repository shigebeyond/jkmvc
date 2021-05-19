package net.jkcode.jkmvc.es

import com.google.gson.*
import com.google.gson.internal.bind.MapTypeAdapterFactory
import com.google.gson.reflect.TypeToken
import io.searchbox.action.Action
import io.searchbox.client.JestClientFactory
import io.searchbox.client.JestResult
import io.searchbox.client.JestResultHandler
import io.searchbox.client.config.HttpClientConfig
import io.searchbox.client.http.JestHttpClient
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
import net.jkcode.jkmvc.orm.OrmEntity
import net.jkcode.jkutil.common.*
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType
import java.io.Closeable
import java.sql.SQLException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * es管理者
 *    JestClient实现JestHttpClient, 使用的是apache-httpComponents中的HttpClient, 配置`HttpClientConfig.multiThreaded(true)`支持多线程, 那么连接管理使用的是PoolingHttpClientConnectionManager, 是线程安全的连接池, 发送前requestConnection()调用leaseConnection()获得/复用连接池中的连接, 发送后releaseConnection()归还连接到池里, 获得与归还连接会加锁, 但他是线程安全的
 *    JestHttpClient单例+线程安全, spring jest template也是这么实现
 *
 * @author shijianhang
 * @date 2021-4-21 下午5:16:59
 */
class EsManager protected constructor(protected val client: JestHttpClient) {

    companion object {

        /**
         * gson
         */
        public val gson: Gson by lazy {
            val builder = GsonBuilder()
                    .setDateFormat("yyyy-MM-dd HH:mm:ss")
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)

            val gson = builder.create()

            val field = Gson::class.java.getAccessibleField("factories")!!
            val factories: List<TypeAdapterFactory> = field.get(gson) as List<TypeAdapterFactory>
            // 获得map类型的适配器
            val mapTypeAdapterFactory = factories.first {
                it is MapTypeAdapterFactory
            }
            val mapTypeAdapter = mapTypeAdapterFactory.create(gson, TypeToken.get(HashMap::class.java))
            // 创建entity类型的适配器工厂
            val entityFactory = EntityTypeAdapterFactory(mapTypeAdapter)

            // 改写 gson.factories: 注意entityFactory优先
            val factories2 = ArrayList<TypeAdapterFactory>()
            factories2.add(entityFactory) // entityFactory 放前面, 防止被ReflectiveTypeAdapterFactory拦截
            factories2.addAll(factories)
            field.set(gson, factories2)

            gson
        }

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
        private fun buildClient(name: String): JestHttpClient {
            // es配置
            val config: IConfig = Config.instance("es.$name", "yaml")
            val esUrl: String = config["esUrl"]!!
            val maxTotal: Int = config["maxTotal"]!!
            val maxTotalPerRoute: Int = config["maxTotalPerRoute"]!!

            val urls = esUrl.split(",")

            // es client工厂
            val factory = JestClientFactory()
            val hcConfig = HttpClientConfig.Builder(urls)
                    .multiThreaded(true)
                    .defaultMaxTotalConnectionPerRoute(maxTotalPerRoute)
                    .maxTotalConnection(maxTotal)
                    .gson(gson)
                    .build()
            factory.setHttpClientConfig(hcConfig)

            // 创建es client
            return factory.getObject() as JestHttpClient
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
    @JvmOverloads
    fun createIndex(index: String, nShards: Int, nReplicas: Int, readonly: Boolean = false): Boolean {
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
    @JvmOverloads
    fun createIndex(index: String, settings: Map<String, *>? = null): Boolean {
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
    fun getSetting(index: String): JsonObject? {
        val result = tryExecute {
            GetSettings.Builder().addIndex(index).build()
        }

        val indexObj = result.jsonObject.get(index)
        if(indexObj == null)
            throw EsException("No setting for index[$index]")

        val setting = indexObj.asJsonObject
                .get("settings").asJsonObject
                .get("index").asJsonObject

        return setting
        //return JsonToMap.toMap(setting)
    }

    /**
     * 更改索引index设置setting
     *
     * @param index
     * @param setting
     * @return
     */
    fun updateSettings(index: String, setting: Map<String, Any?>): Boolean {
        return tryExecuteReturnSucceeded {
            UpdateSettings.Builder(setting).addIndex(index).build()
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
            PutMapping.Builder(index, type, mapping)
                    //.setParameter("include_type_name", true) // es6不用, es7要
                    .build()
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

        return gson.fromJson(type, Map::class.java) as Map<String, Any?>?
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
     * 获取索引对应的别名
     *
     * @param index
     * @return
     */
    fun getIndexAliases(index: String): JestResult? {
        val result = tryExecute {
            GetAliases.Builder().addIndex(index).build()
        }

        return result
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
    fun <T> multGetDocs(index: String, type: String, ids: Collection<String>, clazz: Class<T>): List<T> {
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
     * 索引文档(插入或更新)
     *
     * @param index 索引名
     * @param type  类型
     * @param source  文档, 可以是 json string/bean/map/list, 如果是bean/map/list, 最好有id属性，要不然会自动生成一个
     * @param _id   文档id
     */
    @JvmOverloads
    fun indexDoc(index: String, type: String, source: Any, _id: String? = null): Boolean {
        return tryExecuteReturnSucceeded {
            buildInsertAction(index, type, source, _id)
        }
    }

    /**
     * 索引文档(插入或更新)
     *
     * @param index 索引名
     * @param type  类型
     * @param source  文档, 可以是 json string/bean/map/list, 如果是bean/map/list, 最好有id属性，要不然会自动生成一个
     * @param _id   文档id
     * @return
     */
    @JvmOverloads
    fun <T> indexDocAsync(index: String, type: String, source: Any, _id: String? = null): CompletableFuture<DocumentResult> {
        return tryExecuteAsync {
            buildInsertAction(index, type, source, _id)
        }
    }

    /**
     * 构建插入的动作
     * @param index 索引名
     * @param type  类型
     * @param source 文档, 可以是 json string/bean/map/list, 如果是bean/map/list, 最好有id属性，要不然会自动生成一个
     * @param _id   文档id
     * @return
     */
    protected fun buildInsertAction(index: String, type: String, source: Any, _id: String? = null): Index {
        val action = Index.Builder(source).index(index).type(type)
        var id = _id
        // 获得_id: 用 @EsId 注解的属性值
        if(id == null && source is OrmEntity){
            val rep = EsDocRepository.instance(source.javaClass)
            id = rep.getId(source)
        }
        if(id != null)
            action.id(id)
        return action.build()
    }

    /**
     * 更新文档
     *   支持增量更新, 如添加新字段, 如动态计算的字段值
     *
     * @param index 索引名
     * @param type  类型
     * @param script 更新脚本
     * 属性不用计算
     * {
     *     "doc" : {"title" : "hello"}
     * }
     * 属性要计算
     * {
     *     "script" : "ctx._source.tags += tag",
     *     "params" : { "tag" : "blue"}
     * }
     * @param _id   文档id
     * @param params 脚本参数
     * @return
     */
    @JvmOverloads
    fun <T> updateDoc(index: String, type: String, script: String, _id: String, params: Map<String, Any?> = emptyMap()): Boolean {
        val script2 = Script(ScriptType.INLINE, "painless", script, params)
        return tryExecuteReturnSucceeded {
            buildUpdateAction(index, type, script2, _id)
        }
    }

    /**
     * 更新文档
     *    支持增量更新, 如添加新字段, 如动态计算的字段值
     *
     * @param index 索引名
     * @param type  类型
     * @param entity 文档
     * @param _id   文档id
     * @return
     */
    fun <T: Any> updateDoc(index: String, type: String, entity: T, _id: String): Boolean {
        return tryExecuteReturnSucceeded {
            buildUpdateAction(index, type, entity, _id)
        }
    }

    /**
     * 构建更新动作
     *
     * @param index 索引名
     * @param type  类型
     * @param script 更新脚本
     * 属性不用计算
     * {
     *     "doc" : {"title" : "hello"}
     * }
     * 属性要计算
     * {
     *     "script" : "ctx._source.tags += tag",
     *     "params" : { "tag" : "blue"}
     * }
     * @param _id   文档id
     * @param params 脚本参数
     * @return
     */
    protected fun buildUpdateAction(index: String, type: String, script: Any, _id: String): Update {
        var script2: Any
        if(script is String || script is Script){ // 纯脚本
            script2 = script
        }else{ // 纯文档
            val map = mapOf("doc" to script)
            script2 = gson.toJson(map)
        }

        return Update.Builder(script2)
                .id(_id)
                .index(index)
                .type(type)
                .build()
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
    fun <T: Any> updateDocAsync(index: String, type: String, _id: String, entity: T): CompletableFuture<DocumentResult> {
        return tryExecuteAsync {
            buildUpdateAction(index, type, entity, _id)
        }
    }

    /**
     * 通过查询来批量更新
     *    参考 https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update-by-query.html
     *
     * @param index 索引名
     * @param type  类型
     * @param script
     * @param queryBuilder
     * @param pageSize
     * @param scrollTimeInMillis
     */
    @JvmOverloads
    fun updateDocsByQuery(index: String, type: String, script: String, queryBuilder: EsQueryBuilder, pageSize: Int = 1000, scrollTimeInMillis: Long = 3000): UpdateByQueryResult {
        val xContentBuilder: XContentBuilder = XContentFactory.jsonBuilder()
                .startObject()
                .field("query", queryBuilder.toQuery())
                .startObject("script")
                .field("inline", script)
                .endObject()
                .endObject()

        xContentBuilder.flush()

        //val payload = (xContentBuilder.getOutputStream() as ByteArrayOutputStream).toString("UTF-8")
        val payload = xContentBuilder.bytes().utf8ToString()

        return tryExecute {
            UpdateByQuery.Builder(payload)
                    .addIndex(index)
                    .addType(type)
                    .setParameter("scroll_size", pageSize)
                    .setParameter("scroll", scrollTimeInMillis.toString() + "ms")
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
     * 通过查询来批量删除文档
     *   参考 https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete-by-query.html
     *
     * @param index 索引名
     * @param type  类型
     * @param queryBuilder
     * @param idField id字段
     * @param pageSize
     * @param scrollTimeInMillis
     * @return 被删除的id
     */
    @JvmOverloads
    fun deleteDocsByQuery(index: String, type: String, queryBuilder: EsQueryBuilder, pageSize: Int = 1000, scrollTimeInMillis: Long = 3000): JestResult? {
        val query = queryBuilder.toQuery().toString()
        return tryExecute {
            DeleteByQuery.Builder(query)
                    .addIndex(index)
                    .addType(type)
                    .setParameter("scroll_size", pageSize)
                    .setParameter("scroll", scrollTimeInMillis.toString() + "ms")
                    .build()
        }
    }

    /**
     * 通过查询来批量删除文档
     *
     * @param index 索引名
     * @param type  类型
     * @param queryBuilder
     * @param idField id字段
     * @param pageSize
     * @param scrollTimeInMillis
     * @return 被删除的id
     */
    @JvmOverloads
    fun deleteDocsByQuery2(index: String, type: String, queryBuilder: EsQueryBuilder, pageSize: Int = 1000, scrollTimeInMillis: Long = 3000): Collection<String> {
        // 查id
        // 原来想先 queryBuilder.select("id"), 可惜不知道id字段是啥
        val ids = scrollDocs(index, type, queryBuilder, pageSize, scrollTimeInMillis){ result ->
            // 处理每一页的JestResult(兼容SearchResult/ScrollSearchResult)
            when(result) {
                is SearchResult -> // 第一页
                    result.getHits(JsonObject::class.java).map { it.id }
                is ScrollSearchResult -> // 其他下一页
                    result.getHits(JsonObject::class.java).map { it.id }
                else ->
                    throw IllegalStateException("Unkown scroll result type: " + result.javaClass.name)
            }
        }.toList() // 复制为list, 因为 scrollDocs() 的结果 EsScrollCollection 只能迭代一次

        bulkDeleteDocs(index, type, ids)
        return ids
    }

    /**
     * 批量索引文档(插入或更新)
     * @param index 索引名
     * @param type  类型
     * @param items  (_id主键, 文档), 文档可能是json/bean/map
     */
    fun bulkIndexDocs(index: String, type: String, items: Map<String, Any>) {
        if(items.isEmpty())
            return

        val result = tryExecute {
            val actions = items.map { (_id, item) ->
                buildInsertAction(index, type, item, _id)
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
     * 批量索引文档(插入或更新)
     *
     * @param index    索引名
     * @param type     类型
     * @param items 批量文档
     */
    fun <T: Any> bulkIndexDocs(index: String, type: String, items: List<T>) {
        if(items.isEmpty())
            return

        val result = tryExecute {
            val actions = items.map { item ->
                buildInsertAction(index, type, item)
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
     * 创建查询构建器
     * @return
     */
    public fun queryBuilder(): EsQueryBuilder{
        return EsQueryBuilder(this)
    }

    /**
     * 统计行数
     */
    public fun count(index: String, type: String, queryBuilder: EsQueryBuilder): Long {
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
     * 搜索单个文档
     * @param index 索引名
     * @param type 类型
     * @param queryBuilder 查询构造
     * @param clazz
     * @return
     */
    fun <T> searchDoc(index: String, type: String, queryBuilder: EsQueryBuilder, clazz: Class<T>): T? {
        queryBuilder.limit(1)
        val result: SearchResult = searchDocs(index, type, queryBuilder)
        return result.getFirstHit(clazz)?.source
    }

    /**
     * 搜索多个文档
     * @param index 索引名
     * @param type 类型
     * @param queryBuilder 查询构造
     * @param clazz
     * @return
     */
    fun <T> searchDocs(index: String, type: String, queryBuilder: EsQueryBuilder, clazz: Class<T>): Pair<List<T>, Long> {
        val result: SearchResult = searchDocs(index, type, queryBuilder)
        val list = result.getHits(clazz).map { hit ->
            hit.source
        }
        return list to result.total
    }

    /**
     * 搜索多个文档
     * @param index 索引名
     * @param type 类型
     * @param queryBuilder 查询构造
     * @return
     */
    public fun searchDocs(index: String, type: String, queryBuilder: EsQueryBuilder): SearchResult {
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
     * @param queryBuilder 查询对象, 分页无效
     * @param pageSize
     * @param scrollTimeInMillis 游标的有效时间, 如果报错`Elasticsearch No search context found for id`, 则加大
     * @param resultMapper 结果转换器, 会将每一页的JestResult(兼容SearchResult/ScrollSearchResult), 转为T对象集合
     * @return
     */
    @JvmOverloads
    fun <T> scrollDocs(index: String, type: String, queryBuilder: EsQueryBuilder, pageSize: Int = 1000, scrollTimeInMillis: Long = 3000, resultMapper:(JestResult)->Collection<T>): EsScrollCollection<T> {
        val result = startScroll(index, type, queryBuilder, pageSize, scrollTimeInMillis)

        // 封装为有游标的结果集合, 在迭代中查询下一页, 即调用continueScroll()
        return EsScrollCollection(result, scrollTimeInMillis, resultMapper)
    }

    /**
     * 搜索文档, 并返回有游标的结果集合
     * @param index
     * @param type
     * @param queryBuilder 查询对象, 分页无效
     * @param clazz bean类, 可以是HashMap, 但字段类型不可控, 如long主键值居然被查出为double
     * @param pageSize
     * @param scrollTimeInMillis 游标的有效时间, 如果报错`Elasticsearch No search context found for id`, 则加大
     * @return
     */
    @JvmOverloads
    fun <T> scrollDocs(index: String, type: String, queryBuilder: EsQueryBuilder, clazz: Class<T>, pageSize: Int = 1000, scrollTimeInMillis: Long = 3000): EsScrollCollection<T> {
        return scrollDocs(index, type, queryBuilder, pageSize, scrollTimeInMillis) { result ->
            result.getSourceAsObjectList(clazz)
        }
    }

    /**
     * 开始搜索文档, 并返回有游标的结果集合
     * @param index
     * @param type
     * @param queryBuilder 查询对象, 分页无效
     * @param pageSize
     * @param scrollTimeInMillis 游标的有效时间, 如果报错`Elasticsearch No search context found for id`, 则加大
     * @return
     */
    private fun startScroll(index: String, type: String, queryBuilder: EsQueryBuilder, pageSize: Int, scrollTimeInMillis: Long): SearchResult {
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
     * @param scrollId
     * @param scrollTimeInMillis 游标的有效时间, 如果报错`Elasticsearch No search context found for id`, 则加大
     * @return
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
     *   只能迭代一次, 迭代一次后会close()
     */
    inner class EsScrollCollection<T>(
            protected val result: SearchResult,
            protected val scrollTimeInMillis: Long,
            protected val resultMapper:(JestResult)->Collection<T> // 结果转换器, 会将每一页的JestResult(兼容SearchResult/ScrollSearchResult), 转为T对象集合
    ) : AbstractCollection<T>() {

        // 迭代次数
        protected var iterateCount = AtomicInteger(0);

        // 总数
        override val size: Int = result.total.toInt()

        // 获得迭代器
        override fun iterator(): MutableIterator<T> {
            if(iterateCount.getAndIncrement() > 0)
                throw EsException("EsScrollCollection 只能迭代一次")

            return ScrollIterator(result)
        }

        /**
         * 有游标的迭代器
         *   只能迭代一次, 迭代一次后会close()
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