package net.jkcode.jkmvc.es

import net.jkcode.jkutil.common.getCachedAnnotation
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.memberProperties

/**
 * es id的注解
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class EsId

/**
 * 从属性中获得 @EsId 注解
 */
public val KProperty1<*, *>.esId: EsId?
    get() {
        return getCachedAnnotation()
    }

/**
 * 从类中获得有 @EsId 注解的属性
 */
public fun <T : Any> KClass<T>.getEsIdProp(): KProperty1<T, *>? {
    return this.memberProperties.firstOrNull {
        it.esId != null
    }
}