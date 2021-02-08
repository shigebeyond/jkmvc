package net.jkcode.jkmvc.orm.prop

import net.jkcode.jkmvc.orm.IOrmEntity
import java.io.Serializable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

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
                else -> throw IllegalArgumentException("Cannot auto convert [$value] into Boolean")
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