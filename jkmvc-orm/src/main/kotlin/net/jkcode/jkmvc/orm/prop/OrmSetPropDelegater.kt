package net.jkcode.jkmvc.orm.prop

import net.jkcode.jkmvc.orm.IOrmEntity
import java.io.Serializable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


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