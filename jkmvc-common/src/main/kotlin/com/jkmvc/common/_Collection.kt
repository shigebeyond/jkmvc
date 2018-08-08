package com.jkmvc.common

import java.util.*
import java.util.concurrent.ThreadLocalRandom

/**
 * 是否数组
 * @return
 */
public fun Any.isArray(): Boolean {
    return this is Array<*> || this is IntArray || this is ShortArray || this is LongArray || this is FloatArray || this is DoubleArray || this is BooleanArray
}

/**
 * 是否数组或集合
 * @return
 */
public fun Any?.isArrayOrCollection(): Boolean {
    return this != null && (this.isArray() || this is Collection<*>)
}

/**
 * 是否数组或集合为空
 * @return
 */
public fun Any.isArrayOrCollectionEmpty(): Boolean {
    if(this is Array<*>)
        return this.isEmpty()
    if(this is IntArray)
        return this.isEmpty()
    if(this is ShortArray)
        return this.isEmpty()
    if(this is LongArray)
        return this.isEmpty()
    if(this is FloatArray)
        return this.isEmpty()
    if(this is DoubleArray)
        return this.isEmpty()
    if(this is BooleanArray)
        return this.isEmpty()
    if(this is Collection<*>)
        return this.isEmpty()
    return false
}

/**
 * 检查集合是否为空
 * @return
 */
public fun <T> Collection<T>?.isNullOrEmpty(): Boolean {
    return this === null || this.isEmpty()
}

/**
 * 从集合中获得随机一个元素
 * @return
 */
public fun <T> Collection<T>.getRandom(): T {
    if(this.isEmpty())
        throw IndexOutOfBoundsException("No element in empty collection")

    var n = ThreadLocalRandom.current().nextInt(this.size)
    // list
    if(this is List)
        return this[n]

    // 非list
    val it = this.iterator()
    while (--n > 0)
        it.next()
    return it.next()
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
public inline fun <reified T:Any>  Map<*, *>.getAndConvert(key:String, defaultValue:T? = null): T? {
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
 * 检查是否包含任一个键
 *
 * @param keys
 * @return
 */
public fun Map<*, *>.containsAnyKey(vararg keys: Any): Boolean {
    for(key in keys)
        if(this.containsKey(key))
            return true

    return false
}

/**
 * 检查是否包含所有键
 *
 * @param keys
 * @return
 */
public fun Map<*, *>.containsAllKeys(vararg keys: Any): Boolean {
    for(key in keys)
        if(!this.containsKey(key))
            return false

    return true
}

/**
 * map删除多个key
 * @param keys
 * @return
 */
public fun  <K, V> MutableMap<K, V>.removeAll(keys: Collection<K>): MutableMap<K, V> {
    for (key in keys)
        remove(key)
    return this
}

/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 */
public inline fun <K, V> Map<*, *>.associate(transform: (Map.Entry<*, *>) -> Pair<K, V>): MutableMap<K, V> {
    val result:MutableMap<K, V> = HashMap<K, V>();
    for(e in this)
        result += transform(e)
    return result;
}

/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given array.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 */
public inline fun <A, B, K, V> Map<A, B>.associate(transform: (key: A, value: B) -> Pair<K, V>): MutableMap<K, V> {
    val result:MutableMap<K, V> = HashMap<K, V>();
    for((key, value) in this)
        result += transform(key, value)
    return result;
}

/**
 * Returns `true` if at least one entry matches the given [predicate].
 *
 * @sample samples.collections.Collections.Aggregates.anyWithPredicate
 */
public inline fun <K, V> Map<out K, V>.any(predicate: (key: K, value: V) -> Boolean): Boolean {
    if (isEmpty()) return false
    for ((key, value) in this) if (predicate(key, value)) return true
    return false
}

/**
 * 获得'.'分割的路径下的子项值
 *
 * @param path '.'分割的路径
 * @return
 */
public fun Map<String, *>.path(path:String): Any? {
    return path(path, true)
}

/**
 * 获得'.'分割的路径下的子项值
 *
 * @param path '.'分割的路径
 * @param withException 当不存在子项时，是否抛出异常，否则返回default默认值
 * @param default 默认值，当 withException 为false时有效
 * @return
 */
public fun Map<String, *>.path(path:String, default: Any?): Any? {
    return path(path, false, default)
}

/**
 * 获得'.'分割的路径下的子项值
 *
 * @param path '.'分割的路径
 * @param withException 当不存在子项时，是否抛出异常，否则返回default默认值
 * @param default 默认值，当 withException 为false时有效
 * @return
 */
public fun Map<String, *>.path(path:String, withException: Boolean, default: Any?): Any? {
    // 单层
    if(!path.contains('.'))
        return this[path]

    // 多层
    val empty = emptyMap<String, Any?>()
    val keys:List<String> = path.split('.')
    var data:Map<String, Any?> = this
    var value:Any? = null
    for (key in keys){
        // 当不存在子项时，抛异常 or 返回null
        if(data == empty){
            if(withException)
                throw NoSuchElementException("获得Map子项失败：Map数据为$this, 但路径[$path]的子项不存在")
            return default
        }

        // 一层层往下走
        value =  data[key]
        if(value is Map<*, *>)
            data = value as Map<String, Any?>
        else
            data = empty
    }
    return value
}

/**
 * 设置'.'分割的路径下的子项值
 *
 * @param path '.'分割的路径
 * @param value 目标值
 */
public fun MutableMap<String, Any?>.setPath(path:String, value:Any?): Unit {
    // 单层
    if(!path.contains('.')){
        this[path] = value
        return
    }

    // 多层
    val keys:List<String> = path.split('.')
    var data:MutableMap<String, Any?> = this
    for (i in 0..(keys.size - 2)){
        val key = keys[i]
        // 一层层往下走
        if(data[key] is MutableMap<*, *>)
            data =  data[key] as MutableMap<String, Any?>
        else // 当不存在子项时，抛异常
            throw NoSuchElementException("获得Map子项失败：Map数据为$this, 但路径[$path]的父项不存在")
    }
    data[keys.last()] = value
}

/**
 * 收集某列的值
 *
 * @param key
 * @return
 */
public fun <K, V> Collection<Map<K, V>>.collectColumn(key:K):Collection<V>{
    return this.map {
        it[key]
    } as Collection<V>
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

/**
 * map列表转哈希
 *
 * @param keyField 子项字段名，其值作为结果哈希的key
 * @param valueField 子项字段名，其值作为结果哈希的value，如果为null，则用子项作为结果哈希的value
 * @return
 */
public fun Collection<out Map<*, *>>.asMap(keyField:String, valueField:String?): Map<*, *> {
    if(this.isEmpty())
        return emptyMap<Any, Any>()

    return this.associate {
        // key to value
        it[keyField]!! to (if(valueField == null) it else it[valueField])
    }
}

/**
 * 请求参数转query string
 * @param buffer
 * @return
 */
public fun Map<String, Array<String>>.toQueryString(buffer: StringBuilder): StringBuilder {
    entries.joinTo(buffer, "") {
        "${it.key}=${it.value.first()}&"
    }
    return buffer.deleteSuffix("&")
}

/**
 * 请求参数转query string
 * @param buffer
 * @return
 */
public fun Map<String, Array<String>>.toQueryString(): String {
    if(this.isEmpty())
        return ""

    return toQueryString(StringBuilder()).toString()
}