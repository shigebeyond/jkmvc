package com.jkmvc.common

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.jvm.javaType

/****************************** 反射扩展 *******************************/
/**
 * 匹配方法的名称与参数类型
 * @param name 方法名
 * @param paramTypes 参数类型
 * @return
 */
public fun KFunction<*>.matches(name:String, paramTypes:List<Class<*>> = emptyList()):Boolean{
    // 1 匹配名称
    if(name != this.name)
        return false

    // 2 匹配参数
    // 2.1 匹配参数个数
    if(paramTypes.size != this.parameters.size)
        return false;

    // 2.2 匹配参数类型
    for (i in paramTypes.indices){
        var targetType = this.parameters[i].type.javaType;
        if(targetType is ParameterizedTypeImpl) // 若是泛型类型，则去掉泛型，只保留原始类型
            targetType = targetType.rawType;

        if(paramTypes[i] != targetType)
            return false
    }

    return true;
}

/**
 * 查找方法
 * @param name 方法名
 * @param paramTypes 参数类型
 * @return
 */
public fun KClass<*>.findFunction(name:String, paramTypes:List<Class<*>> = emptyList()): KFunction<*>?{
    var pt = if(paramTypes is MutableList){
        paramTypes
    }else{
        LinkedList<Class<*>>(paramTypes);
    }

    // 第一个参数为this
    pt.add(0, this.java);

    return memberFunctions.find {
        it.matches(name, pt);
    }
}

/**
 * 查找静态方法
 * @param name 方法名
 * @param paramTypes 参数类型
 * @return
 */
public fun KClass<*>.findStaticFunction(name:String, paramTypes:List<Class<*>> = emptyList()): KFunction<*>?{
    return staticFunctions.find {
        it.matches(name, paramTypes);
    }
}

/**
 * 查找构造函数
 * @param paramTypes 参数类型
 * @return
 */
public fun KClass<*>.findConstructor(paramTypes:List<Class<*>> = emptyList()): KFunction<*>?{
    return constructors.find {
        it.matches("<init>", paramTypes); // 构造函数的名称为 <init>
    }
}

/**
 * 查找属性
 * @param name 属性名
 * @return
 */
public fun KClass<*>.findProperty(name:String): KProperty1<*, *>?{
    return this.declaredMemberProperties.find {
        it.name == name;
    }
}

/**
 * 转换参数类型
 * @param value
 * @return
 */
public inline fun KParameter.convert(value: String): Any {
    return value.to(this.type)
}