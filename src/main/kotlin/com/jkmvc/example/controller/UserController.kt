package com.jkmvc.example.controller

import com.jkmvc.http.Controller
import com.jkmvc.http.Request
import com.jkmvc.http.Response

class UserController(req: Request /* 请求对象 */, res: Response /* 响应对象 */): Controller(req, res) {

    public fun actionIndex(){

        res.render(view("index", mutableMapOf("items" to "shijianhang")))
    }

}