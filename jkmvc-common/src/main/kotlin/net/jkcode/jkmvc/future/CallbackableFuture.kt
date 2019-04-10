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
        // 在debug环境下处理早已收到的响应的情况
        // 当client调用本机server时, client很快收到响应, 在 addCallback()之前就收到了, 因此不能被动等待调用 callback, 只能主动调用 callback
        if(isDone){
            callback.completed(get())
            return
        }

        if(callbacks == null)
            callbacks = LinkedList()

        callbacks!!.add(callback)
    }
}