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
     * @return 是否处理，如果没有处理，则交给下一个filter/默认servlet来处理，如处理静态文件请求
     */
    public override fun run(request: HttpServletRequest, response: HttpServletResponse):Boolean{
        //　构建请求对象
        val req:Request = Request(request);

        //　跳过路由解析: 对指定目录下的uri不进行路由解析，主要用于处理静态文件或上传文件
        if(req.isSkipRoute())
            return false;

        // 构建响应对象
        val res:Response = Response(response);

        try{
            // 解析路由
            if (!req.parseRoute())
                throw RouteException("当前uri没有匹配路由：" + req.requestURI);

            // 调用路由对应的controller与action
            callController(req, res);
            return true;
        }
        /* catch (e：RouteException)
        {
            // 输出404响应
            res.setStatus(404).send();
        }  */
        catch (e: Exception) {
//            res.render("异常 - " + e.message)
            e.printStackTrace(res.prepareWriter())
            return true
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
        val controller:Controller = clazz.constructer.call() as Controller;

        // 设置req/res属性
        controller.req = req;
        controller.res = res;

        // 调用controller的action方法
        action.call(controller);
    }

}
