package com.jkmvc.http

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.reflect.KFunction

/**
 * 服务端对象，用于处理请求
 *
 * @author shijianhang
 * @date 2016-10-6 上午9:27:56
 *
 */
object Server:IServer {

    /**
     * 处理请求
     *
     * @param req
     * @param res
     */
    public override fun run(request: HttpServletRequest, response: HttpServletResponse) {
        // 构建请求与响应对象
        val req = Request(request);
        val res = Response(response);

        try {
            // 解析路由
            if (!req.parseRoute())
                throw RouteException("当前uri没有匹配路由：" + req.requestURI);

            // 调用路由对应的controller与action
            callController(req, res);
        }
        /* catch (e：RouteException)
        {
            // 输出404响应
            res.setStatus(404).send();
        }  */
        catch (e: Exception) {
            res.render("异常 - " + e.message)
        }

    }

    /**
     * 调用controller与action
     *
     * @param req
     * @param res
     */
    fun callController(req: Request, res: Response) {
        // 获得controller类
        val clazz:ControllerClass? = ControllerLoader.getControllerClass(req.controller());
        if (clazz == null)
            throw RouteException ("Controller类不存在：" + req.controller());

        // 获得action方法
        val action: KFunction<*>? = clazz.getActionMethod(req.action());
        if (action == null)
            throw RouteException ("控制器${req.controller()}不存在方法：${req.action()}");

        // 创建controller
        val controller:Controller = clazz.constructer.call(req, res) as Controller;

        // 设置req/res属性
        controller.req = req;
        controller.res = res;

        // 调用controller的action方法
        action.call(controller);
    }

}
