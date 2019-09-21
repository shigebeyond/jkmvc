package net.jkcode.jkmvc.scope

import java.io.Closeable

// 针对所有请求的请求作用域
object GlobalAllRequestScope : IRequestScope() {}

// 针对rpc请求的请求作用域
object GlobalRpcRequestScope : IRequestScope() {}

// 针对http请求的请求作用域
object GlobalHttpRequestScope : IRequestScope() {}

/**
 * 请求作用域
 *    对应的请求处理器, 承诺在请求处理前后调用其  beginScope()/endScope()
 *
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-12-16 3:48 PM
 */
open class IRequestScope : BaseScope(), Closeable {

    init {
        // 关机时要关闭
        ClosingOnShutdown.addClosing(this)
    }

    /**
     * 可能没有开始请求作用域, 则需要关机时主动结束作用域(释放资源)
     *    如cli环境中调用Db
     */
    public override fun close() {
        endScope()
    }


}