package com.jkmvc.orm

/**
 * ORM

 * @Package package_name
 * @category
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 */
abstract class Orm(pk: Array<Any> /* 主键值, 非null */) : OrmRelated() {

    // wrong: 主构造函数签名相同冲突
    //public constructor(vararg cols:Any):this(cols)

    // 逐个实现1个参数/2个参数/3个参数的构造函数
    public constructor(a: Any?):this(if(a == null) emptyArray() else arrayOf(a))

    public constructor(a: Any, b:Any):this(toArray(a, b))

    public constructor(a: Any, b:Any, c:Any):this(toArray(a, b, c))

    init{
        // 根据主键值来加载数据
        loadByPk(*pk)
    }

    public override fun toString(): String {
        return "${this.javaClass}: " + data.toString()
    }
}
