package net.jkcode.jkmvc.http.jphp

import net.jkcode.jkmvc.http.HttpRequest
import php.runtime.annotation.Reflection
import php.runtime.env.Environment
import php.runtime.lang.BaseObject

@Reflection.Name("HttpServerRequest")
@Reflection.Namespace(JkmvcHttpExtension.Companion.NS)
class PHttpRequest(env: Environment, protected val request: HttpRequest) : BaseObject(env) {

    @Reflection.Signature
    protected fun __construct() {
    }

    @Reflection.Signature
    fun header(name: String?): String {
        return request.getHeader(name)
    }

    @Reflection.Signature
    fun param(name: String?): String? {
        return request.getParameter(name!!)
    }

    @Reflection.Signature
    fun query(): String {
        return request.queryString
    }

    @Reflection.Signature
    fun path(): String {
        return request.pathInfo
    }

    @Reflection.Signature
    fun method(): String {
        return request.method
    }

    @Reflection.Signature
    fun sessionId(): String {
        return request.getSession(true).id
    }

    @Reflection.Signature
    fun controller(): String {
        //去掉$开头
        return request.controller.removePrefix("$")
    }

    @Reflection.Signature
    fun action(): String {
        return request.action
    }
}