package com.jkmvc.http

import com.jkmvc.common.findConstructor
import com.jkmvc.common.lcFirst
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
     * 无参数的构造函数
     */
    public override val constructer: KFunction<*>
            get() = clazz.findConstructor()!!;

    /**
     * 所有action方法
     */
    public override val actions: MutableMap<String, KFunction<*>> = HashMap<String, KFunction<*>>();

    init{
        // 获得所有action方法
        for (func in clazz.memberFunctions) {
            if(func.name.startsWith("action")) {
                val name = func.name.substring(6).lcFirst() // 首字母小写
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