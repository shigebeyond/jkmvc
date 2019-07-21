package net.jkcode.jkmvc.flusher

import net.jkcode.jkmvc.common.getSuperClassGenricType
import java.util.concurrent.CompletableFuture

/**
 * 刷盘器
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-17 8:27 AM
 */
interface IFlusher<RequestArgumentType /* 请求参数类型 */, ResponseType /* 返回值类型 */> {

    /**
     * 请求是否为空
     * @return
     */
    fun isRequestEmpty(): Boolean

    /**
     * 将积累的请求刷掉
     * @param byTimeout 是否定时触发 or 定量触发
     * @param timerCallback 定时刷盘的回调, 会调用 startTimer() 来继续下一轮定时
     */
    fun flush(byTimeout: Boolean = true, timerCallback: (() -> Unit)? = null)

    /**
     * 单个请求入队
     * @param arg
     * @return 返回异步响应, 如果入队失败, 则返回null
     */
    fun add(arg: RequestArgumentType): CompletableFuture<ResponseType>

    /**
     * 多个请求入队
     *    只有无返回值时才支持批量请求入队
     *
     * @param args
     * @return 返回异步响应, 如果入队失败, 则返回null
     */
    fun addAll(args: List<RequestArgumentType>): CompletableFuture<ResponseType>

    /**
     * 是否无返回值
     *    即返回值值类型为 Void / Unit
     * @return
     */
    fun isNoResponse(): Boolean {
        val responseType = this.javaClass.getSuperClassGenricType(1)
        return responseType == Void::class.java || responseType == Unit::class.java
    }
}