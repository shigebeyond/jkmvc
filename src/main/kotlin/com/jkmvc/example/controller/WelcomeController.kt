package com.jkmvc.example.controller

import com.jkmvc.http.Controller
import com.jkmvc.http.Request
import com.jkmvc.http.Response
import com.jkmvc.http.View

/**
 * 主页
 */
class WelcomeController: Controller() {

    /**
     * 主页
     */
    public fun actionIndex() {
        res.render("hello world");
    }

    /**
     * 显示jsp视图
     */
    public fun actionJsp(){
        res.render(view("index", mutableMapOf("name" to "shijianhang")))
    }

}