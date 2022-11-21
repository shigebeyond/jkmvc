package net.jkcode.jkmvc.example.controller

import net.jkcode.jkmvc.http.controller.Controller
import org.asynchttpclient.Response
import java.util.concurrent.CompletableFuture

/**
 * 主页
 */
class WelcomeController: Controller() {

    /**
     * 主页
     */
    public fun index() {
        val content = "hello world<br/><a href=\"" + req.absoluteUrl("user/index") + "\">用户管理</a>"
        res.renderHtml(content);
    }

    /**
     * 显示jsp视图
     * render jsp view
     */
    public fun jsp(){
        res.renderView(view("index" /* view file */, mapOf("name" to "shijianhang") /* view data */))
    }

    /**
     * 测试转发请求
     *   http://localhost:8080/jkmvc-example/welcome/transfer
     */
    public fun transfer(): CompletableFuture<Response> {
        // return transferAndReturn("http://www.baidu.com/s?wd=jkmvc")
        return transferAndReturn("https://search.gitee.com/?skin=rec&type=repository&q=jkmvc")
    }

}