package net.jkcode.jkmvc.http

import net.jkcode.jkmvc.http.controller.Controller
import net.jkcode.jkutil.ttl.HttpRequestScopedTransferableThreadLocal
import net.jkcode.jkutil.ttl.SttlCurrentHolder

/**
 * http状态
 */
data class HttpState(
        public val req: HttpRequest, // 请求
        public val res: HttpResponse, // 响应
        public val controller: Any? // 控制器，兼容java、jphp
) {

    companion object: SttlCurrentHolder<HttpState>(HttpRequestScopedTransferableThreadLocal()) // http请求域的可传递的 ThreadLocal
}