package com.jkmvc.future

import org.apache.http.concurrent.FutureCallback
import java.util.*

/**
 * 可回调
 * @author shijianhang<772910474@qq.com>
 * @date 2019-01-14 11:46 AM
 */
abstract class Callbackable<T> : ICallbackable<T> {

    /**
     * 回调
     */
    protected var callbacks: MutableList<FutureCallback<T?>>? = null

    /**
     * 添加回调
     * @param callback
     */
    public override fun addCallback(callback: FutureCallback<T?>){
        if(callbacks == null) // 延迟创建
            callbacks = LinkedList()
        callbacks!!.add(callback)
    }
}