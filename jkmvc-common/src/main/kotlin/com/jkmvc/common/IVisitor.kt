package com.jkmvc.common

/**
 * 访问者接口
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-01-18 8:48 AM
 */
interface IVisitor<T> {

    /**
     * 访问单个项目
     * @param item
     */
    fun visit(item: T)
}