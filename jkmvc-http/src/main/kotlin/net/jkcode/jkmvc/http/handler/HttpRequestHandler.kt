package net.jkcode.jkmvc.http.handler

import net.jkcode.jkguard.MethodGuardInvoker
import net.jkcode.jkmvc.http.*
import net.jkcode.jkmvc.http.controller.Controller
import net.jkcode.jkmvc.http.controller.ControllerClass
import net.jkcode.jkmvc.http.controller.ControllerClassLoader
import net.jkcode.jkmvc.http.jphp.PHttpRequest
import net.jkcode.jkmvc.http.jphp.PHttpResponse
import net.jkcode.jkmvc.http.router.RouteException
import net.jkcode.jkutil.common.*
import net.jkcode.jkutil.interceptor.RequestInterceptorChain
import net.jkcode.jkutil.scope.GlobalHttpRequestScope
import net.jkcode.jphp.ext.JphpLauncher
import php.runtime.env.Environment
import php.runtime.env.handler.ExceptionHandler
import php.runtime.lang.exception.BaseBaseException
import java.lang.reflect.Method
import java.util.concurrent.CompletableFuture
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * http请求处理者
 *    在请求处理前后调用 GlobalRequestScope 的 beginScope()/endScope()
 *
 * @author shijianhang
 * @date 2016-10-6 上午9:27:56
 *
 */
object HttpRequestHandler : IHttpRequestHandler, MethodGuardInvoker() {

    /**
     * http配置
     */
    public val config = Config.instance("http", "yaml")

    /**
     * http请求处理的拦截器
     */
    public override val interceptors: List<IHttpRequestInterceptor> = config.classes2Instances("interceptors")

    /**
     * http请求处理的拦截器链表
     */
    private val interceptorChain = RequestInterceptorChain(interceptors)

    /**
     * 是否调试
     */
    public val debug: Boolean = config.getBoolean("debug", false)!!;

    /**
     * 处理请求
     *
     * @param req
     * @param res
     * @return
     */
    public override fun handle(request: HttpServletRequest, response: HttpServletResponse): CompletableFuture<*> {
        //　构建请求对象
        val req = HttpRequest(request);
        if (debug) {
            // 路由
            httpLogger.debug("{}请求uri: {} {}, contextPath: {}", if (req.isInner) "内部" else "", req.method, req.routeUri, req.contextPath)

            // curl命令
            if (!req.isInner && !req.isUpload) // 上传请求，要等到设置了上传子目录，才能访问请求参数
                httpLogger.debug(req.buildCurlCommand())
        }

        // 构建响应对象
        val res = HttpResponse(response, req);

        // 允许跨域
        if (config.getBoolean("allowCrossDomain", false)!!) {
            res.setHeader("Access-Control-Allow-Origin", "*");
            res.setHeader("Access-Control-Allow-Methods", "POST,GET,OPTIONS,PUT,DELETE,HEAD");
            res.setHeader("Access-Control-Allow-Headers", "origin,cache-control,content-type,accept,hash-referer,x-requested-with,token");// 跨域验证登录用户，要用到请求头中的token
            if (req.isOptions)
                return VoidFuture
        }

        // 1 先解析路由: 因为interceptors可能依赖路由信息
        if (!req.parseRoute()){
            val ex = RouteException("当前uri [${req.routeUri}] 没有匹配路由");
            httpLogger.errorAndPrint(ex.message, ex)
            return CompletableFuture<Any?>().apply {
                completeExceptionally(ex)
            }
        }

        if (debug)
            httpLogger.debug("当前uri [{}] 匹配路由: controller=[{}], action=[{}]", req.routeUri, req.controller, req.action)

        // 2 调用controller与action, 并返回future(参考 invokeAfterGuard() 实现)
        val future = if (req.isInner) // 内部请求: 不新开作用域, 也就是复用起始请求的作用域及资源(如db/当前用户)
            callController(req, res)
        else // 起始请求: 新开作用域
            GlobalHttpRequestScope.sttlWrap {
                // 包装请求作用域的处理
                //httpLogger.debug("Request [{}] scope begin", req)
                callController(req, res)
            }

        // 3 关闭请求(资源)
        return future.whenComplete { r, ex ->
            endRequest(req, ex) // 关闭异步请求
//            if(!req.isInner)
//                httpLogger.debug("Request [{}] scope end", req)
        }
    }

