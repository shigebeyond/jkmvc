package com.jkmvc.orm

/**
 * ORM

 * @Package package_name
 * @category
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 */
abstract class Orm(vararg pk:Any /* 主键值, 非null */) : OrmRelated() {

    init{
        // 根据主键值来加载数据
        loadByPk(*pk)
    }

    public override fun toString(): String {
        return "${this.javaClass}: " + data.toString()
    }
}
