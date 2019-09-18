package net.jkcode.jkmvc.scope

import net.jkcode.jkmvc.common.commonLogger
import java.io.Closeable
import java.util.*

/**
 * 全局的请求作用域
 *    包含http请求与rpc请求
 *    对应http请求与rpc请求的处理器, 承诺在请求处理前后调用其  beginScope()/endScope()
 *
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-12-16 3:48 PM
 */
object GlobalRequestScope : Scope(), Closeable {

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