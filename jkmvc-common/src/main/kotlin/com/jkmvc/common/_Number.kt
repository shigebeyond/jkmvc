package com.jkmvc.common

import java.math.BigDecimal
import kotlin.reflect.KClass

/**
 * 将BigDecimal转换为指定类型的数值
 * @param class 要转换的类型
 * @return
 */
public inline fun <T: Any> BigDecimal.toNumber(clazz: KClass<T>): T{
    var result: Any?
    when(clazz){
        Int::class -> result = this.toInt()
        Long::class -> result = this.toLong()
        Float::class -> result = this.toFloat()
        Double::class -> result = this.toDouble()
        else -> throw IllegalArgumentException("字符串不能自动转换为未识别的类型: " + clazz)
    }
    return result as T
}

/**
 * 将BigDecimal转换为指定类型的数值
 * @param class 要转换的类型
 * @return
 */
public inline fun <T: Any> BigDecimal.toNumber(clazz: Class<T>): T{
    return this.toNumber(clazz.kotlin)
}