package net.jkcode.jkmvc.http

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import net.jkcode.jkutil.common.*
import sun.misc.IOUtils
import java.util.*
import javax.servlet.RequestDispatcher
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import kotlin.collections.HashMap

/**
 * include的内部请求
 *    仅适配了jetty server
 *
 * @author shijianhang<772910474@qq.com>
 * @date 4/15/2020 7:58 PM
 */
class IncludedRequest(req: HttpServletRequest /* 请求对象, 是HttpRequest */): HttpServletRequestWrapper(req){

    override fun getContextPath(): String {
        return getAttribute(RequestDispatcher.INCLUDE_CONTEXT_PATH) as String?
                ?: super.getContextPath()
    }

    override fun getRequestURI(): String {
        return getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) as String?
                ?: super.getRequestURI()
    }

    override fun getServletPath(): String {
        return getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH) as String?
                ?: super.getServletPath()
    }

    override fun getPathInfo(): String {
        return getAttribute(RequestDispatcher.INCLUDE_PATH_INFO) as String?
                ?: super.getPathInfo()
    }

    override fun getQueryString(): String {
        return getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) as String?
                ?: super.getQueryString()
    }

    /**
     * 发起jsp
     */
    val originalJsp: String
        get() = (request as HttpServletRequest).servletPath

    /**
     * 父jsp
     */
    val parentJsp: String
        get(){
            // org.eclipse.jetty.server.Dispatcher.IncludeAttributes 对象
            val attr = request.getAttribute("_attr")
            return attr.callFunction(RequestDispatcher.INCLUDE_SERVLET_PATH) as String
        }
}