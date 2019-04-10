package net.jkcode.jkmvc.future

import java.util.concurrent.Future

/**
 * 可回调
 * @author shijianhang<772910474@qq.com>
 * @date 2019-01-14 11:46 AM
 */
interface ICallbackableFuture<T>: Future<T> {

    /**
     * 添加回调
     * @param callback
     */
    fun addCallback(callback: IFutureCallback<T>)

}