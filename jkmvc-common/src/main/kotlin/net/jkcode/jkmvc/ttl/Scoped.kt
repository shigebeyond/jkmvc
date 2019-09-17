package net.jkcode.jkmvc.ttl

import net.jkcode.jkmvc.common.trySupplierFinally
import net.jkcode.jkmvc.common.trySupplierFuture
import java.util.concurrent.CompletableFuture

/**
 * 有作用域的对象
 *    实现该接口, 必须承诺 beginScope()/endScope()会在作用域开始与结束时调用
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-17 9:52 AM
 */
interface Scoped {

    /**
     * 作用域开始
     */
    fun beginScope()

    /**
     * 作用域结束
     */
    fun endScope()

    /**
     * 启动新的作用域
     *    兼容 action 返回类型是CompletableFuture
     *
     * @param action
     * @return
     */
    public fun <T> newScope(action: () -> T):T{
        beginScope() // 开始

        return trySupplierFinally(action){ r, ex ->
            endScope() // 结束

            if(ex != null)
                throw ex;
            r
        }
    }

}