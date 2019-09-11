package net.jkcode.jkmvc.common

import java.math.BigDecimal
import kotlin.reflect.KClass

/**
 * 将BigDecimal转换为指定类型的数值
 * @param class 要转换的类型
 * @return
 */
public inline fun <T: Any> BigDecimal.toNumber(clazz: KClass<T>): T{
    return when(clazz){
        Byte::class -> this.toByte()
        Short::class -> this.toShort()
        Int::class -> this.toInt()
        Long::class -> this.toLong()
        Float::class -> this.toFloat()
        Double::class -> this.toDouble()
        else -> throw IllegalArgumentException("字符串不能自动转换为未识别的类型: " + clazz)
    } as T
}

/**
 * 将BigDecimal转换为指定类型的数值
 * @param class 要转换的类型
 * @return
 */
public inline fun <T: Any> BigDecimal.toNumber(clazz: Class<T>): T{
    return this.toNumber(clazz.kotlin)
}