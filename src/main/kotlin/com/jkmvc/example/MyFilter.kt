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
        // add route rule
        Router.addRoute("default",
                Route("<controller>(\\/<action>(\\/<id>)?)?", // url正则 | url pattern
                    mapOf("id" to "\\d+"), // 参数子正则 | param pattern
                    mapOf("controller" to "welcome", "action" to "index"))); // default param
        // 添加扫描controller的包
        // add package path to scan Controller
        ControllerLoader.addPackage("com.jkmvc.example.controller");
    }
}
