package net.jkcode.jkmvc.http

import net.jkcode.jkmvc.http.controller.Controller
import net.jkcode.jkmvc.http.jphp.PHttpRequest
import net.jkcode.jkmvc.http.jphp.PHttpResponse
import net.jkcode.jkutil.ttl.HttpRequestScopedTransferableThreadLocal
import net.jkcode.jkutil.ttl.SttlCurrentHolder
import net.jkcode.jphp.ext.getPropJavaValue
import php.runtime.memory.ObjectMemory

/**
 * http状态
 */
data class HttpState(
        public val req: HttpRequest, // 请求
        public val res: HttpResponse, // 响应
        public val controller: Any? // 控制器，兼容java、jphp
) {

    companion object: SttlCurrentHolder<HttpState>(HttpRequestScopedTransferableThreadLocal()) // http请求域的可传递的 ThreadLocal
    {
        /**
         * 设置当前http状态
         */
        fun setCurrentByController(controller: Controller) {
            setCurrent(HttpState(controller.req, controller.res, controller))
        }

        /**
         * 设置当前http状态
         */
        fun setCurrentByController(controller: ObjectMemory) {
            val preq = controller.getPropJavaValue("req") as PHttpRequest
            val pres = controller.getPropJavaValue("res") as PHttpResponse
            setCurrent(HttpState(preq.request, pres.response, controller))
        }

    }
}