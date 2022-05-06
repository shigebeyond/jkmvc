package net.jkcode.jkmvc.http.jphp

import net.jkcode.jkmvc.http.HttpRequest
import net.jkcode.jkmvc.http.HttpState
import net.jkcode.jkmvc.http.handler.HttpRequestHandler
import php.runtime.Memory
import php.runtime.annotation.Reflection
import php.runtime.env.Environment
import php.runtime.lang.BaseObject
import php.runtime.memory.ObjectMemory
import php.runtime.memory.StringMemory

@Reflection.Name("HttpRequest")
@Reflection.Namespace(JkmvcHttpExtension.NS)
class PHttpRequest(env: Environment, public val request: HttpRequest) : BaseObject(env) {

    @Reflection.Signature
    protected fun __construct() {
    }

    @Reflection.Signature
    fun header(name: String): String {
        return request.getHeader(name)
    }

    @Reflection.Signature
    fun param(name: String): String? {
        return request.getParameter(name)
    }

    @Reflection.Signature
    fun query(): String {
        return request.queryString
    }

    @Reflection.Signature
    fun uri(): String {
        return request.requestURI
    }

    @Reflection.Signature
    fun routeUri(): String {
        return request.routeUri
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

    companion object{

        @Reflection.Signature
        @JvmStatic
        fun current(env: Environment): Memory {
            return ObjectMemory(PHttpRequest(env, HttpRequest.current()))
        }

        @Reflection.Signature
        @JvmStatic
        fun setCurrentByController(controller: ObjectMemory) {
            HttpState.setCurrentByController(controller)
        }

        /**
         * 守护action方法调用
         */
        @Reflection.Signature
        @JvmStatic
        fun guardActionInvoke(env: Environment, obj: ObjectMemory, methodName: StringMemory, vararg args: Memory): Memory {
            return HttpRequestHandler.guardInvoke(env, obj, methodName, args as Array<Memory>)
        }
    }
}