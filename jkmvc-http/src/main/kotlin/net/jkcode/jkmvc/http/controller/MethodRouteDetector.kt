package net.jkcode.jkmvc.http.controller

import net.jkcode.jkmvc.http.router.Route
import net.jkcode.jkmvc.http.router.Router
import java.lang.reflect.Method
import net.jkcode.jkmvc.http.router.ARoute

/**
 * 方法级路由的检测器
 *   主要是检测方法上的路由注解
 *
 *
 * @author shijianhang
 * @date 2016-10-6 上午12:01:17
 */
open class MethodRouteDetector {

    /**
     * 检测路由注解
     * @param name 路由名
     * @parma method 方法
     */
    public fun detect(controller:String, action: String, method: Method) {
        // 获得注解
        val annotation = method.getAnnotation(ARoute::class.java)
        if(annotation == null)
            return
        // 获得正则
        val regex = annotation.regex.trim()
        // 只有正则不为空才会添加路由, 否则都交给默认路由处理(默认路由最后需要校验方法)
        if(regex.isEmpty())
            return

        val route = Route(regex, annotation.method, controller, action)
        Router.addRoute("$controller#$action", route)
    }

}