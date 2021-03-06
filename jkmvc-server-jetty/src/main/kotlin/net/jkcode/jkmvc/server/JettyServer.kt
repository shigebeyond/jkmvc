package net.jkcode.jkmvc.server

import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.httpLogger
import net.jkcode.jkutil.common.prepareDirectory
import net.jkcode.jkutil.scope.ClosingOnShutdown
import org.apache.jasper.runtime.TldScanner
import org.eclipse.jetty.server.NCSARequestLog
import org.eclipse.jetty.server.NetworkConnector
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.server.handler.RequestLogHandler
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.webapp.WebAppContext
import java.io.Closeable
import java.io.File


/**
 * jetty服务器
 *
 * 启动时报错: `java.lang.NoClassDefFoundError: javax/servlet/ServletRequest`
 * 原因: gradle的 war 插件自动将 javax.servlet-api 弄成 providedCompile, 你就算在工程的build.gradle 改为 compile 也没用
 * fix: project structure -> modules -> 选中 JettyServerLauncher 应用的工程 -> depencies -> 选中 Gradle: javax.servlet:javax.servlet-api:3.1.0 包, 将 scop 由 provided 改为 compile
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-29 12:55 PM
 */
class JettyServer : Closeable{

    /**
     * jetty配置
     */
    public val config: Config = Config.instance("jetty", "yaml")

    /**
     * server
     */
    protected lateinit var server: Server

    /**
     * 启动server
     */
    public fun start() {
        // 关机时要关闭
        ClosingOnShutdown.addClosing(this)

        // 加载 jstl 的 tld 文件
        loadJstlTld()

        // 启动server
        server = Server(createThreadPool()) // 线程池
        server.addConnector(createConnector()) // 连接器
        server.setHandler(createHandlers()) // 处理器
        server.setStopAtShutdown(true)
        server.start()
        httpLogger.info("启动jetty, 监听端口 {}, 请访问 http://localhost:{}{}", config["port"], config["port"], config["contextPath"])

        server.join()
    }

    /**
     * 创建线程池
     */
    protected fun createThreadPool(): QueuedThreadPool {
        var maxThreads: Int = config["maxThreads"]!!
        if(maxThreads == 0)
            maxThreads = Runtime.getRuntime().availableProcessors() * 8
        val threadPool = QueuedThreadPool(maxThreads)
        return threadPool
    }

    /**
     * 创建连接器,接收各种协议(http等)的连接
     */
    protected fun createConnector(): NetworkConnector {
        val connector = ServerConnector(server)
        connector.setHost(config["host"])
        connector.setPort(config["port"]!!)
        return connector
    }

    /**
     * 创建处理器
     */
    protected fun createHandlers(): HandlerCollection {
        val handlerCollection = HandlerCollection()
        handlerCollection.setHandlers(arrayOf(
                createServletContextHandler(),
                createLogHandler()
                //GzipHandler()
        ))
        return handlerCollection
    }

    /**
     * 创建日志处理器
     */
    protected fun createLogHandler(): RequestLogHandler {
        val logDir: String = config["logDir"]!!
        logDir.prepareDirectory() // 准备好目录

        val requestLog = NCSARequestLog()
        requestLog.setFilename("$logDir/jetty.log")
        requestLog.setFilenameDateFormat("yyyy-MM-dd")
        requestLog.setRetainDays(10)
        requestLog.setExtended(false)
        requestLog.setAppend(true)
        requestLog.setLogTimeZone("Asia/Shanghai")
        requestLog.setLogDateFormat("yyyy-MM-dd HH:mm:ss SSS")
        requestLog.setLogLatency(true)

        val logHandler = RequestLogHandler()
        logHandler.setRequestLog(requestLog)
        return logHandler
    }

    /**
     * 创建servlet上下文的处理器
     */
    protected fun createServletContextHandler(): WebAppContext {
        val context = WebAppContext()
        context.setContextPath(config["contextPath"])
        val webDir: String = config["webDir"]!!
        context.setWar(webDir)
        context.setDescriptor("$webDir/WEB-INF/web.xml");
        context.setResourceBase(webDir);
        context.setDisplayName("jetty");
        context.setClassLoader(Thread.currentThread().getContextClassLoader());
        context.setConfigurationDiscovered(true);
        context.setParentLoaderPriority(true);

        val tempDir: String = config["tempDir"]!!
        tempDir.prepareDirectory() // 准备好目录
        context.setTempDirectory(File(tempDir))
        
        return context
    }

    private fun loadJstlTld() {
        try {
            val f = TldScanner::class.java.getDeclaredField("systemUris")
            f.isAccessible = true
            (f.get(null) as MutableSet<*>).clear()
        } catch (e: Exception) {
            throw RuntimeException("Could not clear TLD system uris.", e)
        }
    }

    /**
     * 关闭server
     */
    override fun close() {
        server.stop()
    }

}