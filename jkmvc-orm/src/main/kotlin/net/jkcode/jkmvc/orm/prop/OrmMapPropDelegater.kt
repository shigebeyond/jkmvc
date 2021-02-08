package net.jkcode.jkmvc.orm.prop

import net.jkcode.jkmvc.orm.DbKeyNames
import net.jkcode.jkmvc.orm.IOrmEntity
import java.io.Serializable
import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


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