package net.jkcode.jkmvc.orm

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import net.jkcode.jkmvc.common.decorateIterator
import net.jkcode.jkmvc.db.Row
import java.io.Serializable
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.companionObjectInstance

/**
 * orm普通属性代理
 */
object OrmPropDelegater: ReadWriteProperty<IOrmEntity, Any?>, Serializable {
    // 获得属性
    public override operator fun getValue(thisRef: IOrmEntity, property: KProperty<*>): Any? {
        return thisRef[property.name]
    }

    // 设置属性
    public override operator fun setValue(thisRef: IOrmEntity, property: KProperty<*>, value: Any?) {
        thisRef[property.name] = value
    }
}

/**
 * orm列表属性代理
 */
object OrmListPropDelegater: ReadWriteProperty<IOrmEntity, Any?>, Serializable {
    // 获得属性
    public override operator fun getValue(thisRef: IOrmEntity, property: KProperty<*>): Any? {
        return thisRef.getOrPut(property.name){
            LinkedList<Any?>()
        }
    }

    // 设置属性
    public override operator fun setValue(thisRef: IOrmEntity, property: KProperty<*>, value: Any?) {
        thisRef[property.name] = value
    }
}

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
 * 获得模型类的行转换器
 * @return 转换的匿名函数
 */
public val <T:IOrm> KClass<T>.rowTransformer: (Row) -> T
    get(){
        // 实例化函数
        return {
            // 实例化
            val obj = java.newInstance() as IOrm

            // 设置字段值
            obj.setOriginal(it)
                    obj as T
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
 * orm列表转为实体列表
 */
fun <T: OrmEntity> Collection<out IEntitiableOrm<T>>.itemToEntity(): List<T> {
    if(this.isEmpty())
        return emptyList()

    return this.map {
        it.toEntity()
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

    return decorateIterator(this.iterator()){
        //it[key] as Any
        it.get<Any?>(key)
    }
}

/**
 * 标准化orm数据
 *    对orm对象要转map
 * @param data
 * @return
 */
public fun normalizeOrmData(data: Any?): Any? {
    // 对orm对象要转map
    if (data is IOrm)
        return data.toMap()

    // 对orm列表要转map
    if(data is List<*> && data.isNotEmpty() && data.first() is IOrm)
        return (data as List<IOrm>).itemToMap()

    return data
}

/**
 * 对象转json
 * @return
 */
public fun Any.toJson(): String {
    //data.toJSONString()
    return JSON.toJSONString(normalizeOrmData(this), SerializerFeature.WriteDateUseDateFormat /* Date格式化 */, SerializerFeature.WriteMapNullValue /* 输出null值 */)
}