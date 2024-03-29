package net.jkcode.jkmvc.http.jphp

import net.jkcode.jkmvc.http.HttpRequest
import net.jkcode.jkmvc.http.HttpResponse
import net.jkcode.jkmvc.http.view.View
import net.jkcode.jphp.ext.JphpLauncher

/**
 * php模板视图
 *     放在web根目录下的html文件
 *
 * @author shijianhang<772910474@qq.com>
 * @date 8/25/17 9:49 AM
 */
class PhpView(req: HttpRequest /* 请求对象 */, res: HttpResponse /* 响应对象 */, file:String/* 视图文件 */, vm: MutableMap<String, Any?> /* 视图模型 */): View(req, res, file, vm) {

    /**
     * 渲染php模板
     */
    override fun render() {
        JphpLauncher.run(path + ".php", vm, res.outputStream)
    }

}