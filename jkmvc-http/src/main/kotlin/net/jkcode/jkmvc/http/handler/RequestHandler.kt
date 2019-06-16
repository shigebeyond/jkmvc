package net.jkcode.jkmvc.http.handler

import net.jkcode.jkmvc.closing.ClosingOnRequestEnd
import net.jkcode.jkmvc.common.Config
import net.jkcode.jkmvc.common.ThreadLocalInheritableInterceptor
import net.jkcode.jkmvc.common.trySupplierFinally
import net.jkcode.jkmvc.common.ucFirst
import net.jkcode.jkmvc.http.HttpRequest
import net.jkcode.jkmvc.http.HttpResponse
import net.jkcode.jkmvc.http.controller.Controller
import net.jkcode.jkmvc.http.controller.ControllerClass
import net.jkcode.jkmvc.http.controller.ControllerClassLoader
import net.jkcode.jkmvc.http.httpLogger
import net.jkcode.jkmvc.http.isOptions
import net.jkcode.jkmvc.http.router.RouteException
import java.util.concurrent.CompletableFuture
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.reflect.KFunction

/**
 * http请求处理者
 *
 * @author shijianhang
 * @date 2016-10-6 上午9:27:56
 *
 */
object RequestHandler : IRequestHandler {

    /**
     * http配置
     */
    public val config = Config.instance("http", "yaml")

    /**
     * 是否调试
     */
    public val debug:Boolean = config.getBoolean("debug", false)!!;

    /**
     * 处理请求
     *
     * @param req
     * @param res
     * @return 是否处理，如果没有处理（如静态文件请求），则交给下一个filter/默认servlet来处理
     */
    public override fun handle(request: HttpServletRequest, response: HttpServletResponse):Boolean{
        //　构建请求对象
        val req = HttpRequest(request);
        if(debug){
            // 路由
            httpLogger.debug("请求uri: {} {}, contextPath: {}", req.method, req.routeUri, req.contextPath)

            // curl命令
            if(!req.isUpload()) // 上传请求，要等到设置了上传子目录，才能访问请求参数
                httpLogger.debug(req.buildCurlCommand())
        }

        //　如果是静态文件请求，则跳过路由解析
        if(req.isStaticFile())
            return false;

        val res = HttpResponse(response);
        trySupplierFinally(
            {callController(req, res)}, // 调用路由对应的controller与action
            ThreadLocalInheritableInterceptor().intercept{ r, e -> endRequest(req) } // 继承ThreadLocal + 关闭请求(ThreadLocal中的资源)
        )
        return true
    }

    /**
     * 调用controller与action
     *
     * @param req
     * @param res
     * @return
     */
    private fun callController(req: HttpRequest, res: HttpResponse): Any? {
        // 解析路由
        if (!req.parseRoute())
            throw RouteException("当前uri没有匹配路由：" + req.requestURI);

        if(debug)
            httpLogger.debug("当前uri匹配路由: controller=[{}], action=[{}]", req.controller, req.action)

        // 获得controller类
        val clazz: ControllerClass? = ControllerClassLoader.getControllerClass(req.controller);
        if (clazz == null)
            throw RouteException("Controller类不存在：" + req.controller);

        // 获得action方法
        val action: KFunction<*>? = clazz.getActionMethod(req.action);
        if (action == null){
            val method = "action" + req.action.ucFirst()
            throw RouteException("控制器${req.controller}不存在方法：${method}()");
        }

        // 创建controller
        val controller: Controller = clazz.clazz.java.newInstance() as Controller;

        // 允许跨域
        if(config.getBoolean("allowCrossDomain", false)!!){
            res.setHeader("Access-Control-Allow-Origin", "*");
            res.setHeader("Access-Control-Allow-Methods", "POST,GET,OPTIONS,PUT,DELETE,HEAD");
            res.setHeader("Access-Control-Allow-Headers", "origin,cache-control,content-type,accept,hash-referer,x-requested-with,token");// 跨域验证登录用户，要用到请求头中的token
            if(req.isOptions())
                return null;
        }

        // 设置req/res属性
        controller.req = req;
        controller.res = res;

        // 调用controller的action方法
        return controller.callActionMethod(action)
    }

    /**
     * 关闭请求(资源)
     * @param req
     */
    private fun endRequest(req: HttpRequest) {
        // 请求处理后，关闭资源
        ClosingOnRequestEnd.triggerClosings()

        // 如果是异步操作, 则需要关闭异步响应
        if (req.isAsyncStarted)
            req.asyncContext.complete()
    }

}
