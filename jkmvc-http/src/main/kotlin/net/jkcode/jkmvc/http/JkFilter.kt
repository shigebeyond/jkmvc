package net.jkcode.jkmvc.http


import net.jkcode.jkmvc.http.handler.RequestHandler
import java.util.concurrent.ForkJoinPool
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

open class JkFilter : Filter {



    override fun init(filterConfig: FilterConfig) {
        // fix bug: jetty异步请求后 req.contextPath/req.servletContext 居然为null, 因此直接在 JkFilter.init() 时记录 contextPath, 反正他是全局不变的
        HttpRequest.globalServletContext = filterConfig.servletContext
    }

    override fun doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) {
        // 1 异步处理
        httpLogger.info("before - req.servletContext is null : " + (req.servletContext == null))
        httpLogger.info("before - req.servletContext == filterConfig.servletContext : " + (req.servletContext == HttpRequest.globalServletContext))
        httpLogger.info("before - req.getRequestDispatcher(jsp) is null : " + (req.getRequestDispatcher("/user/index.jsp") == null))

        if(req.isAsyncSupported) {
            // 异步上下文
            val actx = req.startAsync(req, res)

            // 异步处理请求
            //actx.start { // web server线程池
            ForkJoinPool.commonPool().execute { // 其他线程池
                try {
                    httpLogger.info("after - req.servletContext is null : " + (req.servletContext == null))
                    httpLogger.info("after - req.servletContext == filterConfig.servletContext : " + (req.servletContext == HttpRequest.globalServletContext))
                    httpLogger.info("after - req.getRequestDispatcher(jsp) is null : " + (req.getRequestDispatcher("/user/index.jsp") == null))

                    handleRequest(actx.request, actx.response, chain) // wrong: req.contextPath is null
                    //handleRequest(req, res, chain)
                } finally {
                    // 完成异步操作, 关闭异步响应
                    actx.complete()
                }
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
        // 处理请求
        val handled = RequestHandler.handle(req as HttpServletRequest, res as HttpServletResponse)

        //　如果没有处理（如静态文件请求），则交给下一个filter来使用默认servlet来处理
        // if not handled（eg request static file）, we delegate to next filter to use the default servlets
        if (!handled)
            chain.doFilter(req, res)
    }

    override fun destroy() {
    }
}
