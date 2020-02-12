package net.jkcode.jkmvc.http.controller

import net.jkcode.jkutil.ttl.SttlCurrentHolder
import net.jkcode.jkmvc.http.HttpRequest
import net.jkcode.jkmvc.http.HttpResponse
import net.jkcode.jkmvc.http.view.View
import net.jkcode.jkutil.ttl.HttpRequestScopedTransferableThreadLocal

/**
 * 控制器
 *   子类必须要有默认构造函数, 即无参数构造函数
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 *
 */
abstract class Controller : IController {

    companion object: SttlCurrentHolder<Controller>(HttpRequestScopedTransferableThreadLocal()) // http请求域的可传递的 ThreadLocal

    /**
     * 请求对象
     */
    override lateinit var req: HttpRequest

    /**
     * 响应对象
     */
    override lateinit var res: HttpResponse

    /**
     * for jkerp
     */
    val request: HttpRequest
        get() = req

    /**
     * for jkerp
     */
    val response: HttpResponse
        get() = res

    init {
        setCurrent(this)
    }

    /**
     * 视图
     * @param file 视图文件
     * @param data 视图变量
     * @return 视图
     */
    public override fun view(file:String, data:Map<String, Any?>): View
    {
        return View(req, res, file, data);
    }

    /**
     * 视图
     * @param data 视图变量
     * @return 视图
     */
    public override fun view(data:Map<String, Any?>): View
    {
        return view(req.controller + "/" + req.action, data)
    }

    /**
     * 重定向到指定url
     * @param uri
     */
    public override fun redirect(uri: String)
    {
        res.sendRedirect(req.absoluteUrl(uri));
    }
}