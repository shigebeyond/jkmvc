package net.jkcode.jkmvc.http.handler

import net.jkcode.jkmvc.http.IHttpInterceptor
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

/**
 * http请求处理者
 *
 * @author shijianhang
 * @date 2016-10-6 上午9:27:56
 *
 */
interface IHttpRequestHandler {

    /**
     * 拦截器
     */
    val interceptors: List<IHttpInterceptor>

    /**
     * 处理请求
     *
     * @param HttpServletRequest req
     * @param HttpServletResponse res
     * @return 是否处理，如果没有处理（如静态文件请求），则交给下一个filter/默认servlet来处理
     */
    fun handle(request: ServletRequest, response: ServletResponse): Boolean

}
