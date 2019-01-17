package com.jkmvc.common

import java.math.BigDecimal
import java.util.concurrent.ThreadLocalRandom
import kotlin.reflect.KClass

/**
 * 随机的int
 * @return
 */
public inline fun randomInt(bound: Int): Int {
    return ThreadLocalRandom.current().nextInt(bound)
}

/**
 * 随机的long
 * @return
 */
public inline fun randomLong(bound: Long): Long {
    return ThreadLocalRandom.current().nextLong(bound)
}

/**
 * 随机的bool
 * @return
 */
public inline fun randomBoolean(): Boolean {
    return randomInt(2) == 1
}

/**
 * 将BigDecimal转换为指定类型的数值
 * @param class 要转换的类型
 * @return
 */
public inline fun <T: Any> BigDecimal.toNumber(clazz: KClass<T>): T{
    return when(clazz){
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