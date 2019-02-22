package com.jkmvc.http


import com.jkmvc.http.handler.RequestHandler
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

open class JkFilter : Filter {

    override fun init(filterConfig: FilterConfig) {
    }

    override fun doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) {
        // 处理请求
        val handled = RequestHandler.handle(req as HttpServletRequest, res as HttpServletResponse)

        //　如果没有处理（如静态文件请求），则交给下一个filter来使用默认servlet来处理
        // if not handled（eg request static file）, we delegate to next filter to use the default servlets
        if(!handled)
            chain.doFilter(req, res)
    }

    override fun destroy() {

    }
}
