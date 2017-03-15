package com.jkmvc.orm

import java.util.*

/**
 * ORM之元数据
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-10
 *
 */
abstract class OrmMetaData(data: MutableMap<String, Any?> = LinkedHashMap<String, Any?>()) : OrmValid(data) {

    /**
     * 获得主键值
     * @return int|string
     */
    public override fun pk() {
        return this[metadata.primaryKey];
    }
}