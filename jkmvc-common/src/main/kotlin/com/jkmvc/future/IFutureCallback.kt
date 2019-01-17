package com.jkmvc.future

/**
 * A callback interface that gets invoked upon completion of
 * a [java.util.concurrent.Future].
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-01-17 5:45 PM
 */
interface IFutureCallback<T> {

    fun completed(result: T)

    fun failed(ex: Exception)

}
