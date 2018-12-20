package com.jkmvc.common

import org.nustaq.serialization.util.FSTUtil
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.lang.reflect.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.jvm.javaType

/**
 * 强制调用克隆方法
 */
public fun Any.forceClone():Any {
    if(!(this is Cloneable))
        throw IllegalArgumentException("非Cloneable对象，不能调用clone()方法")

    val f:KFunction<*> = javaClass.kotlin.getFunction("clone")!!
    return f.call(this) as Any;
}

/**
 * 获得指定类型的默认值
 * @return
 */
public inline val <T: Any> KClass<T>.defaultValue:T
    get(){
        return when (this) {
            Int::class -> 0
            Long::class -> 0L
            Float::class -> 0.0
            Double::class -> 0.0
            Boolean::class -> false
            Short::class -> 0
            Byte::class -> 0
            else -> null
        } as T
    }

/****************************** kotlin反射扩展: KClass *******************************/
/**
 * 匹配方法的名称与参数类型
 * @param name 方法名
 * @param paramTypes 参数类型
 * @return
 */
public fun KFunction<*>.matches(name:String, paramTypes:Array<out Class<*>>):Boolean{
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
public fun KClass<*>.getFunction(name:String, vararg paramTypes:Class<*>): KFunction<*>?{
    // 第一个参数为this
    val pt = toArray(this.java, *paramTypes)
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
public fun KClass<*>.getStaticFunction(name:String, vararg paramTypes:Class<*>): KFunction<*>?{
    return staticFunctions.find {
        it.matches(name, paramTypes);
    }
}

/**
 * 查找构造函数
 * @param paramTypes 参数类型
 * @return
 */
public fun KClass<*>.getConstructor(vararg paramTypes:Class<*>): KFunction<*>?{
    return constructors.find {
        it.matches("<init>", paramTypes); // 构造函数的名称为 <init>
    }
}

/**
 * 查找属性
 * @param name 属性名
 * @return
 */
public fun KClass<*>.getProperty(name:String): KProperty1<*, *>?{
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

/****************************** java反射扩展: Class *******************************/
/**
 * 是否静态方法
 */
public val Method.isStatic: Boolean
        get() = Modifier.isStatic(modifiers)

/**
 * 是否抽象类
 */
public val  <T> Class<T>.isAbstract: Boolean
    get() =  Modifier.isAbstract(modifiers)

/**
 * 检查当前类 是否是 指定类的子类
 *
 * @param superClass 父类
 * @return
 */
public fun Class<*>.isSubClass(superClass: Class<*>): Boolean {
    return this != superClass && superClass.isAssignableFrom(this)
}

/**
 * 检查当前类 是否是 指定类的父类
 *
 * @param subClass 子类
 * @return
 */
public fun Class<*>.isSuperClass(subClass: Class<*>): Boolean {
    return this != subClass && this.isAssignableFrom(subClass)
}

/**
 * 获得方法签名
 * @return
 */
public fun Method.getSignature(): String {
    return this.parameterTypes.joinTo(StringBuilder(this.name), ",", "(", ")"){
        it.name
    }.toString().replace("java.lang.", "")
}

/**
 * 获得当前类的方法哈系: <方法签名 to 方法>
 * @return
 */
public fun Class<*>.getMethodMaps(): Map<String, Method> {
    return methods.associate {
        it.getSignature() to it
    }
}

/**
 * 创建类的实例
 *   参考 FSTDefaultClassInstantiator#newInstance()
 *
 * @param initRequired 是否需要初始化, 即调用类自身的构造函数
 * @return
 */
public fun <T: Any> KClass<T>.newInstance(initRequired: Boolean = true): Any? {
    // 无[无参数构造函数]
    if(java.getConstructor() == null && !initRequired){
        // best effort. use Unsafe to instantiate.
        // Warning: if class contains transient fields which have default values assigned ('transient int x = 3'),
        // those will not be assigned after deserialization as unsafe instantiation does not execute any default
        // construction code.
        // Define a public no-arg constructor to avoid this behaviour (rarely an issue, but there are cases).
        if (FSTUtil.unFlaggedUnsafe != null)
            return FSTUtil.unFlaggedUnsafe.allocateInstance(java)

        throw RuntimeException("no suitable constructor found and no Unsafe instance avaiable. Can't instantiate " + this)
    }

    return java.newInstance()
}