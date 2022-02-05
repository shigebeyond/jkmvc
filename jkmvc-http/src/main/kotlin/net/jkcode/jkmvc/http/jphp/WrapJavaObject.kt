package net.jkcode.jkmvc.http.jphp

import net.jkcode.jkutil.common.getAccessibleField
import net.jkcode.jkutil.common.getMethodByName
import php.runtime.Memory
import php.runtime.annotation.Reflection
import php.runtime.env.Environment
import php.runtime.env.TraceInfo
import php.runtime.ext.java.JavaClass
import php.runtime.ext.java.JavaMethod
import php.runtime.ext.java.JavaObject
import php.runtime.ext.java.JavaReflection
import php.runtime.lang.BaseWrapper
import php.runtime.memory.ObjectMemory
import php.runtime.memory.StringMemory
import php.runtime.memory.support.MemoryUtils
import php.runtime.memory.support.MemoryUtils.Converter
import php.runtime.reflection.ClassEntity

/**
 * 包装java对象，用来读写属性+动态调用方法
 *    仿jphp自带的 JavaObject，但该类并不能动态调用方法
 *    动态调用方法的实现，使用魔术方法，涉及到参数类型+返回值类型转换
 *    java中的实例化： val obj = WrapJavaObject.of(env, xxx)
 *    php中的实例化: $obj = new WrapJavaObject($xxx);
 */
@Reflection.Name("php\\lang\\WrapJavaObject")
class WrapJavaObject(env: Environment, clazz: ClassEntity) : BaseWrapper<JavaObject>(env, clazz) {

    lateinit var obj: Any

    @Reflection.Signature
    fun __construct(obj: Any): Memory {
        this.obj = obj
        return Memory.NULL
    }

    //__call()实现一： wrong, 不严谨，因为jphp自动转换的实参类型（如数字会转为Long），可能对不上方法的形参类型(如int)
    /*@Reflection.Signature
    fun __call(name: String, vararg args: Any?): Any? {
        try {
            // 获得方法
            val method = obj.javaClass.getMethodByName(name)
            if(method == null)
                throw NoSuchMethodException("类[${obj.javaClass}]无方法[$name]")
            // 调用方法
            return method.invoke(obj, *args)
        } catch (e: Exception) {
            JavaReflection.exception(env, e)
        }
        return Memory.NULL
    }*/

    //__call()实现二： 使用 JavaMethod 包装方法调用
    @Reflection.Signature(value = [Reflection.Arg("name"), Reflection.Arg("arguments")])
    fun __call(env: Environment, vararg args: Memory): Memory {
        try {
            // 第一个参数是方法名
            val name = args[0].toString()
            // 获得方法
            val method = obj.javaClass.getMethodByName(name)
            // 用 JavaMethod 包装方法调用
            val method2 = JavaMethod.of(env, method)
            val args2 = args.toMutableList()
            args2[0] = ObjectMemory(JavaObject.of(env, obj)) // 第一个参数，原来是方法名，现替换为被包装的java对象
            return method2.invokeArgs(env, *args2.toTypedArray())
        } catch (e: Exception) {
            JavaReflection.exception(env, e)
        }
        return Memory.NULL
    }

    // ---------------- 抄final类JavaObject实现 ---------------
    @Reflection.Signature(Reflection.Arg("name"))
    fun __get(env: Environment, vararg args: Memory): Memory {
        val name = args[0].toString()
        try {
            val field = obj.javaClass.getField(name)
            field.isAccessible = true
            return MemoryUtils.valueOf(env, field[obj])
        } catch (e: NoSuchFieldException) {
            JavaReflection.exception(env, e)
        } catch (e: IllegalAccessException) {
            JavaReflection.exception(env, e)
        }
        return Memory.NULL
    }

    @Reflection.Signature(value = [Reflection.Arg("name"), Reflection.Arg("value")])
    fun __set(env: Environment, vararg args: Memory): Memory {
        val name = args[0].toString()
        try {
            val field = obj.javaClass.getField(name)
            field.isAccessible = true
            field[obj] = MemoryUtils.fromMemory(args[1], field.type)
        } catch (e: NoSuchFieldException) {
            JavaReflection.exception(env, e)
        } catch (e: IllegalAccessException) {
            JavaReflection.exception(env, e)
        }
        return Memory.NULL
    }

    @Reflection.Name("getClass")
    @Reflection.Signature
    fun _getClass(env: Environment, vararg args: Memory): Memory {
        return ObjectMemory(JavaClass.of(env, obj.javaClass))
    }

    @Reflection.Signature
    fun getClassName(env: Environment, vararg args: Memory): Memory {
        return StringMemory(obj.javaClass.getName())
    }

    companion object {
        protected val CONVERTERS: HashMap<Class<*>, Converter<*>> = MemoryUtils::class.java.getAccessibleField("CONVERTERS")!!.get(null) as HashMap<Class<*>, Converter<*>>

        init {
            // Memory 转 java object
            // 添加 object 类型的转换器，否则由于找不到 object 类型的转换器导致直接将实参值转换为null, 如 Hashmap 的 put(Object key, Object value) 方法, 在php调用 map.put('price', 11)时到java就变成 map.put(null, null)
            CONVERTERS.put(Any::class.java, object : MemoryUtils.Converter<Any?>() {
                override fun run(env: Environment?, trace: TraceInfo?, value: Memory): Any? {
                    return value.toJavaObject()
                }
            })
        }

        // 创建 WrapJavaObject 实例
        fun of(env: Environment, value: Any): WrapJavaObject {
            val javaObject = WrapJavaObject(env, env.fetchClass("php\\lang\\WrapJavaObject"))
            javaObject.obj = value
            return javaObject
        }


    }
}