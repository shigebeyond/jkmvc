package net.jkcode.jkmvc.http.jphp

import php.runtime.Memory
import php.runtime.env.CallStackItem
import php.runtime.env.TraceInfo
import php.runtime.ext.core.OutputFunctions
import php.runtime.ext.core.classes.WrapClassLoader
import php.runtime.ext.core.classes.WrapClassLoader.WrapLauncherClassLoader
import php.runtime.ext.java.JavaObject
import php.runtime.launcher.LaunchException
import php.runtime.launcher.Launcher
import php.runtime.memory.ArrayMemory
import php.runtime.reflection.support.ReflectionUtils
import java.io.IOException
import java.io.OutputStream

class JphpLauncher protected constructor() : Launcher() {

    companion object {

        /**
         * 线程独有的可复用的JphpLauncher
         */
        protected val insts: ThreadLocal<JphpLauncher> = ThreadLocal.withInitial {
            JphpLauncher()
        }

        public fun instance(): JphpLauncher {
            return insts.get()
        }

    }

    // 只初始化一次
    init {
        // 注册java对象， 方便调用java对象
        val core = compileScope.getExtension("Core")
        compileScope.registerLazyClass(core, JavaObject::class.java)
        compileScope.registerLazyClass(core, WrapJavaObject::class.java)

        // 配置
        readConfig()

        // 扩展
        initExtensions()

        if (isDebug()) {
            if (compileScope.tickHandler == null) {
                throw LaunchException("Cannot find a debugger, please add the jphp-debugger dependency")
            }
        }

        // 类加载器
        val classLoader = config.getProperty("env.classLoader", ReflectionUtils.getClassName(WrapLauncherClassLoader::class.java))
        val classLoaderEntity = environment.fetchClass(classLoader)
        val loader = classLoaderEntity.newObject<WrapClassLoader>(environment, TraceInfo.UNKNOWN, true)
        environment.invokeMethod(loader, "register", Memory.TRUE)
    }

    // 执行php文件
    fun run(bootstrapFile: String, args: Map<String, Any?>, out: OutputStream? = null,  outputBuffering: Boolean = true) {
        // 加载入口文件
        val bootstrap = loadFrom(bootstrapFile) ?: throw IOException("Cannot find '$bootstrapFile' resource")

        // 前置处理
//        beforeIncludeBootstrap()

        // 显示字节码
        /*if (StringMemory(config.getProperty("bootstrap.showBytecode", "")).toBoolean()) {
            val moduleOpcodePrinter = ModuleOpcodePrinter(bootstrap)
            println(moduleOpcodePrinter.toString())
        }*/

        // 添加全局参数
        /*
        val argv = ArrayMemory.ofStrings(*this.args)
        val path = URLDecoder.decode(
                Launcher::class.java.protectionDomain.codeSource.location.toURI().path,
                "UTF-8"
        )
        argv.unshift(StringMemory.valueOf(path))
        environment.globals.put("argv", argv)
        environment.globals.put("argc", LongMemory.valueOf(argv.size()))
        */

        // 添加本地参数
        val locals = ArrayMemory(true)
        for ((k, v) in args) {
            locals.put(k, v?.toMemory())
        }

        // 调用入栈
        val stackItem = CallStackItem(bootstrap.trace)
        environment.pushCall(stackItem)

        // 设置输出流
        if(out != null)
            environment.outputBuffers.peek().output = out

        // 打开缓冲区， 等价于php ob_start()
        if(outputBuffering)
            OutputFunctions.ob_start(environment, bootstrap.trace)

        // include 执行
        try {
            bootstrap.includeNoThrow(environment, locals)
        } finally {
            // 发送内部缓冲区的内容到浏览器，并且关闭输出缓冲区
            if(outputBuffering)
                OutputFunctions.ob_end_flush(environment, bootstrap.trace)

            // 后置处理
//            afterIncludeBootstrap()

            // 调用出栈
            environment.popCall()

            // 进程结束的回调
//            compileScope.triggerProgramShutdown(environment)

            // 清理gc对象 + OutputBuffer
            environment.doFinal()
        }
    }

}