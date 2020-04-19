package net.jkcode.jkmvc.orm

import java.lang.UnsupportedOperationException

/**
 * ORM

 * @Package package_name
 * @category
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 */
abstract class Orm(vararg pks: Any/* 主键值, 非null */) : OrmRelated() {

    constructor(singlePk: Any?): this(*(if(singlePk == null) emptyArray() else arrayOf(singlePk)))

    init{
        // 根据主键值来加载数据
        if(pks.isNotEmpty())
            loadByPk(*pks)
    }

    /**
     * 检查相等
     */
    public override fun equals(other: Any?): Boolean {
        // TODO: 支持int转bool
        if(other is Orm)
            return _data == other._data

        return false
    }

    /**
     * 获得哈希码
     */
    public override fun hashCode(): Int {
        return pk.hashCode()
    }

    /**
     * 从orm对象中设置字段值
     * @param from   字段值的哈希：<字段名 to 字段值>
     * @param include 要设置的字段名的列表
     * @param exclude 要排除的字段名的列表
     */
    public fun fromOrm(from: Orm, include: List<String> = emptyList(), exclude: List<String> = emptyList()) {
        val hasRelated = ormMeta.relations.keys.any {
            from._data.containsKey(it)
        }
        if(hasRelated)
            throw UnsupportedOperationException("不支持复制关联属性")

        fromMap(from._data, include, exclude)
    }
}
