package com.jkmvc.orm

/**
 * ORM之数据校验
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
interface IOrmValid : IOrmEntity {

    /**
     * 校验数据
     * @return boolean
     */
    fun check(): Boolean;
}
