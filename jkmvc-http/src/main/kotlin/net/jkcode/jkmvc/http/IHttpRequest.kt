package net.jkcode.jkmvc.http

import net.jkcode.jkutil.common.generateId
import javax.servlet.DispatcherType
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper

/**
 * jkmvc特有的http请求基类
 *
 * @author shijianhang<772910474@qq.com>
 * @date 4/15/2020 7:58 PM
 */
abstract class IHttpRequest(req: HttpServletRequest): HttpServletRequestWrapper(req){

    /**
     * 请求标识, 调试用
     */
    public val id: Long by lazy {
        generateId("HttpRequest")
    }

    /**
     * 是否内部请求
     */
    public val isInner: Boolean
        get() = dispatcherType == DispatcherType.INCLUDE || dispatcherType == DispatcherType.FORWARD

    /**
     * 原始的请求 = 非 IHttpRequest 的请求
     */
    public val originalRequest: HttpServletRequest
        get() {
            var result = request
            while(result != null && result is IHttpRequest){
                result = result.request
            }
            return result as HttpServletRequest
        }

    /**
     * 发起servlet/jsp
     */
    val originalServletPath: String
        //get() = (request as HttpServletRequest).servletPath
        get() = originalRequest.servletPath

    public override fun toString(): String {
        return "${javaClass.name}{id=$id, servletPath=$servletPath, originalServletPath=$originalServletPath}"
    }
}