package net.jkcode.jksoa.client.combiner.annotation

import net.jkcode.jkmvc.cache.ICache
import java.lang.annotation.Documented
import java.lang.annotation.RetentionPolicy
import kotlin.reflect.KClass

/**
 * 服务元数据的注解
 * @author shijianhang<772910474@qq.com>
 * @date 2019-02-22 6:04 PM
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GroupCombine()