package net.jkcode.jkmvc.common

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * 按组件来切换启用日志
 *    本项目中有多个组件, 如rpc/job/tracer/mq等, 组件之间会相互依赖, 如mq依赖rpc
 *    我在开发某个组件时, 只对该组件日志感兴趣, 对其他组件日志不感兴趣, 这就需要禁用这些组件日志
 *    但一个组件会配置有多个logger(如rpc有register/client/server等几个logger), 禁用该组件日志, 要禁用这多个logger的 `log4j.properties` 配置, 很繁琐
 *    => 直接在组件级别来控制是否启用日志
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-18 7:30 PM
 */
class ModuleLogSwitcher(protected val module: String /* 组件 */) {

    companion object{

        /**
         * 组件日志切换配置
         */
        public val config: Config = Config.instance("module-log-switcher", "yaml")
    }

    /**
     * 是否启用
     */
    protected val enabled: Boolean = config.getBoolean(module, true)!!

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
     * 组件日志处理
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