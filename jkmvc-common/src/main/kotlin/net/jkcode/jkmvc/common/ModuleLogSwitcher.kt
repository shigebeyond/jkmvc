package net.jkcode.jkmvc.common

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * 按模块来切换启用日志
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-18 7:30 PM
 */
class ModuleLogSwitcher(protected val module: String /* 模块 */) {

    companion object{

        /**
         * 模块日志切换配置
         */
        public val config: Config = Config.instance("module-log-switcher")
    }

    /**
     * 是否启用
     */
    protected val enabled: Boolean = config[module]!!

    /**
     * 是否禁用
     */
    protected val disabled: Boolean = !enabled

    /**
     * 禁用的方法
     */
    protected val disableMethods = arrayOf("debug", "info", "warn")

    /**
     * 获得logger
     * @param name
     * @return
     */
    public fun getLogger(name: String): Logger{
        val logger = LoggerFactory.getLogger(name)
        return Proxy.newProxyInstance(this.javaClass.classLoader, arrayOf(Logger::class.java), ModuleLoggerHandler(logger)) as Logger
    }

    /**
     * 模块日志处理
     */
    inner class ModuleLoggerHandler(protected val delegate: Logger/* 代理的日志对象 */) : InvocationHandler{

        public override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any? {
            // 禁用中, 不打印debug/info/warn, 只打印error/fatal日志
            if(!enabled && disableMethods.contains(method.name))
                return null

            return method.invoke(delegate, *args)
        }
    }



}