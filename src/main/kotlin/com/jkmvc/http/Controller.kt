package com.jkmvc.http

/**
 * 控制器
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 *
 */
abstract class Controller :IController {

    /**
     * 请求对象
     */
    override lateinit var req: Request

    /**
     * 响应对象
     */
    override lateinit var res: Response

    /**
     * 视图
     * @param file 视图文件
     * @param data 视图变量
     * @return 视图
     */
    public override fun view(file:String, data:MutableMap<String, Any?>):View
    {
        return View(req, res, file, data);
    }

    /**
     * 视图
     * @param data 视图变量
     * @return 视图
     */
    public override fun view(data:MutableMap<String, Any?>):View
    {
        return view( req.controller() + "/" + req.action(), data)
    }

    /**
     * 重定向到指定url
     * @param uri
     */
    public override fun redirect(uri: String):Unit
    {
        res.sendRedirect(req.absoluteUrl(uri));
    }
}