package net.jkcode.jkmvc.canal

import com.alibaba.otter.canal.protocol.CanalEntry
import net.jkcode.jkutil.common.dbLogger
import java.util.*

/**
 * canal日志处理器
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2022-12-9 7:13 PM
 */
class CanalLogHandlerContainer(
    protected val handlers: MutableList<ICanalLogHandler> = LinkedList() // 代理list
): MutableList<ICanalLogHandler> by handlers{

    /**
     * 处理单个binlog
     */
    public fun handleBinLog(entry: CanalEntry.Entry) {
        // 忽略事务
        if (entry.entryType === CanalEntry.EntryType.TRANSACTIONBEGIN || entry.entryType === CanalEntry.EntryType.TRANSACTIONEND)
            return

        // 获得变更数据
        var rowChage: CanalEntry.RowChange = try {
            CanalEntry.RowChange.parseFrom(entry.storeValue)
        } catch (e: Exception) {
            throw RuntimeException("ERROR ## parser of eromanga-event has an error , data:$entry", e)
        }

        // 获得变更类型: 增删改
        val eventType: CanalEntry.EventType = rowChage.eventType
        dbLogger.debug("binlog[{}:{}] , name[{},{}] , eventType: {}", entry.header.logfileName, entry.header.logfileOffset, entry.header.schemaName, entry.header.tableName, eventType)

        // 获得表名
        val table = entry.header.schemaName + '.' + entry.header.tableName

        // 逐个handler处理
        for(handler in handlers) {
            // handler匹配表名
            if (!handler.filter(table))
                continue

            // 逐行处理
            for (rowData in rowChage.rowDatasList) {
                handleRow(handler as CanalLogHandler, rowData, eventType)
            }
        }
    }

    /**
     * 处理单行
     */
    private fun handleRow(handler: CanalLogHandler, rowData: CanalEntry.RowData, eventType: CanalEntry.EventType) {
        // 插入
        if (eventType === CanalEntry.EventType.INSERT) {
            if(handler.processableEvents.contains("insert")) {
                val row = column2map(rowData.afterColumnsList, true)
                handler.handleInsert(row)
                return
            }
        }

        // 删除
        if (eventType === CanalEntry.EventType.DELETE) {
            if(handler.processableEvents.contains("delete")) {
                val row = column2map(rowData.beforeColumnsList, false)
                handler.handleDelete(row)
                return
            }
        }

        // 更新
        if(handler.processableEvents.contains("update")) {
            val oldRow = column2map(rowData.beforeColumnsList, false)
            val newRow = column2map(rowData.afterColumnsList, true)
            handler.handleUpdate(oldRow, newRow)
        }
    }

    /**
     * 变更的列数据转map
     * @param columns
     * @param onlyUpdate 是否只取更新的列
     * @return
     */
    private fun column2map(columns: List<CanalEntry.Column>, onlyUpdate: Boolean): Map<String, String?> {
        val r = HashMap<String, String?>()
        for (column in columns) {
            if (!onlyUpdate || column.updated)
                r[column.name] = column.value
        }
        return r
    }
}