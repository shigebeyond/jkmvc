package net.jkcode.jkmvc.http.controller

import net.jkcode.jkmvc.http.HttpRequest
import net.jkcode.jkmvc.http.HttpResponse
import net.jkcode.jkmvc.http.view.View
import net.jkcode.jkutil.collection.LazyAllocatedMap
import net.jkcode.jkutil.common.DegradeCommandException
import net.jkcode.jkutil.common.trySupplierFuture
import java.io.Writer
import java.lang.reflect.Method
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
    fun redirect(uri: String)

    /**
     * 执行action
     *   注意：为了区别业务action，该方法不能命名为callAction
     * @param action action方法
     */
    public fun callActionMethod(action: Method): CompletableFuture<Any?> {
        return trySupplierFuture {
                // 1 前置处理
                before()

                // 2 执行真正的处理方法
                action.invoke(this);
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
            t.printStackTrace()
            return null
        }

        // 处理结果
        return result
    }
}