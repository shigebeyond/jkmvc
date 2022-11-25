package net.jkcode.jkmvc.http.controller

import net.jkcode.jkmvc.http.HttpRequest
import net.jkcode.jkmvc.http.HttpResponse
import net.jkcode.jkmvc.http.view.View
import net.jkcode.jkutil.collection.LazyAllocatedMap
import net.jkcode.jkutil.common.ColorFormatter
import net.jkcode.jkutil.common.errorColor
import net.jkcode.jkutil.common.httpLogger
import org.asynchttpclient.Response
import java.io.Writer
import java.util.concurrent.CompletableFuture
import javax.servlet.ServletOutputStream

/**
 * 控制器
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 *
 */
interface IController{

    /**
     * 请求对象
     */
    var req: HttpRequest

    /**
     * 响应对象
     */
    var res: HttpResponse

    /**
     * 响应的writer
     */
    val writer: Writer
        get() = res.prepareWriter()

    /**
     * 响应的output
     */
    val out: ServletOutputStream
        get() = res.outputStream

    /**
     * 视图模型
     */
    val vm:MutableMap<String, Any?>

    /**
     * 视图
     * @param file 视图文件
     * @param data 视图变量
     * @return 视图
     */
    fun view(file:String, data:Map<String, Any?> = LazyAllocatedMap()): View

    /**
     * 视图
     * @param data 视图变量
     * @return 视图
     */
    fun view(data:Map<String, Any?> = LazyAllocatedMap()): View

    /**
     * 重定向到指定url
     * @param uri
     */
    fun redirect(uri: String, data:Map<String, Any?> = emptyMap())


    /**
     * 渲染结果
     */
    fun renderResult(result: Any?)

    /**
     * 前置处理
     */
    fun before(){}

    /**
     * 后置处理
     *   因为这是请求的最后处理(包含异常处理), 你最好不要再往上抛异常
     *
     * @param result action方法执行结果
     * @param t action方法执行抛出的异常
     * @return
     */
    fun after(result: Any?, t: Throwable? = null): Any? {
        // 处理异常
        if(t != null){
            // 日志
            val msg = "处理请求[${req.routeUri}]出错: ${t.message}"
            httpLogger.errorColor(msg, t)
            // 输出500错误
            res.sendError(500, msg)
            return null // 返回null后, res不再渲染
        }

        // 处理结果
        return result
    }


    /**
     * 转发请求，并返回响应
     *   因为是异步处理, 因此在action方法最后一行必须返回该函数的返回值
     * @param url
     * @param useHeaders 是否使用请求头
     * @param useCookies 是否使用cookie
     * @return 异步响应
     */
    fun transferAndReturn(url: String, useHeaders: Boolean = false, useCookies: Boolean = false): CompletableFuture<Response>
}