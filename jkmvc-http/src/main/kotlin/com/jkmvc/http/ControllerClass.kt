package com.jkmvc.http

import com.jkmvc.common.findConstructor
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.memberFunctions

/**
 * 封装Controller类
 *   方便访问其构造函数与所有的action方法
 * Created by shi on 4/26/17.
 */
class ControllerClass(public override val clazz: KClass<*> /* controller类 */):IControllerClass{

    /**
     * 所有action方法
     */
    public override val actions: MutableMap<String, KFunction<*>> = HashMap();

    init{
        // 检查默认构造函数
        if(clazz.findConstructor() == null)
            throw RouteException("Controller类${clazz}无默认构造函数")

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
                val name = funname.substring(0, funname.length - 6) // 去掉Action结尾
                actions[name] = func;
            }
        }
    }

    /**
     * 获得action方法
     */
    public override fun getActionMethod(name:String): KFunction<*>? {
        return actions.get(name);
    }
}