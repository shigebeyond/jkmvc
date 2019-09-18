package net.jkcode.jkmvc.ttl

import net.jkcode.jkmvc.scope.GlobalRequestScope

/**
 * 请求作用域的可传递的 ThreadLocal
 *   包含http请求与rpc请求
 *   作为GlobalRequestScope的子作用域来自动处理
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-18 10:21 AM
 */
open class RequestScopedTransferableThreadLocal<T>(supplier: (()->T)? = null): ScopedTransferableThreadLocal<T>(supplier) {

    init {
        // 作为GlobalRequestScope的子作用域来自动处理
        GlobalRequestScope.addChildScope(this)
    }
}