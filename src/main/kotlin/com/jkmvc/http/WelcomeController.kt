package com.jkmvc.http

class WelcomeController(req: Request /* 请求对象 */, res: Response /* 响应对象 */): Controller(req, res) {

    public fun actionIndex(){
        res.render("hello world");
    }

}