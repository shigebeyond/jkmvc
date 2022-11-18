package net.jkcode.jkmvc.http.jphp

import net.jkcode.jkmvc.http.HttpRequest
import net.jkcode.jkmvc.http.HttpState
import net.jkcode.jkmvc.http.handler.HttpRequestHandler
import org.asynchttpclient.Response
import php.runtime.Memory
import php.runtime.annotation.Reflection
import php.runtime.env.Environment
import php.runtime.lang.BaseObject
import php.runtime.memory.ObjectMemory
import php.runtime.memory.StringMemory
import java.util.concurrent.CompletableFuture

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

    /**
     * 设置上传的子目录(上传文件要存的子目录)
     *   要在调用file()之前设置
     */
    @Reflection.Signature
    fun setUploadSubDir(uploadSubDir: String) {
        request.uploadSubDir = uploadSubDir
    }

    /**
     * 保存上传文件, 并返回相对路径
     */
    @Reflection.Signature
    fun file(name: String): String? {
        return request.storePartFileAndGetRelativePath(name)
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

    /**
     * 转发请求，并返回响应
     *   因为是异步处理, 因此在action方法最后一行必须返回该函数的返回值
     * @param url
     * @param res
     * @param useHeaders 是否使用请求头
     * @param useCookies 是否使用cookie
     * @return 异步响应
     */
    @Reflection.Signature
    @JvmOverloads
    public fun transferAndReturn(url: String, res: PHttpResponse, useHeaders: Boolean = false, useCookies: Boolean = false): CompletableFuture<Response> {
        return request.transferAndReturn(url, res.response, useHeaders, useCookies)
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
         * 守护php方法调用
         *   http controller的方法不会像rpc service的方法那样符合guardInvoke要求（有明确类型的参数或响应）
         *   如多个参数不是在函数声明中指定的，而是在函数体获得的，如返回值不仅仅是业务对象，外面还包了{code, msg, data}一层来适应json响应规范，这样不符合 KeyCombine/GroupCombine 的要求
         *   因此，你可以在php action方法实现中，继续调用 HttpRequest::guardInvoke(其他方法)，以便符合guardInvoke要求
         */
        @Reflection.Signature
        @JvmStatic
        fun guardInvoke(env: Environment, obj: ObjectMemory, methodName: StringMemory, vararg args: Memory): Memory {
            return HttpRequestHandler.guardInvoke(obj, methodName, args as Array<Memory>, env)
        }
    }
}