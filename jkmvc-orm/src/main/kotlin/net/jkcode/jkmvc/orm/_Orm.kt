package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.db.DbResultRow
import net.jkcode.jkutil.common.decorateIterator
import net.jkcode.jkutil.common.lcFirst
import java.io.Serializable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
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
        val value: Any? = thisRef[property.name]
        // int转bool
        if(value != null && Boolean::class == (property.returnType.classifier as KClass<*>) && value !is Boolean){
            return when(value) {
                is Int -> value > 0
                is Long -> value > 0
                is Short -> value > 0
                is Byte -> value > 0
                is Float -> value > 0
                is Double -> value > 0
                else -> throw IllegalArgumentException("值[$value]不能自动转换为Boolean: ")
            }
        }

        // 其他
        return value
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
 * orm map属性代理
 *    TODO: 只是简单将list转为map, 只读
 */
class OrmMapPropDelegater protected constructor (public val keys: DbKeyNames): ReadWriteProperty<IOrmEntity, Any?>, Serializable {

    companion object{

        /**
         * 单例池: <类 to 单例>
         */
        private val insts: ConcurrentHashMap<DbKeyNames, OrmMapPropDelegater> = ConcurrentHashMap();

        /**
         * 获得单例
         */
        public fun instance(keys: DbKeyNames): OrmMapPropDelegater {
            return insts.getOrPut(keys){
                OrmMapPropDelegater(keys)
            }
        }
    }

    // 获得属性
    public override operator fun getValue(thisRef: IOrmEntity, property: KProperty<*>): Any? {
        val list: List<Any?>? = thisRef[property.name]
        if(list == null)
            return emptyMap<Any, Any?>()

        return (list as List<IOrmEntity>).associateBy { item ->
            keys.columns.joinToString("::") { key ->
                item[key]
            }
        }
    }

    // 设置属性
    public override operator fun setValue(thisRef: IOrmEntity, property: KProperty<*>, value: Any?) {
        val list = (value as Map<Any, Any?>?)?.mapTo(HashSet()) { it.value }
        thisRef[property.name] = list
    }
}

/**
 * orm set属性代理
 *    TODO: 只是简单将list转为Set, 只读
 */
object OrmSetPropDelegater: ReadWriteProperty<IOrmEntity, Any?>, Serializable {
    // 获得属性
    public override operator fun getValue(thisRef: IOrmEntity, property: KProperty<*>): Any? {
        val list: List<Any?>? = thisRef[property.name]
        if(list == null)
            return emptySet<Any?>()

        return list.toSet()
    }

    // 设置属性
    public override operator fun setValue(thisRef: IOrmEntity, property: KProperty<*>, value: Any?) {
        // set也是集合, 不用转了
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
    get() = this.simpleName!!.removeSuffix("Model").lcFirst();

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
public val <T:IOrm> KClass<T>.modelRowTransformer: (DbResultRow) -> T
    get(){
        // 实例化函数
        return {
            // 实例化
            //val obj = java.newInstance() as IOrm
            // 使用无参数构造函数/可变参数构造参数来实例化
            val obj = this.modelOrmMeta.newInstance()

            // 设置字段值
            obj.setOriginal(it)
                    obj as T
        }
    }


/**
 * 全局共享的可复用的模型实例
 *   主要用在实体类的行转换器 entityRowTransformer()
 *   由于实体类没有orm映射元数据, 因此他是无法识别结果行的数据, 也无法转换为实体属性, 因此需要通过orm模型作为桥梁
 *   orm模型将结果行转为模型对象, 再将模型对象转为实体对象
 */
private val reusedModels:ThreadLocal<MutableMap<KClass<*>, Any?>> = ThreadLocal.withInitial {
    HashMap<KClass<*>, Any?>();
}

/**
 * 获得实体类的行转换器
 * @return 转换的匿名函数
 */
public fun <T: IEntitiableOrm<E>, E: OrmEntity> KClass<T>.entityRowTransformer(entityClass: KClass<E>): (DbResultRow) -> E {
    // 实例化函数
    return {
        // 获得模型实例
        val obj = reusedModels.get().getOrPut(this) {
            java.newInstance()
        } as IEntitiableOrm<E>

        // 清空字段值
        obj.clear()

        // 设置字段值
        obj.setOriginal(it)

        // 转为实体
        obj.toEntity()
    }
}

/**
 * orm列表转为实体列表
 */
fun <T: OrmEntity> Collection<out IEntitiableOrm<T>>.toEntities(): List<T> {
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

    return this.associate {
        val key:K = it[keyField]
        val value:V = if(valueField == null) it as V else it[valueField]
        key to value
    }
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
