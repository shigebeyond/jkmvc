package net.jkcode.jkmvc.common

import java.util.concurrent.CompletableFuture

/**
 * 拦截器的链表
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-26 3:25 PM
 */
class RequestInterceptorChain<R>(
        protected val chain: MutableList<IRequestInterceptor<R>> // 链表
): IRequestInterceptor<R>{

    /**
     * 倒序的链表
     */
    protected val reversedChain = chain.asReversed()

    /**
     * 拦截action, 插入前置后置处理
     *
     * @param req
     * @param action 被拦截的处理
     * @return
     */
    public override fun intercept(req: R, action: () -> Any?): CompletableFuture<Any?> {
        // 1 无链表, 直接调用action
        if(reversedChain.isEmpty())
            return trySupplierFuture(action)

        // 2 有链表, 链式倒序包装拦截处理
        // 当前处理
        var curr: () -> Any? = action
        // 倒序包装拦截器
        for (interceptor in reversedChain) {
            // 下一个处理, 倒序嘛
            val next = curr
            // 包装拦截处理
            curr = {
                // 拦截
                interceptor.intercept(req, next)
            }
        }
        return curr.invoke() as CompletableFuture<Any?>
    }


}