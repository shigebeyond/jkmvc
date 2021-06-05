package net.jkcode.jkmvc.example.controller

import net.jkcode.jkmvc.http.controller.Controller

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

}