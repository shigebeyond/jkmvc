package net.jkcode.jkmvc.orm.prop

import net.jkcode.jkmvc.orm.IOrmEntity
import java.io.Serializable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * orm列表属性代理
 */
object OrmListPropDelegater: ReadWriteProperty<IOrmEntity, Any?>, Serializable {
    // 获得属性
    public override operator fun getValue(thisRef: IOrmEntity, property: KProperty<*>): Any? {
        return thisRef.getOrPut(property.name){
            //LinkedList<Any?>()
            emptyList<Any?>()
        }
    }

    // 设置属性
    public override operator fun setValue(thisRef: IOrmEntity, property: KProperty<*>, value: Any?) {
        thisRef[property.name] = value
    }
}