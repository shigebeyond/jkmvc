package net.jkcode.jkmvc.http

import net.jkcode.jkmvc.http.handler.HttpRequestHandler
import net.jkcode.jkmvc.http.router.Router
import net.jkcode.jkutil.common.*
import java.lang.IllegalStateException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.RejectedExecutionException
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * web入口
 *   全局只有一个
 *
 * @author shijianhang
 * @date 2019-4-13 上午9:27:56
 */
open class JkFilter() : Filter {

    companion object{
        /**
         * http配置
         */
        public val config = Config.instance("http", "yaml")

        /**
         * 静态文件的扩展名
         */
        protected val staticFileExts: List<String> = config.getString("staticFileExts", "gif|jpg|jpeg|png|bmp|ico|swf|js|css|eot|ttf|woff")!!.split('|')

        /**
         * 单例
         */
        protected var inst: JkFilter? = null

        @JvmStatic
        public fun instance(): JkFilter{
            return inst ?: throw IllegalStateException("There is no JkFilter")
        }
    }

    /**
     * 记录servletContext
     *   fix bug: jetty异步请求后 req.contextPath/req.servletContext 居然为null, 因此直接在 JkFilter.init() 时记录ServletContext(包含contextPath), 反正他是全局不变的
     */
    public lateinit var servletContext: ServletContext

    /**
     * url前缀, 尾部无/
     */
    public lateinit var urlPrefix: String

    /**
     * 初始化
     */
    override fun init(filterConfig: FilterConfig) {
        // 1 单例
        if(inst != null)
            throw IllegalStateException("Only 1 JkFilter can exist")
        inst = this

        // 2 记录servletContext -- fix bug: jetty异步请求后 req.contextPath/req.servletContext 居然为null, 因此直接在 JkFilter.init() 时记录 contextPath, 反正他是全局不变的
        servletContext = filterConfig.servletContext

        // 3 根据filter配置的<url-pattern>(假定只有一个), 解析出url前缀
        val reg: FilterRegistration = servletContext.getFilterRegistration(filterConfig.filterName)
        val urlPattern = reg.urlPatternMappings.first()
        // <url-pattern>规范: https://stackoverflow.com/questions/24652893/url-pattern-in-tomcat-and-jetty
        //1. A string beginning with a ‘/’ character and ending with a ‘/*’ suffix is used for path mapping.
        //2. A string beginning with a ‘*.’ prefix is used as an extension mapping.
        //3. The empty string ("") is a special URL pattern that exactly maps to the application's context root, i.e., requests of the form  http://host:port/<contextroot>/. In this case the path info is ’/’ and the servlet path and context path is empty string (““).
        //4. A string containing only the ’/’ character indicates the "default" servlet of the application. In this case the servlet path is the request URI minus the context path and the path info is null.
        //5. All other strings are used for exact matches only.
        // url前缀 = /*前面的子串
        if(urlPattern.contains("/*"))
            urlPrefix = urlPattern.substringBefore("/*")
        else
            urlPrefix = ""

        // 4 初始化插件
        PluginLoader.loadPlugins()

        // 5 加载路由配置
        Router.load()
    }

    /**
     * 执行过滤
     */
    override fun doFilter(req0: ServletRequest, res: ServletResponse, chain: FilterChain) {
        var req = req0 as HttpServletRequest
        // 内部请求: INCLUDE/FORWARD
        val isInnerReq = req0.isInner
        if(isInnerReq)
            req = InnerHttpRequest(req0) // 封装include请求, 其中 req0 是HttpRequest => 后续在 isJspRequest(req) 中的 req.requestURI 才会获得真正的uri

        //　静态文件/jsp请求，则交给下一个filter来使用默认servlet来处理
        if (isStaticFileRequest(req) || isJspRequest(req)) { // 检查后缀
            chain.doFilter(req0 /* 原始的请求 */, res)
            return;
        }

        // bug: 上传文件报错: No multipart config for servlet
        // fix: web.xml 中 <filter> 不支持 <multipart-config> 配置, 只有 <servlet> 才支持, 因此只能硬编码设置上传配置
        // TODO 临时处理, 只对jetty有效, 参考 https://stackoverflow.com/questions/52514462/jetty-no-multipart-config-for-servlet-problem
        if(req.isUpload) {
            // val uploadDir = UploadFileUtil.uploadRootDirectory // TODO: 如果jetty的临时上传目录就是我们的业务上传目录, 则会节省另存为的成本
            val uploadDir = "/tmp"
            val multipartConfig = MultipartConfigElement(uploadDir)
            req.setAttribute("org.eclipse.jetty.multipartConfig", multipartConfig)
        }

        // 1 异步处理
        if(req.isAsyncSupported // 支持异步
                && !isInnerReq // 非内部请求: 内部请求的处理还是放到跟父请求的同一个线程处理
                && !config.getBoolean("debug")!!) { // 非调试
            // 异步处理
            try {
                // 启动异步上下文: 1 必须在线程池之前调用, 否则jetty报状态错误 2 在完成异步操作后, 需要调用 actx.complete() 来关闭
                val actx = req.startAsync(req, res)
                //actx.start { // web server线程池
                CommonExecutor.execute { // 其他线程池
                    // 异步处理
                    handleRequest(actx.request as HttpServletRequest, actx.response as HttpServletResponse, chain)
                        .whenComplete { r, ex ->
                            // 关闭异步上下文
                            //req.asyncContext.complete()
                            actx.complete()
                        }
                }
            }catch (e: RejectedExecutionException){
                httpLogger.errorColor("JkFilter处理请求错误: 公共线程池已满", e)
            }
            return;
        }

        // 2 同步处理
        handleRequest(req, res as HttpServletResponse, chain).get() // 同步必须要调用get()来等待
    }

    /**
     * 检查是否jsp请求
     *    filter也可能匹配到jsp页面, 但JkFilter不处理
     * @param req
     * @return
     */
    protected open fun isJspRequest(req: HttpServletRequest): Boolean {
        return req.requestURI.endsWith(".jsp")
    }

    /**
     * 检查是否静态文件请求
     * @param req
     * @return
     */
    protected open fun isStaticFileRequest(req: HttpServletRequest): Boolean {
        val ext = req.requestURI.substringAfterLast('.') // 获得后缀
        return staticFileExts.contains(ext)
    }

    /**
     * 处理请求
     *   1 方便复用与重载
     *   2 统一处理(日志+渲染500错误)， 如果你先改变错误渲染，直接重写该方法
     * @param req
     * @param res
     * @return
     */
    protected open fun handleRequest(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain): CompletableFuture<*> {
        val f = HttpRequestHandler.handle(req, res)
        f.exceptionally { t ->
            // 异常日志
            val err = t.message ?: t.cause?.message
            val msg = "处理请求[${req.requestURI}]出错: ${err}"
            httpLogger.errorColor(msg, t)
            // 渲染500错误
            res.sendError(500, msg)
            null
        }
        return f
    }

    /**
     * 析构
     */
    override fun destroy() {
    }
}
