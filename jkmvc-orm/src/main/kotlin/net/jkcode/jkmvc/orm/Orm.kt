package net.jkcode.jkmvc.orm

import net.jkcode.jkutil.common.toArray
import java.lang.UnsupportedOperationException

/**
 * ORM

 * @Package package_name
 * @category
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 */
abstract class Orm(pk: Array<Any> = emptyArray() /* 主键值, 非null */) : OrmRelated() {

    // wrong: 主构造函数签名相同冲突
    //public constructor(vararg cols:Any):this(cols)

    // 逐个实现1个参数/2个参数/3个参数的构造函数
    public constructor(a: Any?):this(if(a == null) emptyArray() else arrayOf(a))

    public constructor(a: Any, b:Any):this(toArray(a, b))

    public constructor(a: Any, b:Any, c:Any):this(toArray(a, b, c))

    public constructor(a: Any, b:Any, c:Any, d: Any):this(toArray(a, b, c, d))

    public constructor(a: Any, b:Any, c:Any, d: Any, e: Any):this(toArray(a, b, c, d, e))

    init{
        // 根据主键值来加载数据
        if(pk.isNotEmpty())
            loadByPk(*pk)
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
     * @param expected 要设置的字段名的列表
     */
    public fun fromOrm(from: Orm, expected: List<String> = emptyList()) {
        val hasRelated = ormMeta.relations.keys.any {
            from._data.containsKey(it)
        }
        if(hasRelated)
            throw UnsupportedOperationException("不支持复制关联属性")

        fromMap(from._data, expected)
    }
}
