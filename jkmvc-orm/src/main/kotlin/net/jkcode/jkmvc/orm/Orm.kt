package net.jkcode.jkmvc.orm

import net.jkcode.jkutil.common.getPathPropertyValue

/**
 * ORM

 * @Package package_name
 * @category
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 */
abstract class Orm @JvmOverloads constructor(vararg pks: Any/* 主键值, 非null */, useCache: Boolean = true /* 是否使用缓存 */) : OrmRelated() {

    @JvmOverloads
    constructor(singlePk: Any?, useCache: Boolean = true): this(*(if(singlePk == null) emptyArray() else arrayOf(singlePk)), useCache = useCache)

    init{
        // 根据主键值来加载数据
        if(pks.isNotEmpty())
            loadByPk(*pks, useCache = useCache)
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
     *
     * @param from 要复制的对象
     * @param include 要设置的字段名的列表
     * @param exclude 要排除的字段名的列表
     * @param includeRelated 是否包含关联属性, 仅当 include 为空时有效
     */
    public fun fromOrm(from: Orm, include: List<String> = emptyList(), exclude: List<String> = emptyList(), includeRelated: Boolean = false): Orm {
        fromMap(from._data, include, exclude, includeRelated)
        return this
    }

    /**
     * 获得字段值 -- 转为Map
     * @param to
     * @param include 要设置的字段名的列表
     * @param exclude 要排除的字段名的列表
     * @return
     */
    override fun toMap(to: MutableMap<String, Any?>, include: List<String>, exclude: List<String>): MutableMap<String, Any?> {
         super.toMap(to, include, exclude)

        for(column in include){
            if(exclude.contains(column) // 排除
                    || ormMeta.props.contains(column) // 自身属性
                    || ormMeta.hasRelation(column) // 关联属性
            )
                continue

            to[column] = this.getPathPropertyValue(column) // 支持多级属性
        }

        return to
    }
}
