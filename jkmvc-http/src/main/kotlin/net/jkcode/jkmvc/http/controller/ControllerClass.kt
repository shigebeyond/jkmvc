package net.jkcode.jkmvc.http.controller

import net.jkcode.jkmvc.http.router.Route
import net.jkcode.jkutil.common.getConstructorOrNull
import net.jkcode.jkutil.common.lcFirst
import net.jkcode.jkmvc.http.router.RouteException
import net.jkcode.jkmvc.http.router.Router
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import net.jkcode.jkmvc.http.router.annotation.Route as ARoute

/**
 * 封装Controller类
 *   方便访问其构造函数与所有的action方法
 * Created by shi on 4/26/17.
 */
class ControllerClass(public override val clazz: KClass<*> /* controller类 */): IControllerClass {

    /**
     * 根据类名获得controller名
     */
    public val name: String by lazy{
        val name = clazz.simpleName!!
        val end = name.length - 10
        name.substring(0, end).lcFirst() /* 首字母小写 */
    }

    /**
     * 所有action方法
     */
    public override val actions: MutableMap<String, KFunction<*>> = HashMap();

    init{
        // 检查默认构造函数
        if(clazz.java.getConstructorOrNull() == null)
            throw RouteException("Class [${clazz}] has no no-arg constructor") // Controller类${clazz}无默认构造函数

        // 解析所有action方法
        parseActionMethods()
    }

    /**
     * 解析所有action方法
     */
    private fun parseActionMethods() {
        for (func in clazz.memberFunctions) {
            val funname = func.name
            if (funname.endsWith("Action") || func.parameters.isEmpty()) { // 以Action结尾 + 无参数
                // action名: 去掉Action结尾
                val action = funname.substring(0, funname.length - 6)
                // 缓存action
                actions[action] = func;
                // 解析路由注解
                val annotation = func.findAnnotation<ARoute>()
                if(annotation != null)
                    Router.addRoute(name, action, annotation)
            }
        }
    }

}