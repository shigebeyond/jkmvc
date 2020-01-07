package net.jkcode.jkmvc.http.router.annotation

import net.jkcode.jkmvc.http.router.HttpMethod
import net.jkcode.jkmvc.http.router.IRouter
import net.jkcode.jkutil.common.getCachedAnnotation
import java.lang.reflect.Method

/**
 * 路由注解
 * @author shijianhang<772910474@qq.com>
 * @date 2020-1-6 6:04 PM
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Route(
     public val regex: String, // url正则
     public val method: HttpMethod = HttpMethod.ALL // http方法
)

/**
 * 获得路由注解
 */
public val Method.route: Route?
    get(){
        return getCachedAnnotation()
    }