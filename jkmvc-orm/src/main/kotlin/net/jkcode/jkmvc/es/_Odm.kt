package net.jkcode.jkmvc.es

import net.jkcode.jkmvc.orm.IOrm
import net.jkcode.jkmvc.orm.IOrmMeta
import net.jkcode.jkutil.common.getStaticPropertyValue
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

/**
 * 根据模型类来获得模型元数据
 *   元数据 = kotlin类伴随对象 或 java类的静态属性ormMeta
 */
public val KClass<out Odm>.modelOdmMeta: OdmMeta
    get(){
        val om = companionObjectInstance // kotlin类的伴随对象
                ?: getStaticPropertyValue("odmMeta")

        if(om is OdmMeta)
            return om

        throw IllegalStateException("No OdmMeta definition for class: $this")
    }