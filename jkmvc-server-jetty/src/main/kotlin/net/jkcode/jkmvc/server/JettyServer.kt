package net.jkcode.jkmvc.server

import net.jkcode.jkutil.scope.ClosingOnShutdown
import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.prepareDirectory
import net.jkcode.jkutil.common.httpLogger
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

        // 启动server
        val threadPool = createThreadPool() // 线程池
        server = Server(threadPool)
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
        val threadNum = config.getInt("threadNum", Runtime.getRuntime().availableProcessors())!!
        val threadPool = QueuedThreadPool(threadNum)
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

    /**
     * 关闭server
     */
    override fun close() {
        server.stop()
    }

}