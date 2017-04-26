package com.jkmvc.http

import com.jkmvc.common.findConstructor
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.memberFunctions

/**
 * 封装Controller类
 *   方便访问其构造函数与所有的action方法
 * Created by shi on 4/26/17.
 */
class ControllerClass(public val clazz: KClass<*> /* controller类 */){

    /**
     * 构造函数
     */
    public val constructer: KFunction<*> = clazz.findConstructor(listOf(Request::class.java, Response::class.java))!!;

    /**
     * 所有action方法
     */
    public val actions: MutableMap<String, KFunction<*>> = HashMap<String, KFunction<*>>();

    init{
        // 获得所有action方法
        for (func in clazz.memberFunctions) {
            if(func.name.startsWith("action_")) {
                actions[func.name.substring(7)] = func;
            }
        }
    }

    /**
     * 获得action方法
     */
    public fun getActionMethod(name:String): KFunction<*>? {
        return actions.get(name);
    }
}