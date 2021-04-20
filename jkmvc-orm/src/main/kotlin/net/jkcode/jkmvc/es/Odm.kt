package net.jkcode.jkmvc.es

import net.jkcode.jkmvc.orm.OrmEntity
import net.jkcode.jkutil.common.dateFormat

/**
 * 对象关系映射
 */
class Odm: OrmEntity() {

    public val id: String
        get() = this[odmMeta.idField.name]

    /**
     * 元数据
     *   伴随对象就是元数据
     */
    public val odmMeta: OdmMeta
        get() = this::class.modelOdmMeta

    /**
     * 新增
     */
    fun create(): Boolean {
        return odmMeta.create(this)
    }

    /**
     * 更新
     */
    fun update(): Boolean {
        return odmMeta.update(this)
    }

    /**
     * 删除
     */
    fun delete(): Boolean {
        return odmMeta.deleteById(id)
    }

}