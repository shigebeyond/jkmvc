package net.jkcode.jkmvc.guard

/**
 * 拦截器
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-01 11:39 AM
 */
interface IInterceptor<T> {

    /**
     * 前置处理请求
     * @param req
     * @return
     */
    fun preHandleRequest(req: T): Boolean

}