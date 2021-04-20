package net.jkcode.jkmvc.es

/**
 * odm的元数据
 */
class OdmMeta(
        public val model: Class<*>, // 模型类
        public val index: String, // 索引, 相当于db
        public var type: String, // 类别, 相当于表
        public var idField: String, // id字段
        public val esName: String = "default" // es配置名
){

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
    fun create(item: Odm): Boolean {
        return esmgr.insertDoc(index, type, item)
    }

    /**
     * 更新
     */
    fun update(item: Odm): Boolean {
        return esmgr.updateDoc(index, type, item[idField], item)
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
}