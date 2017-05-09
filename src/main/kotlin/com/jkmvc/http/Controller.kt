package com.jkmvc.http

/**
 * 控制器
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 *
 */
abstract class Controller(override val req: Request /* 请求对象 */, override val res: Response /* 响应对象 */) :IController {

    /**
     * 视图
     */
    public override fun view(file:String, data:MutableMap<String, Any?>):View
    {
        return View(req, res, file, data);
    }

    /**
     * 视图
     */
    public override fun view(data:MutableMap<String, Any?>):View
    {
        return view( req.controller() + "/" + req.action(), data)
    }

}