package net.jkcode.jkmvc.es

import net.jkcode.jkmvc.es.annotation.esDoc
import net.jkcode.jkmvc.es.annotation.esIdProp
import net.jkcode.jkutil.common.getPropertyValue
import java.util.concurrent.ConcurrentHashMap

/**
 * 文档管理者
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
        val adoc = model.kotlin.esDoc ?: throw EsException("Class $model miss @EsDoc annotation")
        index = adoc.index
        type = adoc.type
        idProp = model.kotlin.esIdProp?.name ?: throw EsException("$model 没有用 @EsId 注解来指定 _id 属性")
    }

    /**
     * 获得es管理器
     */
    val esmgr: EsManager
        get() = EsManager.instance(esName)

    /**
     * 获得查询构建器
     */
    fun queryBuilder(): ESQueryBuilder{
        return esmgr.queryBuilder().index(index).type(type)
    }

    /**
     * 新增
     */
    fun create(item: T): Boolean {
        val id = item.getPropertyValue(idProp).toString()
        return esmgr.insertDoc(index, type, item, id)
    }

    /**
     * 更新
     */
    fun update(item: T): Boolean {
        val id = item.getPropertyValue(idProp).toString()
        return esmgr.updateDoc(index, type, item, id)
    }

    /**
     * 查一个
     */
    fun <T> findById(id: String): T? {
        return esmgr.getDoc(index, type, id, model) as T
    }

    /**
     * 删一个
     */
    fun deleteById(id: String): Boolean {
        return esmgr.deleteDoc(index, type, id)
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
