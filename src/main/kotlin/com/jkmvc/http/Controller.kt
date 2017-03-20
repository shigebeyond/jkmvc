package com.jkmvc.http

import java.util.*

/**
 * 控制器
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 *
 */
abstract class Controller(protected val req: Request /* 请求对象 */, protected val res: Response /* 响应对象 */) {

    /**
     * 视图
     */
    public fun view(file:String, data:MutableMap<String, Any?> = HashMap<String, Any?>()):View
    {
        return View(req, res, file, data);
    }

    /**
     * 视图
     */
    public fun view(data:MutableMap<String, Any?> = HashMap<String, Any?>()):View
    {
        return view( req.controller() + "/" + req.action(), data)
    }

}