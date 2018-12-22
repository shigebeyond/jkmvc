package com.jkmvc.common

import java.math.BigDecimal
import java.util.*
import java.util.concurrent.ThreadLocalRandom

/****************************** 扩展 Array + Collection *****************************/
/**
 * 非空参数转为array, 仅用于在 DbKey/Orm 的构造函数中转参数
 * @param params
 * @return
 */
public inline fun <T> toArray(vararg params:T): Array<T> {
    return params as Array<T>
}

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
 * 获得数组或集合的迭代器
 * @return
 */
public fun Any.iteratorArrayOrCollection(): Iterator<*>? {
    if(this is Array<*>)
        return this.iterator()
    if(this is IntArray)
        return this.iterator()
    if(this is ShortArray)
        return this.iterator()
    if(this is LongArray)
        return this.iterator()
    if(this is FloatArray)
        return this.iterator()
    if(this is DoubleArray)
        return this.iterator()
    if(this is BooleanArray)
        return this.iterator()
    if(this is Collection<*>)
        return this.iterator()
    return null
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
 * 集合转数组
 *   注: Array<R> 不能使用R作为泛型参数, 只能使用具体类
 */
public inline fun <T, reified R> Collection<T>.mapToArray(transform: (T) -> R): Array<R> {
    val arr = arrayOfNulls<R?>(this.size);
    var i = 0;
    for (item in this)
        arr[i++] = transform(item)
    return arr as Array<R>
}

/**
 * 数组转数组
 *   注: Array<R> 不能使用R作为泛型参数, 只能使用具体类
 */
public inline fun <T, reified R> Array<T>.mapToArray(transform: (T) -> R): Array<R> {
    val arr = arrayOfNulls<R?>(this.size);
    var i = 0;
    for (item in this)
        arr[i++] = transform(item)
    return arr as Array<R>
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

/****************************** query string *****************************/
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

/****************************** 扩展 Iterator *****************************/
/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 */
public inline fun <T, R> Iterator<T>.map(transform: (T) -> R): List<R> {
    return mapTo(ArrayList<R>(), transform)
}

/**
 * Applies the given [transform] function to each element of the original collection
 * and appends the results to the given [destination].
 */
public inline fun <T, R, C : MutableCollection<in R>> Iterator<T>.mapTo(destination: C, transform: (T) -> R): C {
    for (item in this)
        destination.add(transform(item))
    return destination
}

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 *
 * @sample samples.collections.Collections.Transformations.joinToString
 */
public fun <T> Iterator<T>.joinToString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((T) -> CharSequence)? = null): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
}

/**
 * Appends the string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 *
 * @sample samples.collections.Collections.Transformations.joinTo
 */
public fun <T> Iterator<T>.joinTo(buffer: StringBuilder, separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((T) -> CharSequence)? = null): StringBuilder {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            val value = if(transform == null) element else transform(element)
            buffer.append(value)
        } else break
    }
    if (limit >= 0 && count > limit) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
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

/****************************** 扩展 Map *****************************/
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
 * @param withException 当不存在子项时，是否抛出异常，否则返回default默认值
 * @param default 默认值，当 withException 为false时有效
 * @return
 */
public fun Map<String, *>.path(path:String, withException: Boolean = true, default: Any? = null): Any? {
    // 单层
    if(!path.contains('.'))
        return this[path]

    // 多层
    val keys:List<String> = path.split('.')
    var data:Any? = this
    var value:Any? = null
    for (key in keys){
        // 一层层往下走
        if(data is Map<*, *>){ // Map
            value =  data[key]
        }else if(data is List<*>){ // List
            try {
                value = data[key.toInt()]
            }catch (e: NumberFormatException){
                throw IllegalArgumentException("获得Map子项失败：Map数据为$this, 但路径[$path]中的父项是List, 子项[$key]却不是Int")
            }
        }else{ // 当不存在子项时，抛异常 or 返回null
            if(withException)
                throw NoSuchElementException("获得Map子项失败：Map数据为$this, 但路径[$path]中的无子项[$key]")
            return default
        }

        data = value
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
 * map列表转哈希
 *
 * @param keyField 子项字段名，其值作为结果哈希的key
 * @param valueField 子项字段名，其值作为结果哈希的value，如果为null，则用子项作为结果哈希的value
 * @return
 */
public fun Collection<out Map<*, *>>.toMap(keyField:String, valueField:String? = null): Map<*, *> {
    if(this.isEmpty())
        return emptyMap<Any, Any>()

    return this.associate {
        // key to value
        it[keyField]!! to (if(valueField == null) it else it[valueField])
    }
}

/********************** 转为各种类型的Map<String, *>.getter *********************/
/**
 * Get attribute of db type: varchar, char, enum, set, text, tinytext, mediumtext, longtext
 */
fun Map<String, *>.getString(name: String): String {
    return this[name] as String
}

/**
 * Get attribute of db type: int, integer, tinyint(n) n > 1, smallint, mediumint
 */
fun Map<String, *>.getInt(name: String): Int? {
    return this[name] as Int?
}

/**
 * Get attribute of db type: bigint, unsign int
 */
fun Map<String, *>.getLong(name: String): Long? {
    return this[name] as Long?
}

/**
 * Get attribute of db type: unsigned bigint
 */
fun Map<String, *>.getBigInteger(name: String): java.math.BigInteger {
    return this[name] as java.math.BigInteger
}

/**
 * Get attribute of db type: date, year
 */
fun Map<String, *>.getDate(name: String): java.util.Date {
    return this[name] as java.util.Date
}

/**
 * Get attribute of db type: time
 */
fun Map<String, *>.getTime(name: String): java.sql.Time {
    return this[name] as java.sql.Time
}

/**
 * Get attribute of db type: timestamp, datetime
 */
fun Map<String, *>.getTimestamp(name: String): java.sql.Timestamp {
    return this[name] as java.sql.Timestamp
}

/**
 * Get attribute of db type: real, double
 */
fun Map<String, *>.getDouble(name: String): Double? {
    return this[name] as Double?
}

/**
 * Get attribute of db type: float
 */
fun Map<String, *>.getFloat(name: String): Float? {
    return this[name] as Float?
}

/**
 * Get attribute of db type: bit, tinyint(1)
 */
fun Map<String, *>.getBoolean(name: String): Boolean? {
    return this[name] as Boolean?
}

/**
 * Get attribute of db type: tinyint(1)
 */
fun Map<String, *>.getShort(name: String): Short? {
    return this[name] as Short?
}

/**
 * Get attribute of db type: decimal, numeric
 */
fun Map<String, *>.getBigDecimal(name: String): BigDecimal {
    return this[name] as BigDecimal
}

/**
 * Get attribute of db type: binary, varbinary, tinyblob, blob, mediumblob, longblob
 */
fun Map<String, *>.getBytes(name: String): ByteArray {
    return this[name] as ByteArray
}

/**
 * Get attribute of any type that extends from Number
 */
fun Map<String, *>.getNumber(name: String): Number {
    return this[name] as Number
}