package net.jkcode.jkmvc.http.controller

import co.paralleluniverse.fibers.Suspendable
import net.jkcode.jkguard.IMethodMeta
import net.jkcode.jkmvc.http.HttpRequest
import net.jkcode.jkmvc.http.HttpResponse
import net.jkcode.jkmvc.http.view.View
import net.jkcode.jkutil.collection.LazyAllocatedMap
import net.jkcode.jkutil.common.DegradeCommandException
import net.jkcode.jkutil.common.buildQueryString
import net.jkcode.jkutil.common.to
import net.jkcode.jkutil.common.trySupplierFuture
import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * 控制器
 *   子类必须要有默认构造函数, 即无参数构造函数
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 *
 */
abstract class Controller : IController {

    /**
     * 请求对象
     */
    override lateinit var req: HttpRequest

    /**
     * 响应对象
     */
    override lateinit var res: HttpResponse

    /**
     * 视图模型
     */
    override val vm:MutableMap<String, Any?> by lazy {
        LazyAllocatedMap<String, Any?>()
    }

    /**
     * 视图
     * @param file 视图文件
     * @param data 视图变量
     * @return 视图
     */
    public override fun view(file:String, data:Map<String, Any?>): View
    {
        return View(this.req, this.res, file, data);
    }

    /**
     * 视图
     * @param data 视图变量
     * @return 视图
     */
    public override fun view(data:Map<String, Any?>): View
    {
        return view(this.req.controller + "/" + this.req.action, data)
    }

    /**
     * 重定向到指定url
     * @param uri
     * @param data
     */
    public override fun redirect(uri: String, data:Map<String, Any?>)
    {
        var url = this.req.absoluteUrl(uri)
        var query = data.buildQueryString(true)
        if(query.isNotEmpty()){
            val delimiter = if(url.contains('?')) '&' else '?'
            url = url + delimiter + query
        }
        this.res.sendRedirect(url);
    }

    /**
     * 执行action方法
     * @param action action方法
     */
    @Suspendable
    public fun callActionMethod(action: IMethodMeta): CompletableFuture<Any?> {
        return trySupplierFuture {
            // 1 前置处理
            before()

            // 2 执行真正的处理方法
            action.invoke(this, buildActionParams(action))
            //}.whenComplete{ r, ex -> // 不转换结果, 还是会抛异常(如 DegradeCommandException, 不应该往上抛)
        }.handle{ r, ex -> // whenComplete() + 转换结果
            // 3 后置处理
            var result = r
            if(ex is DegradeCommandException) // 异常自带降级处理
                result = ex.handleFallback()
            else  // 后置处理
                result = after(result, ex)

            // 4 渲染结果
            renderResult(result)
        }
    }

    /**
     * 构建action方法的实参
     * @param action
     * @return
     */
    protected fun buildActionParams(action: IMethodMeta):Array<Any?>{
        // 只处理一个参数的情况，且该实参为路由参数id的值
        if(action.parameterTypes.size == 1){
            val id = req.routeParams["id"] // 参数值
            if(id != null) {
                val paramClass = action.parameterTypes.single() // 参数类型
                return arrayOf(id.to(paramClass)) // 转类型
            }
        }

        return emptyArray()
    }

    /**
     * 渲染结果
     */
    override fun renderResult(result: Any?) {
        if(result == null || result is Unit || result is Void // 无结果
                || res.rendered) // 渲染过
            return

        // 渲染视图或重定向
        if (result is String) {
            if(result.startsWith("redirect:")){ // 重定向
                var url = result.substringAfter("redirect:")
                // 相对url, 添加contextPath
                if(!url.startsWith("http:") && !url.startsWith("https:"))
                    url = req.contextPath + url
                res.sendRedirect(url)
            }else { // 渲染视图
                res.renderView(result, vm) // 渲染视图的场景比渲染文本的多, 因此直接渲染视图啦
            }

            return
        }

        // 渲染视图
        if (result is View) {
            result.mergeVm(vm)
            res.renderView(result)
            return
        }

        // 渲染文件
        if (result is File) {
            res.renderFile(result)
            return
        }

        // 渲染json
        res.renderJson(result)
    }
}