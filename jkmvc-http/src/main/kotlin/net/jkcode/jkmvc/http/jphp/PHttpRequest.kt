package net.jkcode.jkmvc.http.jphp

import net.jkcode.jkmvc.http.*
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
class PHttpRequest(env: Environment, public val req: HttpRequest) : BaseObject(env) {

    @Reflection.Signature
    protected fun __construct() {
    }

    @Reflection.Signature
    fun header(name: String): String {
        return req.getHeader(name)
    }

    @Reflection.Signature
    @JvmOverloads
    fun param(name: String, valueAsArray: Boolean = false): Any? {
        val value = req.getParameterValues(name)
        if(valueAsArray)
            return value
        return value?.firstOrNull()
    }

    /**
     * 获得所有参数
     * @param valueAsArray 值是否作为数组
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun params(valueAsArray: Boolean = false): Map<String, Any?> {
        if(valueAsArray)
            return req.parameterMap

        return HttpParamMap(req.parameterMap)
    }

    @Reflection.Signature
    fun query(): String {
        return req.queryString
    }

    /**
     * 合并多个参数为对象
     *    值是数组的多个参数, 转对象数组
     * @param names 参数名数组, 必须保证所有参数值的数组长度都一致
     */
    @Reflection.Signature
    fun combineParams(names: Array<String>): List<Map<String, Any?>>{
        return req.combineParams(names)
    }

    /**
     * 将参数名以 namePrefix 为前缀的参数合并为对象 -- 一维参数转多维对象
     *    如 fields[0][name]=a&fields[0][type]=int(10) unsigned&fields[0][default]=0&fields[0][comment]=a&fields[0][is_null]=NOT NULL
     *    合并转为对象 [{"name":"a","type":"int(10) unsigned","default":"0","comment":"a","is_null":"NOT NULL"}]
     * @param namePrefix 参数名前缀
     * @return
     */
    @Reflection.Signature
    public fun params2Object(namePrefix: String): Map<String, Any?> {
        return req.params2Object(namePrefix)
    }

    @Reflection.Signature
    fun uri(): String {
        return req.requestURI
    }

    @Reflection.Signature
    fun routeUri(): String {
        return req.routeUri
    }

    @Reflection.Signature
    fun method(): String {
        return req.method
    }

    @Reflection.Signature
    fun sessionId(): String {
        return req.getSession(true).id
    }

    /**
     * 设置上传的子目录(上传文件要存的子目录)
     *   要在调用file()之前设置
     */
    @Reflection.Signature
    fun setUploadSubDir(uploadSubDir: String) {
        req.uploadSubDir = uploadSubDir
    }

    /**
     * 保存上传文件, 并返回相对路径
     */
    @Reflection.Signature
    fun file(name: String): String? {
        return req.storePartFileAndGetRelativePath(name)
    }

    @Reflection.Signature
    fun controller(): String {
        //去掉$开头
        return req.controller.removePrefix("$")
    }

    @Reflection.Signature
    fun action(): String {
        return req.action
    }

    /**
     * 是否内部请求: INCLUDE/FORWARD
     * @return
     */
    @Reflection.Signature
    public fun isInner(): Boolean{
        return req.isInner
    }

    /**
     * 是否post请求
     * @return
     */
    @Reflection.Signature
    public fun isPost(): Boolean{
        return req.isPost
    }

    /**
     * 是否option请求
     * @return
     */
    @Reflection.Signature
    public fun isOptions(): Boolean{
        return req.isOptions
    }

    /**
     * 是否get请求
     * @return
     */
    @Reflection.Signature
    public fun isGet(): Boolean{
        return req.isGet
    }

    /**
     * 是否 multipart 请求
     * @return
     */
    @Reflection.Signature
    public fun isMultipartContent(): Boolean{
        return req.isMultipartContent
    }

    /**
     * 是否上传文件的请求
     * @return
     */
    @Reflection.Signature
    public fun isUpload(): Boolean{
        return req.isUpload
    }

    /**
     * 是否ajax请求
     * @return
     */
    @Reflection.Signature
    public fun isAjax(): Boolean{
        return req.isAjax
    }

    /**
     * 使用 http client 转发请求
     * @param url
     * @param useHeaders 是否使用请求头
     * @param useCookies 是否使用cookie
     * @return 异步响应
     */
    @Reflection.Signature
    @JvmOverloads
    public fun transfer(url: String, useHeaders: Boolean = false, useCookies: Boolean = false): CompletableFuture<String> {
        return req.transfer(url, useHeaders, useCookies).thenApply { r ->
            r.responseBody
        }
    }

    /**
     * 转发请求，并返回响应
     *   因为是异步处理, 因此在action方法最后一行必须返回该函数的返回值
     * @param url
     * @param res
     * @param useHeaders 是否使用请求头
     * @param useCookies 是否使用cookie
     * @return 异步结果
     */
    @Reflection.Signature
    @JvmOverloads
    public fun transferAndReturn(url: String, res: PHttpResponse, useHeaders: Boolean = false, useCookies: Boolean = false): CompletableFuture<Any?> {
        return req.transferAndReturn(url, res.res, useHeaders, useCookies).thenApply {
            // 不返回org.asynchttpclient.Response对象, 因为jphp无法转换
            // 直接返回null
            null
        }
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
         *   1 php action中调用
         *   http controller的方法不会像rpc service的方法那样符合guardInvoke要求（有明确类型的参数或响应）
         *   如多个参数不是在函数声明中指定的，而是在函数体获得的，如返回值不仅仅是业务对象，外面还包了{code, msg, data}一层来适应json响应规范，这样不符合 KeyCombine/GroupCombine 的要求
         *   因此，你可以在php action方法实现中，继续调用 HttpRequest::guardInvoke(其他方法)，以便符合guardInvoke要求
         *   2 关于返回值: PhpMethodMeta#getResultFromFuture() 中同步结果值有可能是null或java对象, 异步结果是PhpReturnCompletableFuture
         */
        @Reflection.Signature
        @JvmStatic
        fun guardInvoke(env: Environment, obj: ObjectMemory, methodName: StringMemory, vararg args: Memory): Any? {
            return HttpRequestHandler.guardInvoke(obj, methodName, args as Array<Memory>, env)
        }
    }
}