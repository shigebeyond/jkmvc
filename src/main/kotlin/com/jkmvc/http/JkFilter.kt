package com.jkmvc.http


import com.jkmvc.db.Db
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

open class JkFilter : Filter {

    override fun init(filterConfig: FilterConfig) {
    }

    override fun doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) {
        // 处理请求
        val handled = Server.run(req as HttpServletRequest, res as HttpServletResponse)

        //　如果没有处理，则交给下一个filter来使用默认servlet来处理，如处理静态文件/上传文件
        // if not handled, we delegate to next filter to use the default servlets, to serve static files　/ uploaded files
        if(!handled)
            chain.doFilter(req, res)
    }

    override fun destroy() {
        // 关闭当前线程相关的所有db连接
        Db.closeAllDb();
    }
}
