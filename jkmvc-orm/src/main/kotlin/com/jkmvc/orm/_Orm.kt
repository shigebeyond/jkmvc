package com.jkmvc.orm

import org.apache.commons.collections.iterators.AbstractIteratorDecorator
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

/**
 * 检查数据是否存在于db
 * @return
 */
public fun Orm?.isLoaded(): Boolean {
    return this != null && this.loaded;
}

/**
 * 根据模型类来获得模型名
 *   假定model类名, 都是以"Model"作为后缀
 */
public val KClass<out IOrm>.modelName:String
    get() = this.simpleName!!.removeSuffix("Model").toLowerCase();

/**
 * 根据模型类来获得模型元数据
 *   随对象就是元数据
 */
public val KClass<out IOrm>.modelOrmMeta: IOrmMeta
    get() = companionObjectInstance as IOrmMeta

/**
 * orm列表获得字段值
 */
fun Collection<out IOrm>.itemAsMap(): List<Map<String, Any?>> {
    return this.map {
        it.asMap()
    }
}

/**
 * orm列表转哈希
 *
 * @param keyField 子项字段名，其值作为结果哈希的key
 * @param valueField 子项字段名，其值作为结果哈希的value，如果为null，则用子项作为结果哈希的value
 * @return
 */
fun <K, V> Collection<out IOrm>.asMap(keyField:String, valueField:String?): Map<K, V?> {
    val result = HashMap<K, V?>()
    this.forEach {
        val key:K = it[keyField]
        val value:V = if(valueField == null) it as V else it[valueField]
        result[key] = value
    }
    return result
}

/**
 * orm列表转哈希
 *
 * @param keyField 字段名，其值作为结果哈希的key
 * @return
 */
fun <K, V:IOrm> Collection<V>.asMap(keyField:String): Map<K, V> {
    return asMap<K, V>(keyField, null) as Map<K, V>
}

/**
 * 收集某列的值
 *
 * @param keyField 列名
 * @return
 */
public fun Collection<out IOrm>.collectColumn(keyField:String):List<Any?>{
    return this.map {
        val v: Any? = it[keyField]
        v
    }
}

/**
 * 获得某列的迭代器
 *
 * @param keyField 列名
 * @return
 */
public fun Collection<out IOrm>.columnIterator(keyField:String):Iterator<Any?>{
    return ColumnIterator(this.iterator(), keyField)
}

/**
 * 列的迭代器
 */
class ColumnIterator(iterator: Iterator<out IOrm>, protected val keyField:String): AbstractIteratorDecorator(iterator){
    override fun next(): Any {
        val item = super.next() as IOrm
        return item[keyField]
    }
}