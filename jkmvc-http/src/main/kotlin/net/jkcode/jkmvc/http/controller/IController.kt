package net.jkcode.jkmvc.http.controller

import net.jkcode.jkmvc.http.HttpRequest
import net.jkcode.jkmvc.http.HttpResponse
import net.jkcode.jkmvc.http.view.View
import net.jkcode.jkutil.common.LazyAllocatedMap
import java.io.File
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.CompletableFuture

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
    fun redirect(uri: String):Unit

    /**
     * 执行action
     *   注意：为了区别业务action，该方法不能命名为callAction
     * @param action action方法
     */
    public fun callActionMethod(action: Method): Any? {
        // 前置处理
        before()

        // 执行真正的处理方法
        val result = action.invoke(this);

        // 后置处理
        after()

        // 渲染结果
        if(result is CompletableFuture<*>)
            return result.thenApply(::renderResult)

        renderResult(result)
        return result
    }

    /**
     * 渲染结果
     */
    fun renderResult(result: Any?) {
        if(result == null || result is Unit || result is Void // 无结果
                || res.rendered) // 渲染过
            return

        // 渲染视图
        if (result is View)
            res.renderView(result)

        // 渲染文本
        if (result is String)
            res.renderString(result)

        // 渲染文件
        if (result is File)
            res.renderFile(result)

        // 渲染json
        res.renderJson(result)
    }

    /**
     * 前置处理
     */
    fun before(){}

    /**
     * 后置处理
     */
    fun after(){}
}