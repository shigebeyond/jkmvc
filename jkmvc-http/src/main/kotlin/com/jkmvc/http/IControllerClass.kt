package com.jkmvc.http

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * 封装Controller类
 *   方便访问其构造函数与所有的action方法
 * Created by shi on 4/26/17.
 */
interface IControllerClass{

    /**
     * controller类
     */
    val clazz: KClass<*>;

    /**
     * 所有action方法
     */
    val actions: Map<String, KFunction<*>>;

    /**
     * 获得action方法
     * @return
     */
    fun getActionMethod(name:String): KFunction<*>? {
        return actions.get(name);
    }
}