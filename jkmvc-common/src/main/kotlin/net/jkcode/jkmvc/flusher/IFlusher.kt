package net.jkcode.jkmvc.flusher

import java.util.concurrent.CompletableFuture

/**
 * 刷盘器
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-17 8:27 AM
 */
interface IFlusher<RequestType /* 请求类型 */, ResponseType /* 响应值类型 */> {

    /**
     * 获得请求计数
     * @return
     */
    fun requestCount(): Int

    /**
     * 请求是否为空
     * @return
     */
    fun isRequestEmpty(): Boolean{
        return requestCount() == 0
    }

    /**
     * 将积累的请求刷掉
     * @param byTimeout 是否定时触发 or 定量触发
     */
    fun flush(byTimeout: Boolean)

    /**
     * 单个请求入队
     * @param req
     * @return
     */
    fun add(req: RequestType): CompletableFuture<ResponseType>
}