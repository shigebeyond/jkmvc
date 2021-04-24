package net.jkcode.jkmvc.es

import net.jkcode.jkutil.common.getCachedAnnotation
import java.lang.reflect.Method

/**
 * es id的注解
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class EsId

/**
 * 获得es id的注解
 */
public val Method.esId: EsId?
    get(){
        return getCachedAnnotation()
    }