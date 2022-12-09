package net.jkcode.jkmvc.canal

import com.alibaba.otter.canal.client.CanalConnector
import com.alibaba.otter.canal.client.CanalConnectors
import com.alibaba.otter.canal.protocol.Message
import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.dbLogger
import java.net.InetSocketAddress

/**
 * canal client
 *   负责订阅server，并处理收到的binlog
 * @author shijianhang<772910474@qq.com>
 * @date 2022-12-9 7:13 PM
 */
object CanalClient {

    /**
     * 配置
     */
    private val config = Config.instance("canal", "yaml");

    /**
     * 处理器容器
     */
    private val handlerContainer = CanalLogHandlerContainer()

    /**
     * 添加处理器
     */
    public fun addLogHandler(handler: ICanalLogHandler): CanalClient {
        handlerContainer.add(handler)
        return this
    }

    /**
     * 连接canal server，处理收到的binlog
     */
    public fun connectServer() {
        // 创建链接
        val connector: CanalConnector = CanalConnectors.newSingleConnector(
            InetSocketAddress(config.getString("ip"), config.getInt("port")!!),
            config.getString("destination"),
            "",
            ""
        )
        val batchSize = 1000
        var emptyCount = 0
        try {
            connector.connect()
            connector.subscribe(".*\\..*")
            connector.rollback()
            val totalEntryCount = 1200
            while (emptyCount < totalEntryCount) {
                // 获取指定数量的日志
                val message: Message = connector.getWithoutAck(batchSize)
                val batchId: Long = message.id
                val size: Int = message.entries.size
                if (batchId == -1L || size == 0) { // 日志为空
                    emptyCount++
                    dbLogger.info("empty count : $emptyCount")
                    try {
                        Thread.sleep(5000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                } else { // 日志不为空，则逐个处理日志
                    emptyCount = 0
                    for (entry in message.entries)
                        handlerContainer.handleBinLog(entry)
                }
                connector.ack(batchId) // 提交确认
            }
            dbLogger.error("empty too many times, exit")
        } catch (e: Exception) {
            dbLogger.error(e.message, e)
            //connector.rollback(batchId); // 处理失败, 回滚数据
        } finally {
            connector.disconnect()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // 处理单表
        val handler1 = object:CanalLogHandler("test", "user"){
            override fun handleUpdate(oldRow: Map<String, String?>, newRow: Map<String, String?>) {
                if("age" in newRow)
                    println("处理单表 age change: from [${oldRow["age"]}] to ${newRow["age"]}")
            }
        }
        CanalClient.addLogHandler(handler1)
        // 处理所有表
        val handler2 = object:CanalLogHandler("test", "*"){
            override fun handleInsert(row: Map<String, String?>) {
                println("处理所有表 handleInsert: $row")
            }
            override fun handleDelete(row: Map<String, String?>) {
                println("处理所有表 handleDelete: $row")
            }
            override fun handleUpdate(oldRow: Map<String, String?>, newRow: Map<String, String?>) {
                println("处理所有表 handleUpdate: \n\tfrom: $oldRow \n\tto: $newRow")
            }
        }
        CanalClient.addLogHandler(handler2)
        CanalClient.connectServer()
    }

}