package net.jkcode.jkmvc.http.controller

import net.jkcode.jkmvc.http.router.RouteException
import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.getConstructorOrNull
import net.jkcode.jkutil.common.getSignature
import net.jkcode.jkutil.common.lcFirst
import java.lang.Throwable
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.javaMethod

/**
 * 封装Controller类
 *   方便访问其构造函数与所有的action方法
 * Created by shi on 4/26/17.
 */
class ControllerClass(public override val clazz: KClass<*> /* controller类 */): IControllerClass {

    companion object{

        /**
         * http配置
         */
        public val config = Config.instance("http", "yaml")

        /**
         * 方法级路由的检测器
         *    可通过自定义检测器, 来实现丰富的路由注解与处理
         *
         */
        public val methodRoutDetector by lazy {
            val clazz = config.get("methodRouteDetectorClass", "net.jkcode.jkmvc.http.controller.MethodRouteDetector")
            Class.forName(clazz).newInstance() as MethodRouteDetector
        }

        /**
         * 忽略的方法
         */
        public val ignoreMethods: Array<String> = arrayOf(
                "before()",
                "after()",
                "hashCode()",
                "toString()",
                "equals(Object)"
        )

    }

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
    public override val actions: MutableMap<String, Method> = HashMap();

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
            val method = func.javaMethod!!
            // 忽略方法
            val methodSignature = method.getSignature()
            if(ignoreMethods.contains(methodSignature))
                continue

            // 过滤action方法: public + 无参数
            if (Modifier.isPublic(method.modifiers)
                    && method.parameterCount == 0) {
                // action名 = 方法名
                val action = method.name
                // 缓存action
                actions[action] = method;
                // 检测路由注解
                methodRoutDetector.detect(name, action, method)
            }
        }
    }

}