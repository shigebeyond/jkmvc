package net.jkcode.jkmvc.es

import net.jkcode.jkmvc.orm.OrmEntity

/**
 * 对象关系映射
 */
class Odm: OrmEntity() {

    /**
     * es主键
     */
    public val _id: String
        get() = this[odmMeta.idProp]

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
        return odmMeta.deleteById(_id)
    }

}