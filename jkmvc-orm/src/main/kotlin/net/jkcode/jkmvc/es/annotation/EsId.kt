package net.jkcode.jkmvc.es.annotation

import net.jkcode.jkutil.common.getCachedAnnotation
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * es id的注解
 *   用于表示 _id 的字段
 *
 * @author shijianhang
 * @date 2021-4-21 下午5:16:59
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
public val <T : Any> KClass<T>.esIdProp: KProperty1<T, *>?
    get() {
        return this.memberProperties.firstOrNull {
            it.esId != null
        }
    }