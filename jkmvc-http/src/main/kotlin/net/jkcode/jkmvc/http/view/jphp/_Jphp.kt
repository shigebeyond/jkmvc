package net.jkcode.jkmvc.http.view.jphp

import php.runtime.Memory
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
//        is IObject -> StringMemory(this)
        else -> throw IllegalArgumentException("Cannot auto convert [$this] into Memory")
    }
}