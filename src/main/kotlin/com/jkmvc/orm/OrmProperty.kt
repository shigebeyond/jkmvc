package com.jkmvc.orm

import kotlin.reflect.KProperty

/**
 * orm的属性代理对象
 */
object OrmProperty /* : ReadWriteProperty */ {
    operator fun getValue(thisRef: Orm, property: KProperty<*>): Any? {
        return thisRef[property.name]
    }

    operator fun setValue(thisRef: Orm, property: KProperty<*>, value: Any?) {
        thisRef[property.name] = value
    }
}