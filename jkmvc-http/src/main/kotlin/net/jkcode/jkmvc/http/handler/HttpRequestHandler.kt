package net.jkcode.jkmvc.http.handler

import net.jkcode.jkmvc.scope.GlobalRequestScope
import net.jkcode.jkmvc.common.Config
import net.jkcode.jkmvc.common.httpLogger
import net.jkcode.jkmvc.common.ucFirst
import net.jkcode.jkmvc.http.*
import net.jkcode.jkmvc.http.controller.Controller
import net.jkcode.jkmvc.http.controller.ControllerClass
import net.jkcode.jkmvc.http.controller.ControllerClassLoader
import net.jkcode.jkmvc.http.router.RouteException
import net.jkcode.jkmvc.interceptor.RequestInterceptorChain
import net.jkcode.jkmvc.ttl.SttlInterceptor
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

/**
 * http请求处理者
 *    在请求处理前后调用 GlobalRequestScope 的 beginScope()/endScope()
 *
 * @author shijianhang
 * @date 2016-10-6 上午9:27:56
 *
 */
object HttpRequestHandler : IHttpRequestHandler/*, MethodGuardInvoker()*/ {

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
    public val debug:Boolean = config.getBoolean("debug", false)!!;

    /**
     * 处理请求
     *
     * @param req
     * @param res
     * @return 是否处理，如果没有处理（如静态文件请求），则交给下一个filter/默认servlet来处理
     */
    public override fun handle(request: ServletRequest, response: ServletResponse):Boolean{
        //　构建请求对象
        val req = HttpRequest(request as HttpServletRequest);
        if(debug){
            // 路由
            httpLogger.debug("请求uri: {} {}, contextPath: {}", req.method, req.routeUri, req.contextPath)

            // curl命令
            if(!req.isUpload()) // 上传请求，要等到设置了上传子目录，才能访问请求参数
                httpLogger.debug(req.buildCurlCommand())
        }

        // 构建响应对象
        val res = HttpResponse(response as HttpServletResponse);

        // 允许跨域
        if(config.getBoolean("allowCrossDomain", false)!!){
            res.setHeader("Access-Control-Allow-Origin", "*");
            res.setHeader("Access-Control-Allow-Methods", "POST,GET,OPTIONS,PUT,DELETE,HEAD");
            res.setHeader("Access-Control-Allow-Headers", "origin,cache-control,content-type,accept,hash-referer,x-requested-with,token");// 跨域验证登录用户，要用到请求头中的token
            if(req.isOptions())
                return true;
        }

        // 1 先解析路由: 因为interceptors可能依赖路由信息
        if (!req.parseRoute())
            throw RouteException("当前uri没有匹配路由：" + req.requestURI);
        if(debug)
            httpLogger.debug("当前uri匹配路由: controller=[{}], action=[{}]", req.controller, req.action)

        // 2 调用controller与action
        val future = interceptorChain.intercept(req) {
            callController(req, res)
        }

        // 3 关闭请求(资源)
        future.whenComplete(SttlInterceptor.interceptToBiConsumer { r, ex ->
            endRequest(req, ex) // 关闭请求(资源)
        })
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
        // 获得controller类
        val clazz: ControllerClass? = ControllerClassLoader.get(req.controller);
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

        // 设置req/res属性
        controller.req = req;
        controller.res = res;

        // 请求处理前，开始作用域
        GlobalRequestScope.beginScope()

        // 调用controller的action方法
        return controller.callActionMethod(action.javaMethod!!)
        //return guardInvoke(action.javaMethod!!, controller, emptyArray())
    }

    /**
     * 关闭请求(资源)
     * @param req
     * @param ex
     */
    private fun endRequest(req: HttpRequest, ex: Throwable?) {
        // 0 打印异常
        if(ex != null)
            ex.printStackTrace()

        // 1 请求处理后，结束作用域(关闭资源)
        GlobalRequestScope.endScope()

        // 2 如果是异步操作, 则需要关闭异步响应
        if (req.isAsyncStarted)
            req.asyncContext.complete()
    }

    /**
     * 获得调用的对象
     * @param method
     * @return
     */
    /*public override fun getCombineInovkeObject(method: Method): Any{
        return Controller.current()
    }*/

    /**
     * 守护之后真正的调用
     *    调用controller的action方法
     *
     * @param action 方法
     * @param controller 对象
     * @param args 参数
     * @return
     */
    /*public override fun invokeAfterGuard(action: Method, controller: Any, args: Array<Any?>): CompletableFuture<Any?> {
        return trySupplierFuture {
            // 调用controller的action方法
            (controller as Controller).callActionMethod(action)
        }
    }*/

}
