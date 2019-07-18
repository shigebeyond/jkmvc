package net.jkcode.jkmvc.common

import org.slf4j.Logger
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * 按模块来切换启用日志
 * 
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-18 7:30 PM
 */
class ModuleLogSwitch(protected val delegate: Logger) : InvocationHandler {

    public override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any {
    }

    fun test(): Any? {
        return Proxy.newProxyInstance(this.javaClass.classLoader, arrayOf(Logger::class.java), this)
    }
}