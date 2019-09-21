package net.jkcode.jkmvc.ttl

import net.jkcode.jkmvc.scope.*

/**
 * 请求域的可传递的 ThreadLocal
 *   在所有请求的作用域中有效
 *   作为请求域的子作用域来自动处理
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-18 10:21 AM
 */
abstract class IRequestScopedTransferableThreadLocal<T>(reqScope: IRequestScope, supplier: (()->T)? = null): ScopedTransferableThreadLocal<T>(supplier) {

    init {
        // 作为请求域的子作用域来自动处理
        reqScope.addChildScope(this)
    }
}

// 所有请求域的可传递的 ThreadLocal
open class AllRequestScopedTransferableThreadLocal<T>(supplier: (()->T)? = null): IRequestScopedTransferableThreadLocal<T>(GlobalAllRequestScope, supplier)

// http请求域的可传递的 ThreadLocal
open class HttpRequestScopedTransferableThreadLocal<T>(supplier: (()->T)? = null): IRequestScopedTransferableThreadLocal<T>(GlobalHttpRequestScope, supplier)

// rpc请求域的可传递的 ThreadLocal
open class RpcRequestScopedTransferableThreadLocal<T>(supplier: (()->T)? = null): IRequestScopedTransferableThreadLocal<T>(GlobalRpcRequestScope, supplier)

