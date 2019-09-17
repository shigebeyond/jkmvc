package net.jkcode.jkmvc.ttl

import net.jkcode.jkmvc.common.trySupplierFuture
import java.util.concurrent.CompletableFuture

/**
 * 有作用域的对象
 *    实现该接口, 必须承诺 begin()/end()会在作用域开始与结束时调用
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-17 9:52 AM
 */
interface Scoped {

    /**
     * 作用域开始
     */
    fun begin()

    /**
     * 作用域结束
     */
    fun end()

    /**
     * 启动新的作用域
     * @param action
     * @return
     */
    public fun <T> newScope(action: () -> T):T{
        try{
            begin() // 开始
            return action()
        }finally {
            end() // 结束
        }
    }

    /**
     * 启动新的作用域, 但异步结束
     * @param action
     * @return
     */
    public fun <T> newScopeAsync(action: () -> CompletableFuture<T>): CompletableFuture<T> {
        begin() // 开始
        val future = trySupplierFuture(action)
        return future.whenComplete{ r, ex ->
            end() // 结束
            r
        } as CompletableFuture<T>
    }
}