package net.jkcode.jkmvc.es

import net.jkcode.jkmvc.es.annotation.esDoc
import net.jkcode.jkmvc.es.annotation.esIdProp
import net.jkcode.jkutil.common.getPropertyValue
import java.util.concurrent.ConcurrentHashMap

/**
 * 文档仓库
 *
 * @author shijianhang
 * @date 2021-4-21 下午5:16:59
 */
class EsDocRepository<T: Any>(
        public val model: Class<T> // 模型类
){

    /**
     * 索引, 相当于db
     */
    public lateinit var index: String

    /**
     * 类别, 相当于表
     */
    public lateinit var type: String

    /**
     * es配置名
     */
    public lateinit var esName: String

    /**
     * id属性名
     */
    public lateinit var idProp: String

    init {
        val adoc = model.kotlin.esDoc // @EsDoc 注解
                ?: throw EsException("Class $model miss @EsDoc annotation")
        index = adoc.index
        type = adoc.type
        esName = adoc.esName
        idProp = model.kotlin.esIdProp?.name // @EsId 注解, 用在kotlin属性中
                ?: model.jestIdField?.name // @JestId 注解, 用在java字段中
                ?: throw EsException("$model 没有用 @EsId 或 @JestId 注解来指定 _id 属性")
    }

    /**
     * 获得es管理器
     */
    val esmgr: EsManager
        get() = EsManager.instance(esName)

    /**
     * 获得对象id
     *   用 @EsId 注解的属性值
     */
    fun getId(item: T): String? {
        return item.getPropertyValue(idProp)?.toString()
    }

    /**
     * 获得查询构建器
     */
    fun queryBuilder(): EsQueryBuilder{
        return esmgr.queryBuilder().index(index).type(type)
    }

    /**
     * 保存(插入或更新)
     */
    fun save(item: T): Boolean {
        val id = getId(item)
        return esmgr.indexDoc(index, type, item, id)
    }

    /**
     * 保存(插入或更新)
     */
    fun saveAll(items: List<T>) {
        esmgr.bulkIndexDocs(index, type, items)
    }

    /**
     * 更新
     */
    fun update(item: T): Boolean {
        val id = getId(item) ?: throw IllegalArgumentException("Miss _id")
        return esmgr.updateDoc(index, type, item, id)
    }

    /**
     * 删一个
     */
    fun deleteById(id: String): Boolean {
        return esmgr.deleteDoc(index, type, id)
    }

    /**
     * 删一个
     */
    fun delete(item: T): Boolean {
        val id = getId(item) ?: throw IllegalArgumentException("Miss _id")
        return esmgr.deleteDoc(index, type, id)
    }

    /**
     * 删多个
     */
    fun deleteAll(items: List<T>) {
        for (item in items)
            delete(item)
    }

    /**
     * 删多个
     */
    @JvmOverloads
    fun deleteAll(query: EsQueryBuilder? = null, pageSize: Int = 1000, scrollTimeInMillis: Long = 3000): Collection<String> {
        return prepareQueryBuilder(query).deleteDocs(pageSize, scrollTimeInMillis)
    }

    /**
     * 根据id查一个
     */
    fun findById(id: String): T? {
        return esmgr.getDoc(index, type, id, model) as T
    }

    /**
     * 根据id查多个
     */
    @JvmOverloads
    fun findAllByIds(ids: Collection<String>): List<T> {
        return esmgr.multGetDocs(index, type, ids, model)
    }

    /**
     * 查多个
     */
    @JvmOverloads
    fun findAll(query: EsQueryBuilder? = null): Pair<List<T>, Long> {
        return prepareQueryBuilder(query).searchDocs(model)
    }

    /**
     * 开始搜索文档, 并返回有游标的结果集合
     * @param query 查询对象, 分页无效
     * @param pageSize
     * @param scrollTimeInMillis
     * @return
     */
    @JvmOverloads
    fun scrollAll(query: EsQueryBuilder? = null, pageSize: Int = 1000, scrollTimeInMillis: Long = 3000): EsManager.EsScrollCollection<T>{
        return prepareQueryBuilder(query).scrollDocs(model, pageSize, scrollTimeInMillis)
    }

    /**
     * 计数
     */
    @JvmOverloads
    fun count(query: EsQueryBuilder? = null): Long {
        return prepareQueryBuilder(query).count()
    }

    /**
     * 准备好query buider
     */
    protected fun prepareQueryBuilder(query: EsQueryBuilder?): EsQueryBuilder {
        if(query == null)
            return queryBuilder()

        query.index(index).type(type)
        return query
    }

    /**
     * 刷新索引
     */
    fun refresh(): Boolean {
        return esmgr.refreshIndex(index)
    }

    companion object{

        /**
         * <类 to OdmMeta实例>
         */
        private val insts: ConcurrentHashMap<Class<*>, EsDocRepository<*>> = ConcurrentHashMap();

        /**
         * 获得OdmMeta实例
         */
        fun <T: Any> instance(model: Class<T>): EsDocRepository<T> {
            return insts.getOrPut(model){
                EsDocRepository(model)
            } as EsDocRepository<T>
        }
    }

}
