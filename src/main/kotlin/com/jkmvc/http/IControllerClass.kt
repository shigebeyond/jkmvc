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
     * 构造函数
     */
    val constructer: KFunction<*>;

    /**
     * 所有action方法
     */
    val actions: MutableMap<String, KFunction<*>>;

    /**
     * 获得action方法
     */
    fun getActionMethod(name:String): KFunction<*>?;
}