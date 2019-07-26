package net.jkcode.jkmvc.common

import java.util.*

/**
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-26 3:25 PM
 */
class RequestInterceptorContainer<R>(
        protected val list: MutableList<IRequestInterceptor<R>> = LinkedList() // 代理list
): IRequestInterceptor<R>{

    init {
        // 倒序
        list.reverse()
    }

    /**
     * 拦截action, 插入前置后置处理
     *
     * @param req
     * @param action 被拦截的处理
     */
    public override fun intercept(req: R, action: () -> Any?) {
        var last: () -> Any? = action
        for (interceptor in list) {
            val next = last
            last = { interceptor.intercept(req, next) }
        }
    }


}