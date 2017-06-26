package com.jkmvc.example

import com.jkmvc.db.Db
import com.jkmvc.db.DruidDataSourceFactory
import com.jkmvc.http.ControllerLoader
import com.jkmvc.http.JkFilter
import com.jkmvc.http.Route
import com.jkmvc.http.Router
import javax.servlet.FilterConfig

class MyFilter: JkFilter() {

    override fun init(filterConfig: FilterConfig) {
        super.init(filterConfig);
        // 指定数据源工厂，需要 IDataSourceFactory 对象，可自定义实现类，但需自己实现根据数据源名来缓存创建好的数据源
        // set dataSourceFactory for Db, it wants IDataSourceFactory's object, you can implements your subclass, but you need to cache dataSource by a name
        Db.dataSourceFactory = DruidDataSourceFactory;
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
