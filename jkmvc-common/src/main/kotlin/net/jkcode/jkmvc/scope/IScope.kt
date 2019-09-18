package net.jkcode.jkmvc.scope

import net.jkcode.jkmvc.common.trySupplierFinally
import java.io.Closeable

/**
 * 作用域对象
 *    1. 实现该接口, 必须承诺 beginScope()/endScope()会在作用域开始与结束时调用
 *    2. 父作用域的 beginScope()/endScope() 会自动调用子作用域的 beginScope()/endScope()
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-17 9:52 AM
 */
interface IScope {

    /**
     * 添加子作用域
     * @param childScope
     */
    fun addChildScope(childScope: IScope)

    /**
     * 添加子作用域
     * @param closing
     */
    fun addChildScope(closing: Closeable){
        addChildScope(CloseableScope(closing))
    }

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