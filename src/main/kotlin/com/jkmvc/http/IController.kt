package com.jkmvc.http

import java.util.*

/**
 * 控制器
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 *
 */
interface IController{

    /**
     * 请求对象
     */
    val req: Request

    /**
     * 响应对象
     */
    val res: Response

    /**
     * 视图
     */
    public fun view(file:String, data:MutableMap<String, Any?> = HashMap<String, Any?>()):View

    /**
     * 视图
     */
    public fun view(data:MutableMap<String, Any?> = HashMap<String, Any?>()):View
}