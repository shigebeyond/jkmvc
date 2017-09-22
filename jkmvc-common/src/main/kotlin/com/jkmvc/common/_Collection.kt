package com.jkmvc.common

import java.util.*

/**
 * 检查集合是否为空
 */
public fun <T> Collection<T>?.isNullOrEmpty(): Boolean {
    return this === null || this.isEmpty()
}

/**
 * 获得数组的某个元素值，如果值为空，则给该元素赋值
 * @param index 元素索引
 * @param default 赋值回调
 * @return
 */
public inline fun <T> Array<T>.getOrPut(index: Int, defaultValue: (Int) -> T): T {
    if(this[index] == null)
        this[index] = defaultValue(index)
    return this[index];
}

/**
 * 获得map的某个值，如果值为空，则返回默认值
 * @param key 键名
 * @param default 默认值
 * @return
 */
public inline fun <K, V> Map<K, V>?.getOrDefault(key:K, default:V? = null): V? {
    val value = this?.get(key)
    return if(value == null)
                default
            else
                value;
}

/**
 * 获得map的某个值，并转换为指定类型
 * @param key 键名
 * @param default 默认值
 * @return
 */
public inline fun <reified T:Any>  Map<*, *>.getAndConvert(key:String, defaultValue:T?): T? {
    val value = get(key)
    // 默认值
    if(value === null)
        return defaultValue
    // 不用转换
    if(value is T)
        return value
    // 要转换
    if(value !is String)
        throw ClassCastException("Fail to convert [$value] to type [${T::class}]")
    return (value as String).to(T::class)
}

/**
 * Iterator转Enumeration
 */
class ItEnumeration<T>(val it: Iterator<T>) : Enumeration<T> {

    override fun hasMoreElements(): Boolean{
        return it.hasNext()
    }

    override fun nextElement(): T {
        return it.next();
    }
}

/**
 * 获得Enumeration
 * @return
 */
public fun <T> Iterable<T>.enumeration(): ItEnumeration<T> {
    return ItEnumeration(iterator())
}