package net.jkcode.jkmvc.http.handler

import co.paralleluniverse.fibers.Suspendable
import net.jkcode.jkguard.IMethodMeta
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
import net.jkcode.jphp.ext.PhpReturnCompletableFuture
import net.jkcode.jphp.ext.PhpMethodMeta
import php.runtime.Memory
import php.runtime.env.Environment
import php.runtime.env.handler.ExceptionHandler
import php.runtime.lang.exception.BaseBaseException
import php.runtime.memory.ObjectMemory
import php.runtime.memory.StringMemory
import php.runtime.reflection.ClassEntity
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
     * 处理请求
     * @param req
     * @param res
     * @return
     */
    public override fun handle(request: HttpServletRequest, response: HttpServletResponse): CompletableFuture<*> {
        // 允许跨域
        if (config.getBoolean("allowCrossDomain", false)!!) {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "POST,GET,OPTIONS,PUT,DELETE,HEAD");
            response.setHeader("Access-Control-Allow-Headers", "origin,cache-control,content-type,accept,hash-referer,x-requested-with,token");// 跨域验证登录用户，要用到请求头中的token
            if (request.isOptions)
                return VoidFuture
        }

        //　构建请求/响应对象
        val req = HttpRequest(request);
        val res = HttpResponse(response, req);

        // 1 先解析路由: 因为拦截器interceptors可能依赖路由信息, 如HttpServerTraceInterceptor需要controller+action信息
        if (!req.parseRoute())// 直接抛异常
            return completeExceptionFuture(RouteException("当前uri [${req.routeUri}] 没有匹配路由"))

        // 2 拦截与调用controller, 并返回future(参考 invokeAfterGuard() 实现)
        return if (req.isInner) // 内部请求: 不新开作用域, 也就是复用起始请求的作用域及资源(如db/当前用户)
                    interceptAndCallController(req, res)
                else // 起始请求: 新开作用域, 因为拦截器(如HttpServerTraceInterceptor)会用到sttl(有作用域的可传递的ThreadLocal), 因此需用作用域包装拦截器处理
                    GlobalHttpRequestScope.sttlWrap {
                        interceptAndCallController(req, res)
                    }
    }

    /**
     * 真正的处理请求
     *   1 拦截
     *   2 解析与调用controller
     *   3 对内部请求, 记录与恢复原来的controller
     * @param req
     * @param res
     * @return
     */
    @Suspendable
    private fun interceptAndCallController(req: HttpRequest, res: HttpResponse): CompletableFuture<Any?> {
        val oldState = HttpState.currentOrNull() // 获得旧的当前状态

        // 1 加拦截
        return interceptorChain.intercept(req) {
            // 2 解析与调用controller
            if (req.controller.startsWith('$')) // 如果controller是$开头，则表示走php controller
                callPhpController(req, res)
            else // 否则，走java controller
                callJavaController(req, res)
        }.whenComplete{ r, ex ->
            // 3 对于内部请求, 需恢复旧的当前状态
            if(oldState != null)
                HttpState.setCurrent(oldState)
        }
    }

    /***************** 调用java的controller 实现 *****************/
    /**
     * 调用java的controller
     */
    @Suspendable
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

        // 设置当前状态
        HttpState.setCurrentByController(controller)

        // 4 调用controller的action方法
        //return controller.callActionMethod(action.javaMethod!!)
        return guardInvoke(action, controller, emptyArray())
    }

    /***************** 调用php的controller 实现 *****************/
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
     *   callController.php负责工作： 1 定义controller基类 2 创建controller实例 3 HttpState.setCurrentByController() 4 guardInvoke()即调用action
     */
    private fun callPhpController(req: HttpRequest, res: HttpResponse){
        // 执行 callController.php
        val lan = JphpLauncher
        val phpFile = Thread.currentThread().contextClassLoader.getResource("jphp/callController.php").path
        val data = mapOf(
                "req" to PHttpRequest(lan.environment, req),
                "res" to PHttpResponse(lan.environment, res)
        )
        //lan.run(phpFile, data, res.outputStream, exceptionHandler = phpExceptionHandler) // 异常处理
        lan.run(phpFile, data, res.outputStream)
    }

    /***************** MethodGuardInvoker 实现 *****************/
    /**
     * 守护php方法调用 -- 入口
     *
     * @param methodName 方法
     * @param obj 对象
     * @param args0 参数
     * @return 结果
     */
    @Suspendable
    fun guardInvoke(obj: ObjectMemory, methodName: StringMemory, args: Array<Memory>, env: Environment): Memory {
        // 获得php类
        val classEntity: ClassEntity = obj.value.reflection
        // 获得php方法
        val method = classEntity.findMethod(methodName.toString().toLowerCase())
        // 调用php方法
        return guardInvoke(PhpMethodMeta(method, this), obj as Any, args as Array<Any?>) as Memory
    }

    /**
     * 守护之后真正的调用
     *    实现：server端实现是调用原生方法, client端实现是发rpc请求
     *    => 调用controller的action方法
     *
     * @param action 方法
     * @param controller 对象
     * @param args 参数
     * @return
     */
    @Suspendable
    public override fun invokeAfterGuard(action: IMethodMeta, controller: Any, args: Array<Any?>): CompletableFuture<Any?> {
        // 调用controller的action方法
        // 1 php方法: 调用action.invoke(), 涉及到各种转类型
        if(action is PhpMethodMeta)
            return PhpReturnCompletableFuture.tryPhpSupplierFuture{ action.invoke(controller, *args) as Memory }

        // 2 java方法: 也是调用 action.invoke()，只是封装了更多java controller的特性
        return (controller as Controller).callActionMethod(action)
    }

    /**
     * 获得调用的对象
     *   合并后会异步调用其他方法, 原来方法的调用对象会丢失
     * @param method
     * @return
     */
    public override fun getCombineInovkeObject(method: IMethodMeta): Any {
        if(!JkApp.useSttl) // 必须包装sttl
            throw IllegalStateException()

        return HttpState.current().controller!!
    }

}
