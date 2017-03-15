package com.jkmvc.orm

/**
 * ORM之元数据
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-10
 *
 */
interface IOrmMetaData : IOrmValid {

    /**
     * 元数据
     */
    val metadata: MetaData;

    /**
     * 获得主键值
     * @return int|string
     */
    public fun pk();
}