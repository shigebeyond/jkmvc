package net.jkcode.jkmvc.future

import java.util.*

/**
 * 可回调的异步结果
 * @author shijianhang<772910474@qq.com>
 * @date 2019-01-14 11:46 AM
 */
abstract class CallbackableFuture<T> : ICallbackableFuture<T> {

    /**
     * 回调列表
     */
    protected var callbacks: MutableList<IFutureCallback<T>>? = null

    /**
     * 添加回调
     * @param callback
     */
    public override fun addCallback(callback: IFutureCallback<T>){
        if(callbacks == null)
            callbacks = LinkedList()

        callbacks!!.add(callback)
    }
}