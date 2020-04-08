package net.jkcode.jkmvc.http.controller

import net.jkcode.jkmvc.http.router.Route
import net.jkcode.jkmvc.http.router.Router
import java.lang.reflect.Method
import net.jkcode.jkmvc.http.router.ARoute

/**
 * 方法级路由的检测器
 *   主要是检测方法上的路由注解
 *   for jkerp
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
        val annotation = method.getAnnotation(ARoute::class.java)
        if(annotation == null)
            return

        val params = mapOf(
                "controller" to controller,
                "action" to action
        )
        Router.addRoute("$controller#$action", Route(annotation.regex, emptyMap(), params, annotation.method))
    }

}