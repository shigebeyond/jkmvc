package net.jkcode.jkmvc.http

import net.jkcode.jkmvc.common.Config
import net.jkcode.jkmvc.common.IPlugin
import net.jkcode.jkmvc.common.ThreadLocalInheritableThreadPool
import net.jkcode.jkmvc.http.handler.HttpRequestHandler
import javax.servlet.*
import javax.servlet.http.HttpServletRequest

/**
 * web入口
 *
 * @author shijianhang
 * @date 2019-4-13 上午9:27:56
 */
open class JkFilter() : Filter {

    /**
     * 静态文件uri的正则
     */
    protected val staticFileRegex: Regex = ".*\\.(gif|jpg|jpeg|png|bmp|ico|swf|js|css|eot|ttf|woff)$".toRegex(RegexOption.IGNORE_CASE)

    /**
     * 插件配置
     */
    public val pluginConfig: Config = Config.instance("plugin", "yaml")

    /**
     * 插件列表
     */
    public val plugins: List<IPlugin> = pluginConfig.classes2Instances("interceptors")

    /**
     * 初始化
     */
    override fun init(filterConfig: FilterConfig) {
        // fix bug: jetty异步请求后 req.contextPath/req.servletContext 居然为null, 因此直接在 JkFilter.init() 时记录 contextPath, 反正他是全局不变的
        HttpRequest.globalServletContext = filterConfig.servletContext

        // 将公共线程池应用到 CompletableFuture.asyncPool
        ThreadLocalInheritableThreadPool.applyCommonPoolToCompletableFuture()

        // 初始化插件
        for(p in plugins)
            p.start()
    }

    /**
     * 执行过滤
     */
    override fun doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) {
        //　静态文件请求，则交给下一个filter来使用默认servlet来处理
        if(staticFileRegex.matches((req as HttpServletRequest).requestURI)) {
            chain.doFilter(req, res)
            return;
        }

        // 1 异步处理
        if(req.isAsyncSupported) {
            // 异步上下文, 在完成异步操作后, 需要调用 actx.complete() 来关闭异步响应, 调用下放到 RequestHandler.handle()
            val actx = req.startAsync(req, res)

            // 异步处理请求
            //actx.start { // web server线程池
            ThreadLocalInheritableThreadPool.commonPool.execute { // 其他线程池
                handleRequest(actx.request, actx.response, chain)

            }
            return;
        }

        // 2 同步处理
        handleRequest(req, res, chain)
    }

    /**
     * 处理请求
     */
    protected fun handleRequest(req: ServletRequest, res: ServletResponse, chain: FilterChain) {
        try{
            // 处理请求
            val handled = HttpRequestHandler.handle(req, res)

            //　如果没有处理，则交给下一个filter来使用默认servlet来处理
            // if not handled, we delegate to next filter to use the default servlets
            if (!handled)
                chain.doFilter(req, res)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun destroy() {
        // 关闭插件
        for(p in plugins)
            p.close()
    }
}
