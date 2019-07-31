package net.jkcode.jkmvc.interceptor

import java.util.concurrent.CompletableFuture

/**
 * 请求拦截器
 *    泛型 R 是请求类型
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-01 11:39 AM
 */
interface IRequestInterceptor<R> {

    /**
     * 拦截action, 插入前置后置处理
     *
     * @param req
     * @param action 被拦截的处理
     * @return 返回CompletableFuture: 1是给业务方调用, 让他能设置回调, 2是给拦截器链表的下一个拦截器调用, 让他能够更准确的确定后置处理的调用时机
     */
    fun intercept(req: R, action: () -> Any?): CompletableFuture<Any?>
    /*{
        // 前置处理 -- 可以直接抛异常, 可以直接return

        // 转future
        val future = trySupplierFuture(action)

        // 后置处理
        future.whenComplete{ r, ex ->

        }
    }*/

}