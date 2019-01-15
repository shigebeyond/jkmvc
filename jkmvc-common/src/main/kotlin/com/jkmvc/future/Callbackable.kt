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
    public override var callback: FutureCallback<T?>? = null
}