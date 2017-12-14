package com.jkmvc.http

/**
 * 加载Controller类
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
interface IControllerClassLoader {

    /**
     * 获得controller类
     * @param controller名
     * @return
     */
    fun getControllerClass(name: String): ControllerClass?;
}
