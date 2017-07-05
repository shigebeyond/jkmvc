package com.jkmvc.common

import java.util.*

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
public inline fun <reified T:Any>  Map<Any, String?>.getAndConvert(key:String, defaultValue:T?): T? {
    return get(key).toNullable(T::class, defaultValue)
}

/**
 * -- 无用，基类　HashTable已实现getOrDefault()方法
 * 获得属性表的某个值，如果值为空，则返回默认值
 * @param key 键名
 * @param default 默认值
 * @return
 */
/*public inline fun Properties.getOrDefault(key:String, default:String? = null): String? {
    val value = getProperty(key)
    return if(value == null)
                default
            else
                value;
}*/

/**
 * 获得属性表的某个值，并转换为指定类型
 * @param key 键名
 * @param default 默认值
 * @return
 */
public inline fun <reified T:Any>  Properties.getAndConvert(key:String, defaultValue:T?): T? {
    return getProperty(key).toNullable(T::class, defaultValue)
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