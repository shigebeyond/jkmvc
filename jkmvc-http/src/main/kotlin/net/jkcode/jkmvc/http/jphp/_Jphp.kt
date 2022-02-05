package net.jkcode.jkmvc.http.jphp

import net.jkcode.jkutil.common.getAccessibleField
import php.runtime.Memory
import php.runtime.lang.IObject
import php.runtime.memory.*

/**
 * 将java对象转换为jphp的Memory
 * @return
 */
public inline fun Any?.toMemory(): Memory {
    return when(this) {
        null -> NullMemory.INSTANCE
        is Boolean -> if(this) TrueMemory.INSTANCE else FalseMemory.INSTANCE
        is Int -> LongMemory(this.toLong())
        is Long -> LongMemory(this)
        is Short -> LongMemory(this.toLong())
        is Byte -> LongMemory(this.toLong())
        is Float -> DoubleMemory(this.toDouble())
        is Double -> DoubleMemory(this)
        is String -> StringMemory(this)
        is String -> StringMemory(this)
        is List<*> -> ArrayMemory(this)
        is Map<*, *> -> ArrayMemory(this)
        is IObject -> ObjectMemory(this)
        is WrapJavaObject -> ObjectMemory(this)
        else -> throw IllegalArgumentException("Cannot auto convert [$this] into Memory")
    }
}

val mapField = ArrayMemory::class.java.getAccessibleField("map")!!
val listField = ArrayMemory::class.java.getAccessibleField("_list")!!

/**
 * 将jphp的Memory转换为java对象
 * @return
 */
public fun Memory.toJavaObject(): Any? {
    return when(this) {
        is NullMemory -> null
        is TrueMemory -> true
        is FalseMemory -> false
        is LongMemory -> this.value
        is DoubleMemory -> this.toDouble()
        is StringMemory -> this.toString()
        is ArrayMemory -> if(this.isMap()) mapField.get(this) else listField.get(this)
        is ObjectMemory -> this.value
        is ReferenceMemory -> this.value.toJavaObject() // 递归
        else -> throw IllegalArgumentException("Cannot auto convert [$this] into JavaObject")
    }
}