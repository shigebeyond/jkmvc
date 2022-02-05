package net.jkcode.jkmvc.tests

import freemarker.template.Configuration
import net.jkcode.jphp.ext.JphpLauncher
import org.apache.velocity.VelocityContext
import org.apache.velocity.io.VelocityWriter
import org.apache.velocity.runtime.RuntimeInstance
import org.junit.Test
import php.runtime.ext.core.OutputFunctions
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.Writer
import java.util.*
import kotlin.reflect.KFunction0

class TemplateTests {

    val vm = mapOf(
            "name" to "shi",
            "age" to 12,
            "friends" to listOf("wang", "li", "zhang")
    )

    val rootPath = "src/test/resources"

    /**
     * 执行runJphp()耗时: 8s
     * 执行runVelocity()耗时: 36s
     * 执行runFreemarker()耗时: 14s
     */
    @Test
    fun testTemplate(){
        runPerformance(this::runJphp)
        runPerformance(this::runVelocity)
        runPerformance(this::runFreemarker)
    }

    fun getOutput(name: String): FileOutputStream {
        return FileOutputStream("test-$name.out")
    }

    // 测试性能
    fun runPerformance(func: KFunction0<Unit>) {
        val start = System.currentTimeMillis()
        for (i in 0 until 1000)
            func.invoke()
        println("执行" + func.name + "()耗时: " + (System.currentTimeMillis() - start) / 1000 + "s") // 5s
    }

    fun runJphp() {
        JphpLauncher.instance().run("$rootPath/test.php", vm, getOutput("jphp"))
    }

    fun runVelocity() {
        // 构建属性
        val props = Properties()
        props.setProperty("resource.loader", "file")
        props.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader")
        props.setProperty("file.resource.loader.path", rootPath)
        props.setProperty("file.resource.loader.cache", "false")
        props.setProperty("file.resource.loader.modificationCheckInterval", "2")
        props.setProperty("input.encoding", "UTF-8")
        props.setProperty("output.encoding", "UTF-8")
        props.setProperty("default.contentType", "text/html; charset=UTF-8")
        props.setProperty("velocimarco.library.autoreload", "true")
        props.setProperty("runtime.log.error.stacktrace", "false")
        props.setProperty("runtime.log.warn.stacktrace", "false")
        props.setProperty("runtime.log.info.stacktrace", "false")
        props.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.SimpleLog4JLogSystem")
        props.setProperty("runtime.log.logsystem.log4j.category", "velocity_log")

        // 单例用 Velocity
        // val instance = Velocity();
        // 非单例用 RuntimeInstanc：同时使用多个Velocity
        val instance = RuntimeInstance()
        instance.init(props)

        // 获得模板文件
        val template = instance.getTemplate("test.vm", "UTF-8");

        // 构建上下文：要渲染的数据
        val context = VelocityContext(vm)

        // 渲染模板
        var vwriter: VelocityWriter? = null
        try {
            //vwriter = VelocityWriter(System.out.writer())
            vwriter = VelocityWriter(getOutput("velocity").writer())
            template.merge(context, vwriter) // 合并上下文，根据数据渲染并输出
            vwriter.flush()
        } finally {
            vwriter?.recycle(null)
        }
    }

    @Test
    fun runFreemarker() {
        val conf = Configuration()
        //加载模板文件(模板的路径)
        conf.setDirectoryForTemplateLoading(File(rootPath))
        // 加载模板
        val template = conf.getTemplate("/test.ftl")
        // 定义输出
        template.process(vm, getOutput("freemarker").writer())
    }
}