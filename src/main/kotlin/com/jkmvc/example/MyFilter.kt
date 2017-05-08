package com.jkmvc.example

import com.jkmvc.http.ControllerLoader
import com.jkmvc.http.JkFilter
import com.jkmvc.http.Route
import com.jkmvc.http.Router
import javax.servlet.FilterConfig

class MyFilter: JkFilter() {

    override fun init(filterConfig: FilterConfig) {
        super.init(filterConfig);
        // 添加路由规则
        Router.addRoute("default",
                Route("<controller>(\\/<action>(\\/<id>)?)?", // url正则
                    mapOf("id" to "\\d+"), // 参数子正则
                    mapOf("controller" to "welcome", "action" to "index"))); //
        // 添加controller的扫描的包
        ControllerLoader.addPackage("com.jkmvc.example");
    }
}
