package net.jkcode.jkmvc.es.annotation

import net.jkcode.jkutil.common.getCachedAnnotation
import kotlin.reflect.KClass

/**
 * es文档注解
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class EsDoc(
        public val index: String,
        public val type: String = "_doc",
        public val esName: String = "default" // es配置名
)

/**
 * 从类中获得 @EsDoc 注解
 */
public val <T: Any> KClass<T>.esDoc: EsDoc?
    get() {
        return getCachedAnnotation()
    }