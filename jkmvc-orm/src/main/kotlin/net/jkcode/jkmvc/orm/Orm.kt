package net.jkcode.jkmvc.orm

import net.jkcode.jkutil.common.containsAny
import net.jkcode.jkutil.common.getPathPropertyValue
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
     *   不支持复制关联属性, 因为无法判断关联对象是要新增或修改, 因此就干脆不做
     *
     * @param from   字段值的哈希：<字段名 to 字段值>
     * @param include 要设置的字段名的列表
     * @param exclude 要排除的字段名的列表
     */
    public fun fromOrm(from: Orm, include: List<String> = emptyList(), exclude: List<String> = emptyList()) {
        // 是否复制关联属性: 只有include强制指定才有效, 默认是ormMeta.props(不包含关联属性)
        val hasRelated = include.containsAny(ormMeta.relations.keys)
        if(hasRelated)
            throw UnsupportedOperationException("不支持复制关联属性")

        val columns = if (include.isEmpty())
                        ormMeta.props // 不包含关联属性
                    else
                        include
        fromMap(from._data, columns, exclude)
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
