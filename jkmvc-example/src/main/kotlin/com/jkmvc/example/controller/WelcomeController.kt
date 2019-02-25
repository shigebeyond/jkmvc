package net.jkcode.jkmvc.example.controller

import net.jkcode.jkmvc.http.controller.Controller

/**
 * 主页
 */
class WelcomeController: Controller() {

    /**
     * 主页
     */
    public fun indexAction() {
        res.renderString("hello world");
    }

    /**
     * 显示jsp视图
     * render jsp view
     */
    public fun jspAction(){
        res.renderView(view("index" /* view file */, mutableMapOf("name" to "shijianhang") /* view data */))
    }

}