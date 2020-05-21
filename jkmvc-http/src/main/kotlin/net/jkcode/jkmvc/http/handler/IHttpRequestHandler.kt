package net.jkcode.jkmvc.http.handler

import net.jkcode.jkmvc.http.IHttpRequestInterceptor
import java.util.concurrent.CompletableFuture
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
     * http请求处理的拦截器
     */
    val interceptors: List<IHttpRequestInterceptor>

    /**
     * 处理请求
     *
     * @param HttpServletRequest req
     * @param HttpServletResponse res
     * @return
     */
    fun handle(request: ServletRequest, response: ServletResponse): CompletableFuture<*>

}
