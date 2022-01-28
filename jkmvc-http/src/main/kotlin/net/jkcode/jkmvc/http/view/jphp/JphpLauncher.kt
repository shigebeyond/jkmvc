package net.jkcode.jkmvc.http.view.jphp

import org.develnext.jphp.core.opcode.ModuleOpcodePrinter
import php.runtime.Memory
import php.runtime.env.CallStackItem
import php.runtime.env.TraceInfo
import php.runtime.ext.core.classes.WrapClassLoader
import php.runtime.ext.core.classes.WrapClassLoader.WrapLauncherClassLoader
import php.runtime.ext.java.JavaObject
import php.runtime.launcher.LaunchException
import php.runtime.launcher.Launcher
import php.runtime.memory.*
import php.runtime.reflection.support.ReflectionUtils
import java.io.IOException

object JphpLauncher : Launcher() {

    fun run(bootstrapFile: String, args: Map<String, Any?>) {
        // 注册java对象， 方便调用java对象
        val core = compileScope.getExtension("Core")
        compileScope.registerLazyClass(core, JavaObject::class.java)

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

        // 加载入口文件
        val bootstrap = loadFrom(bootstrapFile) ?: throw IOException("Cannot find '$bootstrapFile' resource")

        // 前置处理
//        beforeIncludeBootstrap()

        // 显示字节码
        if (StringMemory(config.getProperty("bootstrap.showBytecode", "")).toBoolean()) {
            val moduleOpcodePrinter = ModuleOpcodePrinter(bootstrap)
            println(moduleOpcodePrinter.toString())
        }

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
        for((k, v) in args){
            locals.put(k, v?.toMemory())
        }

        // 调用入栈
        val stackItem = CallStackItem(bootstrap.trace)
        environment.pushCall(stackItem)

        // include 执行
        try {
            bootstrap.includeNoThrow(environment, locals)
        } finally {
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