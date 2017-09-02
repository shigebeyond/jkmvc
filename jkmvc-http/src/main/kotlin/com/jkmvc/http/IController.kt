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
    var req: Request

    /**
     * 响应对象
     */
    var res: Response

    /**
     * 视图
     * @param file 视图文件
     * @param data 视图变量
     * @return 视图
     */
    fun view(file:String, data:MutableMap<String, Any?> = HashMap<String, Any?>()):View

    /**
     * 视图
     * @param data 视图变量
     * @return 视图
     */
    fun view(data:MutableMap<String, Any?> = HashMap<String, Any?>()):View

    /**
     * 重定向到指定url
     * @param uri
     */
    fun redirect(uri: String):Unit
}