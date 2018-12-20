package com.jkmvc.orm

import com.jkmvc.common.newInstance
import com.jkmvc.db.Row
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
 * 获得类的行转换器
 * @return 转换的匿名函数
 */
public val <T:IOrm> KClass<T>.rowTranformer: (Row) -> T
    get(){
        return {
            //val obj = this.newInstance(false) as IOrm // 无需默认构造函数
            val obj = java.newInstance() as IOrm // 必须默认构造函数
            obj.setOriginal(it) as T
        }
    }

/**
 * orm列表获得字段值
 */
fun Collection<out IOrm>.itemToMap(): List<Map<String, Any?>> {
    if(this.isEmpty())
        return emptyList()

    return this.map {
        it.toMap()
    }
}

/**
 * orm列表转哈希
 *
 * @param keyField 子项字段名，其值作为结果哈希的key
 * @param valueField 子项字段名，其值作为结果哈希的value，如果为null，则用子项作为结果哈希的value
 * @return
 */
fun <K, V> Collection<out IOrm>.toMap(keyField:String, valueField:String?): Map<K, V?> {
    if(this.isEmpty())
        return emptyMap()

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
fun <K, V:IOrm> Collection<V>.toMap(keyField:String): Map<K, V> {
    return toMap<K, V>(keyField, null) as Map<K, V>
}

/**
 * 收集某列的值
 *
 * @param key 列名
 * @return
 */
public fun Collection<out IOrm>.collectColumn(key:String):List<Any?>{
    if(this.isEmpty())
        return emptyList()

    return this.map {
        val v: Any? = it[key]
        v
    }
}

/**
 * 收集某列去重唯一的值
 *
 * @param key 列名
 * @return
 */
public fun Collection<out IOrm>.collectUniqueColumn(key:String):Set<Any?>{
    if(this.isEmpty())
        return emptySet()

    val result = HashSet<Any?>()
    for(item in this)
        result.add(item[key])
    return result
}

/**
 * 获得某列的迭代器
 *
 * @param key 列名
 * @return
 */
public fun Collection<out IOrm>.columnIterator(key:String):Iterator<Any?>{
    if(this.isEmpty())
        return org.apache.commons.collections.iterators.EmptyIterator.INSTANCE

    return ColumnIterator(this.iterator(), key)
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