    /**
     * 调用controller与action
     *   1 调用controller
     *   2 记录与恢复原来的controller
     *   3 拦截
     *
     * @param req
     * @param res
     * @return
     */
    private fun callController(req: HttpRequest, res: HttpResponse): CompletableFuture<Any?> {
        val oldCtrl = Controller.currentOrNull() // 获得旧的当前controller

        // 加拦截
        return interceptorChain.intercept(req) {
            if(req.controller.startsWith('$')) // 如果controller是$开头，则表示走php controller
                callPhpController(req, res)
            else // 否则，走java controller
                callJavaController(req, res)
        }.whenComplete{ r, ex ->
            // 对于内部请求, 需恢复旧的当前controller
            if(oldCtrl != null)
                Controller.setCurrent(oldCtrl)
        }
    }

    /**
     * 调用java的controller
     */
    private fun callJavaController(req: HttpRequest, res: HttpResponse): Any? {
        // 1 获得controller类
        val clazz: ControllerClass? = ControllerClassLoader.get(req.controller);
        if (clazz == null)
            throw RouteException("Controller class not exists: " + req.controller);

        // 2 获得action方法
        val action: Method? = clazz.getActionMethod(req.action);
        if (action == null)
            throw RouteException("Controller ${req.controller} has no method: ${req.action}()");

        // 3 创建controller
        val controller: Controller = clazz.clazz.java.newInstance() as Controller;
        // 设置req/res属性
        controller.req = req;
        controller.res = res;

        Controller.setCurrent(controller)

        // 4 调用controller的action方法
        //return controller.callActionMethod(action.javaMethod!!)
        return guardInvoke(action, controller, emptyArray())
    }

    /**
     * 接管php异常处理
     *   特殊处理路由异常
     */
    private val phpExceptionHandler = object : ExceptionHandler(null, null) {
        override fun onException(env: Environment, ex: BaseBaseException): Boolean {
            // 1 路由异常：直接抛给java
            var msg = ex.toString()
            if(msg.contains("$404[")){
                msg = msg.substringBetween("$404[", "]")
                throw RouteException(msg, ex)
            }

            // 2 其他异常：默认处理
            ExceptionHandler.DEFAULT.onException(env, ex)
            return false
        }
    }

    /**
     * 调用php的controller
     */
    private fun callPhpController(req: HttpRequest, res: HttpResponse){
        val lan = JphpLauncher.instance()
        val phpFile = Thread.currentThread().contextClassLoader.getResource("jphp/callController.php").path
        val data = mapOf(
                "req" to PHttpRequest(lan.environment, req),
                "res" to PHttpResponse(lan.environment, res)
        )
        //lan.run(phpFile, data, res.outputStream, exceptionHandler = phpExceptionHandler) // 异常处理
        lan.run(phpFile, data, res.outputStream)
    }

    /**
     * 关闭异步请求
     * @param req
     * @param ex
     */
    private fun endRequest(req: HttpRequest, ex: Throwable?) {
        // 打印异常
        if (ex != null)
            ex.printStackTrace()

        // 关闭异步请求, 仅非内部请求
        if (req.isAsyncStarted && !req.isInner)
            req.asyncContext.complete()
    }

    /***************** MethodGuardInvoker 实现 *****************/
    /**
     * 获得调用的对象
     * @param method
     * @return
     */
    public override fun getCombineInovkeObject(method: Method): Any {
        return Controller.current()
    }

    /**
     * 守护之后真正的调用
     *    调用controller的action方法
     *
     * @param action 方法
     * @param controller 对象
     * @param args 参数
     * @return
     */
    public override fun invokeAfterGuard(action: Method, controller: Any, args: Array<Any?>): CompletableFuture<Any?> {
        // 调用controller的action方法
        return (controller as Controller).callActionMethod(action)
    }

}
